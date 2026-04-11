package com.realestate.security;

import com.realestate.config.AppProperties;
import com.realestate.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Handles all JWT operations:
 *  - generateAccessToken()   → short-lived token (24h) sent in API responses
 *  - generateRefreshToken()  → long-lived token (7d) used to get new access tokens
 *  - validateToken()         → checks signature + expiry
 *  - extractEmail()          → reads the subject claim
 *  - extractRole()           → reads the custom "role" claim
 */
@Component
@Slf4j
public class JwtUtil {

    private final AppProperties appProperties;
    private final SecretKey signingKey;

    public JwtUtil(AppProperties appProperties) {
        this.appProperties = appProperties;
        // Decode the base64 secret and build an HMAC-SHA256 key
        byte[] keyBytes = Decoders.BASE64.decode(appProperties.getJwt().getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // ─────────────────────────────────────────────
    // Token generation
    // ─────────────────────────────────────────────

    /**
     * Creates an access token containing:
     *   sub   = user email
     *   role  = BUYER / SELLER / AGENT / ADMIN
     *   uid   = user UUID (so controllers can load the user without a DB hit)
     */
    public String generateAccessToken(User user) {
        return buildToken(
            user,
            appProperties.getJwt().getAccessTokenExpiryMs(),
            Map.of(
                "role", user.getRole().name(),
                "uid",  user.getId().toString(),
                "name", user.getName()
            )
        );
    }

    /**
     * Refresh token — minimal claims, longer expiry.
     * Stored nowhere on the server; client sends it to /auth/refresh-token.
     */
    public String generateRefreshToken(User user) {
        return buildToken(
            user,
            appProperties.getJwt().getRefreshTokenExpiryMs(),
            Map.of("type", "refresh")
        );
    }

    private String buildToken(User user, long expiryMs, Map<String, Object> extraClaims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .claims(extraClaims)
            .subject(user.getEmail())
            .issuer(appProperties.getJwt().getIssuer())
            .issuedAt(new Date(now))
            .expiration(new Date(now + expiryMs))
            .id(UUID.randomUUID().toString())   // jti — unique token ID
            .signWith(signingKey)
            .compact();
    }

    // ─────────────────────────────────────────────
    // Token validation
    // ─────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims empty: {}", e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────
    // Claims extraction
    // ─────────────────────────────────────────────

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("uid", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

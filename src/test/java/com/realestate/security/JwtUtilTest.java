package com.realestate.security;

import com.realestate.config.AppProperties;
import com.realestate.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JwtUtil}, including the #33 token-type enforcement:
 * access tokens carry type=access, refresh tokens type=refresh, and the two
 * must be distinguishable so a refresh token can't be used as an access token.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // HS256 needs a >=256-bit key; this base64 secret decodes to 47 bytes.
        String secret = Base64.getEncoder()
            .encodeToString("a-very-long-test-signing-key-of-many-bytes-123".getBytes(StandardCharsets.UTF_8));

        AppProperties props = new AppProperties();
        props.getJwt().setSecret(secret);
        props.getJwt().setAccessTokenExpiryMs(86_400_000L);
        props.getJwt().setRefreshTokenExpiryMs(604_800_000L);
        props.getJwt().setIssuer("realestate-test");

        jwtUtil = new JwtUtil(props);
    }

    private User user() {
        return User.builder()
            .id(UUID.randomUUID())
            .email("user@propfind.in")
            .name("Test User")
            .role(User.Role.BUYER)
            .build();
    }

    @Test
    void accessToken_validatesAndCarriesAccessType() {
        String token = jwtUtil.generateAccessToken(user());

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@propfind.in");
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo(JwtUtil.TYPE_ACCESS);
        assertThat(jwtUtil.isRefreshToken(token)).isFalse();
    }

    @Test
    void refreshToken_isMarkedAsRefresh() {
        String token = jwtUtil.generateRefreshToken(user());

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo(JwtUtil.TYPE_REFRESH);
        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
    }

    @Test
    void accessTokenIsNotAcceptedAsRefresh_andViceVersa() {
        // The whole point of #33: the two token kinds are not interchangeable.
        assertThat(jwtUtil.isRefreshToken(jwtUtil.generateAccessToken(user()))).isFalse();
        assertThat(jwtUtil.isRefreshToken(jwtUtil.generateRefreshToken(user()))).isTrue();
    }

    @Test
    void garbageTokenFailsValidation() {
        assertThat(jwtUtil.validateToken("not-a-real-jwt")).isFalse();
    }

    @Test
    void extractRoleAndUserIdFromAccessToken() {
        User u = user();
        String token = jwtUtil.generateAccessToken(u);

        assertThat(jwtUtil.extractRole(token)).isEqualTo("BUYER");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(u.getId().toString());
    }
}

package com.realestate.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter for /auth/** endpoints.
 * Keyed by client IP. No Redis or external store needed.
 *
 * Limits:
 *   POST /auth/login            — 10 attempts / minute
 *   POST /auth/register         — 5 attempts / 15 minutes
 *   POST /auth/forgot-password  — 3 attempts / 10 minutes
 *   everything else under /auth — 30 requests / minute
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets        = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets     = new ConcurrentHashMap<>();
    private final Map<String, Bucket> forgotPwdBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalAuthBuckets  = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Only rate-limit auth endpoints
        String path = request.getServletPath();
        if (!path.startsWith("/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip     = resolveClientIp(request);
        Bucket bucket = bucketFor(path, ip);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\"," +
                "\"message\":\"Too many attempts. Please wait and try again.\"}"
            );
        }
    }

    private Bucket bucketFor(String path, String ip) {
        if (path.startsWith("/auth/login")) {
            // 10 per minute — brute-force guard
            return loginBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                    .build());
        }
        if (path.startsWith("/auth/register")) {
            // 5 per 15 minutes — spam guard
            return registerBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(15))))
                    .build());
        }
        if (path.startsWith("/auth/forgot-password")) {
            // 3 per 10 minutes — OTP abuse guard
            return forgotPwdBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(10))))
                    .build());
        }
        // Everything else under /auth — generous limit
        return generalAuthBuckets.computeIfAbsent(ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1))))
                .build());
    }

    /**
     * Prefers X-Forwarded-For (set by Railway / Render / Cloudflare reverse proxies)
     * over the raw socket address.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

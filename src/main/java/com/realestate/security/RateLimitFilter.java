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
 * In-memory rate limiter, keyed by client IP. No Redis / external store.
 *
 * Auth limits (brute-force / OTP / signup spam):
 *   POST /auth/login            — 10 attempts / minute
 *   POST /auth/register         — 5 attempts / 15 minutes
 *   POST /auth/forgot-password  — 3 attempts / 10 minutes
 *   everything else under /auth — 30 requests / minute
 *
 * Property-write limits (storage spam / email-quota abuse — protects R2 bill):
 *   POST /properties/{id}/inquiries — 5 / hour  (also guards owner email quota)
 *   POST /properties                — 10 / day
 *   POST /properties/{id}/images    — 100 / hour
 *   POST /properties/{id}/documents — 40 / hour
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets          = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets       = new ConcurrentHashMap<>();
    private final Map<String, Bucket> forgotPwdBuckets      = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalAuthBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> inquiryBuckets        = new ConcurrentHashMap<>();
    private final Map<String, Bucket> propertyCreateBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> imageUploadBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> docUploadBuckets      = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path   = request.getServletPath();
        String method = request.getMethod();
        Bucket bucket = pickBucket(method, path, resolveClientIp(request));

        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\"," +
                "\"message\":\"Too many requests. Please wait and try again.\"}"
            );
        }
    }

    /** Returns null when no rate limit applies — the request is waved through. */
    private Bucket pickBucket(String method, String path, String ip) {
        // ── Auth ─────────────────────────────────────────────
        if (path.startsWith("/auth/login")) {
            return loginBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                    .build());
        }
        if (path.startsWith("/auth/register")) {
            return registerBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(15))))
                    .build());
        }
        if (path.startsWith("/auth/forgot-password")) {
            return forgotPwdBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(10))))
                    .build());
        }
        if (path.startsWith("/auth/")) {
            return generalAuthBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1))))
                    .build());
        }

        // ── Property writes — POSTs only; reads stay unmetered ─
        if (!"POST".equalsIgnoreCase(method)) return null;
        if (!path.startsWith("/properties"))  return null;

        if (path.matches("^/properties/[^/]+/inquiries/?$")
         || path.matches("^/properties/[^/]+/site-visits/?$")) {
            // Same 5/hr cap for both inquiries and site-visit bookings —
            // they hit the same downstream (owner notification email).
            return inquiryBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofHours(1))))
                    .build());
        }
        if (path.matches("^/properties/[^/]+/images/?$")) {
            return imageUploadBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofHours(1))))
                    .build());
        }
        if (path.matches("^/properties/[^/]+/documents/?$")) {
            return docUploadBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(40, Refill.greedy(40, Duration.ofHours(1))))
                    .build());
        }
        if (path.equals("/properties") || path.equals("/properties/")) {
            return propertyCreateBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofDays(1))))
                    .build());
        }
        return null;
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

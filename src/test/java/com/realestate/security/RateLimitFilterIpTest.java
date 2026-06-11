package com.realestate.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the X-Forwarded-For handling in {@link RateLimitFilter}.
 *
 * X-Forwarded-For is "client, proxy1, proxy2" where each proxy appends what it saw.
 * A malicious client can forge entries on the LEFT; our trusted proxy appends the real
 * socket IP on the RIGHT. The old code took the leftmost entry and so keyed the rate
 * limiter on attacker-controlled input — a client could rotate a fake value per request
 * and never get throttled. The fix takes the rightmost (trusted) entry. These tests pin it.
 */
class RateLimitFilterIpTest {

    private final RateLimitFilter filter = new RateLimitFilter();

    private String resolveClientIp(HttpServletRequest request) throws Exception {
        Method m = RateLimitFilter.class.getDeclaredMethod("resolveClientIp", HttpServletRequest.class);
        m.setAccessible(true);
        return (String) m.invoke(filter, request);
    }

    @Test
    void ignoresSpoofedLeftmostEntryAndUsesTrustedRightmost() throws Exception {
        // Attacker forges "1.2.3.4"; the trusted proxy appended the real "203.0.113.9".
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 203.0.113.9");

        assertThat(resolveClientIp(req)).isEqualTo("203.0.113.9");
    }

    @Test
    void differentSpoofedPrefixesResolveToTheSameTrustedIp() throws Exception {
        // The whole point: a client rotating the leftmost value must NOT get fresh buckets.
        HttpServletRequest a = mock(HttpServletRequest.class);
        when(a.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9, 203.0.113.9");
        HttpServletRequest b = mock(HttpServletRequest.class);
        when(b.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8, 203.0.113.9");

        assertThat(resolveClientIp(a)).isEqualTo(resolveClientIp(b));
    }

    @Test
    void usesSingleEntryWhenOnlyOneHopPresent() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.9");

        assertThat(resolveClientIp(req)).isEqualTo("203.0.113.9");
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderAbsent() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("10.0.0.5");

        assertThat(resolveClientIp(req)).isEqualTo("10.0.0.5");
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderBlank() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(req.getRemoteAddr()).thenReturn("10.0.0.5");

        assertThat(resolveClientIp(req)).isEqualTo("10.0.0.5");
    }
}

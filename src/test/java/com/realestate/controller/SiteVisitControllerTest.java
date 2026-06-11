package com.realestate.controller;

import com.realestate.exception.UnauthorizedException;
import com.realestate.service.SiteVisitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression for #46 — the authenticated endpoints must surface a 401
 * (UnauthorizedException) instead of an NPE when no principal is present,
 * so a future SecurityConfig loosening fails safe.
 */
@ExtendWith(MockitoExtension.class)
class SiteVisitControllerTest {

    @Mock private SiteVisitService bookingService;

    @InjectMocks private SiteVisitController controller;

    @Test
    void listMine_withoutPrincipal_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.listMine(0, 20, null))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void listIncoming_withoutPrincipal_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.listIncoming(0, 20, null))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void cancel_withoutPrincipal_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.cancel(UUID.randomUUID(), null, null))
            .isInstanceOf(UnauthorizedException.class);
    }
}

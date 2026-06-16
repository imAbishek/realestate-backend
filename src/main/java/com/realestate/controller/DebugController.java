package com.realestate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY — Sentry smoke test. DELETE this file after verifying error reporting.
 *
 * GET /api/debug/boom throws an unhandled exception → 500 →
 * GlobalExceptionHandler.handleGeneral logs it (log.error) → sentry-logback
 * ships it to Sentry → issue-alert rule fires → email + Slack.
 *
 * Paired with a temporary `permitAll` for /debug/** in SecurityConfig (also
 * to be removed). Returns a generic 500 to the client (no stack trace leaked).
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/boom")
    public String boom() {
        throw new RuntimeException("Sentry smoke test — intentional error from GET /api/debug/boom");
    }
}

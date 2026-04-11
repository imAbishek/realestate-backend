package com.realestate.controller;

import com.realestate.dto.auth.AuthDtos.*;
import com.realestate.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles all authentication routes under /auth
 * (base context path /api is set in application.properties)
 *
 * Full paths:
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   POST /api/auth/refresh-token
 *   POST /api/auth/verify-otp
 *   POST /api/auth/forgot-password
 *   POST /api/auth/reset-password
 *
 * All routes are public (no JWT needed) — configured in SecurityConfig.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, and token management")
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user",
               description = "Creates account and sends OTP to email for verification")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(authService.register(request));
    }

    // ─────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login with email + password",
               description = "Returns access token (24h) and refresh token (7d)")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ─────────────────────────────────────────────
    // Refresh token
    // ─────────────────────────────────────────────

    @PostMapping("/refresh-token")
    @Operation(summary = "Get a new access token using a refresh token")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    // ─────────────────────────────────────────────
    // OTP verification
    // ─────────────────────────────────────────────

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify email OTP after registration")
    public ResponseEntity<MessageResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    // ─────────────────────────────────────────────
    // Forgot password
    // ─────────────────────────────────────────────

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset OTP to email")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    // ─────────────────────────────────────────────
    // Reset password
    // ─────────────────────────────────────────────

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP token")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}

package com.realestate.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTOs (Data Transfer Objects) for auth endpoints.
 * These are what the API accepts and returns — never raw entities.
 *
 * Nested as static classes to keep them in one file.
 */
public class AuthDtos {

    // ─────────────────────────────────────────────
    // REQUEST DTOs  (what client sends to the API)
    // ─────────────────────────────────────────────

    /** POST /auth/register */
    @Data
    public static class RegisterRequest {

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 150, message = "Name must be 2-150 characters")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        private String email;

        @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Please enter a valid 10-digit Indian mobile number"
        )
        private String phone;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
            regexp = ".*[A-Za-z].*",
            message = "Password must contain at least one letter"
        )
        private String password;

        // Optional — defaults to BUYER if not provided
        private User.Role role;
    }

    /** POST /auth/login */
    @Data
    public static class LoginRequest {

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    /** POST /auth/refresh-token */
    @Data
    public static class RefreshTokenRequest {

        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    /** POST /auth/verify-otp */
    @Data
    public static class VerifyOtpRequest {

        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        private String otp;
    }

    /** POST /auth/forgot-password */
    @Data
    public static class ForgotPasswordRequest {

        @NotBlank(message = "Email is required")
        @Email
        private String email;
    }

    /** POST /auth/reset-password */
    @Data
    public static class ResetPasswordRequest {

        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        private String otp;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;
    }

    // ─────────────────────────────────────────────
    // RESPONSE DTOs  (what the API sends back)
    // ─────────────────────────────────────────────

    /** Returned on successful login or registration */
    @Data
    @lombok.Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;          // always "Bearer"
        private long   expiresIn;          // milliseconds
        private UserInfo user;
    }

    /** Basic user info embedded in AuthResponse */
    @Data
    @lombok.Builder
    @JsonIgnoreProperties("verified")
    public static class UserInfo {
        private String      id;
        private String      name;
        private String      email;
        private String      phone;
        private User.Role   role;
        @JsonProperty("isVerified")
        private boolean     isVerified;
        private String      profilePhotoUrl;
    }

    /** Simple success/message responses */
    @Data
    @lombok.AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }
}

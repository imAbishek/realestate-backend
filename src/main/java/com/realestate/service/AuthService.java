package com.realestate.service;

import com.realestate.config.AppProperties;
import com.realestate.dto.auth.AuthDtos.*;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ConflictException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.exception.UnauthorizedException;
import com.realestate.repository.UserRepository;
import com.realestate.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * All authentication business logic lives here.
 * Controllers just call these methods and return the result.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtUtil           jwtUtil;
    private final AuthenticationManager authManager;
    private final EmailService      emailService;
    private final AppProperties     appProperties;

    // ─────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest req) {

        // 1. Check for duplicates
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("An account with this email already exists");
        }
        if (req.getPhone() != null && userRepository.existsByPhone(req.getPhone())) {
            throw new ConflictException("An account with this phone number already exists");
        }

        // 2. Build and save the user — all new accounts start as BUYER
        User user = User.builder()
            .name(req.getName())
            .email(req.getEmail().toLowerCase().trim())
            .phone(req.getPhone())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .role(User.Role.BUYER)
            .verified(false)
            .active(true)
            .build();

        user = userRepository.save(user);

        // 3. Generate and send OTP for email verification
        String otp = generateOtp();
        saveOtp(user, otp);
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), otp);
        } catch (Exception e) {
            log.warn("Could not send verification email to {}: {}", user.getEmail(), e.getMessage());
        }

        log.info("New user registered: {} ({})", user.getEmail(), user.getRole());

        // 4. Return tokens immediately (user can use the app, but is "unverified")
        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest req) {

        String raw = req.getIdentifier().trim();
        // Phone: starts with 6-9, exactly 10 digits (Indian numbers)
        boolean isPhone = raw.matches("^[6-9]\\d{9}$");
        String identifier = isPhone ? raw : raw.toLowerCase();

        // Spring Security calls CustomUserDetailsService.loadUserByUsername(identifier)
        // which resolves phone or email to the correct user record
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, req.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid credentials");
        }

        User user = isPhone
            ? userRepository.findByPhone(identifier)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"))
            : userRepository.findByEmail(identifier)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Your account has been deactivated. Contact support.");
        }

        // Update last login timestamp (non-blocking — best effort)
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        log.info("User logged in: {} (via {})", user.getEmail(), isPhone ? "phone" : "email");
        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────

    public AuthResponse refreshToken(RefreshTokenRequest req) {
        String token = req.getRefreshToken();

        if (!jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Refresh token is invalid or expired. Please log in again.");
        }

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────
    // OTP VERIFICATION
    // ─────────────────────────────────────────────

    @Transactional
    public MessageResponse verifyOtp(VerifyOtpRequest req) {
        User user = userRepository.findByEmailAndOtpCode(req.getEmail().toLowerCase().trim(), req.getOtp())
            .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // Mark verified and clear OTP
        user.setVerified(true);
        userRepository.save(user);
        userRepository.clearOtp(user.getId());

        log.info("User verified: {}", user.getEmail());
        return new MessageResponse("Email verified successfully!");
    }

    // ─────────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────────

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        // Always return success to prevent email enumeration attacks
        userRepository.findByEmail(req.getEmail().toLowerCase().trim()).ifPresent(user -> {
            String otp = generateOtp();
            saveOtp(user, otp);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), otp);
            log.info("Password reset OTP sent to: {}", user.getEmail());
        });

        return new MessageResponse(
            "If an account with that email exists, a reset link has been sent."
        );
    }

    // ─────────────────────────────────────────────
    // RESET PASSWORD
    // ─────────────────────────────────────────────

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        User user = userRepository.findByEmailAndOtpCode(req.getEmail().toLowerCase().trim(), req.getOtp())
            .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        userRepository.clearOtp(user.getId());

        log.info("Password reset for: {}", req.getEmail());
        return new MessageResponse("Password reset successfully. You can now log in.");
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        UserInfo userInfo = UserInfo.builder()
            .id(user.getId().toString())
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole())
            .isVerified(user.isVerified())
            .profilePhotoUrl(user.getProfilePhotoUrl())
            .build();

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(appProperties.getJwt().getAccessTokenExpiryMs())
            .user(userInfo)
            .build();
    }

    private String generateOtp() {
        // Cryptographically secure 6-digit OTP
        SecureRandom random = new SecureRandom();
        int otp = 100_000 + random.nextInt(900_000);
        return String.valueOf(otp);
    }

    private void saveOtp(User user, String otp) {
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(15));  // OTP valid for 15 minutes
        userRepository.save(user);
    }
}

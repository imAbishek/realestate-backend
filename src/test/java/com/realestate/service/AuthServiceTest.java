package com.realestate.service;

import com.realestate.config.AppProperties;
import com.realestate.dto.auth.AuthDtos.*;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ConflictException;
import com.realestate.exception.UnauthorizedException;
import com.realestate.repository.UserRepository;
import com.realestate.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} — register/login/refresh guards plus the
 * #33 (token type on refresh) and #34 (OTP brute-force cap) security fixes.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository       userRepository;
    @Mock private PasswordEncoder      passwordEncoder;
    @Mock private JwtUtil              jwtUtil;
    @Mock private AuthenticationManager authManager;
    @Mock private EmailService         emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getJwt().setAccessTokenExpiryMs(86_400_000L);
        authService = new AuthService(
            userRepository, passwordEncoder, jwtUtil, authManager, emailService, props);
    }

    private User activeUser(String email) {
        return User.builder()
            .id(UUID.randomUUID())
            .name("Test")
            .email(email)
            .role(User.Role.BUYER)
            .active(true)
            .build();
    }

    private RegisterRequest registerReq() {
        RegisterRequest r = new RegisterRequest();
        r.setName("Test User");
        r.setEmail("New@Propfind.in");
        r.setPassword("password1");
        return r;
    }

    // ── register ─────────────────────────────────────────────────

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerReq()))
            .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_success_savesBuyerAndIssuesTokens() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse res = authService.register(registerReq());

        assertThat(res.getAccessToken()).isEqualTo("access-token");
        assertThat(res.getUser().getRole()).isEqualTo(User.Role.BUYER);
        // Email is normalised to lowercase on save.
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(saved.capture()); // user + OTP save
        assertThat(saved.getAllValues().get(0).getEmail()).isEqualTo("new@propfind.in");
    }

    // ── login ────────────────────────────────────────────────────

    @Test
    void login_badCredentials_throwsUnauthorized() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        LoginRequest req = new LoginRequest();
        req.setIdentifier("user@propfind.in");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_success_returnsTokens() {
        when(authManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("user@propfind.in"))
            .thenReturn(Optional.of(activeUser("user@propfind.in")));
        when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");

        LoginRequest req = new LoginRequest();
        req.setIdentifier("user@propfind.in");
        req.setPassword("password1");

        AuthResponse res = authService.login(req);
        assertThat(res.getAccessToken()).isEqualTo("access-token");
    }

    // ── refresh (#33) ────────────────────────────────────────────

    @Test
    void refresh_rejectsAccessTokenUsedAsRefresh() {
        when(jwtUtil.validateToken("access-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("access-token")).thenReturn(false);

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("access-token");

        assertThatThrownBy(() -> authService.refreshToken(req))
            .isInstanceOf(UnauthorizedException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void refresh_acceptsRealRefreshToken() {
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.extractEmail("refresh-token")).thenReturn("user@propfind.in");
        when(userRepository.findByEmail("user@propfind.in"))
            .thenReturn(Optional.of(activeUser("user@propfind.in")));
        when(jwtUtil.generateAccessToken(any())).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("new-refresh");

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh-token");

        AuthResponse res = authService.refreshToken(req);
        assertThat(res.getAccessToken()).isEqualTo("new-access");
    }

    // ── OTP brute-force cap (#34) ─────────────────────────────────

    private User userWithOtp(String code, int attempts, LocalDateTime expiry) {
        User u = activeUser("otp@propfind.in");
        u.setOtpCode(code);
        u.setOtpAttempts(attempts);
        u.setOtpExpiresAt(expiry);
        return u;
    }

    @Test
    void verifyOtp_correctCode_marksVerifiedAndClearsOtp() {
        User u = userWithOtp("123456", 0, LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("otp@propfind.in")).thenReturn(Optional.of(u));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("otp@propfind.in");
        req.setOtp("123456");

        authService.verifyOtp(req);

        assertThat(u.isVerified()).isTrue();
        verify(userRepository).clearOtp(u.getId());
    }

    @Test
    void verifyOtp_wrongCode_incrementsAttemptsAndDoesNotClear() {
        User u = userWithOtp("123456", 0, LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("otp@propfind.in")).thenReturn(Optional.of(u));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("otp@propfind.in");
        req.setOtp("000000");

        assertThatThrownBy(() -> authService.verifyOtp(req))
            .isInstanceOf(BadRequestException.class);

        assertThat(u.getOtpAttempts()).isEqualTo(1);
        verify(userRepository, never()).clearOtp(any());
    }

    @Test
    void verifyOtp_tooManyAttempts_burnsOtp() {
        User u = userWithOtp("123456", 5, LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("otp@propfind.in")).thenReturn(Optional.of(u));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("otp@propfind.in");
        req.setOtp("123456"); // even a correct guess is rejected once capped

        assertThatThrownBy(() -> authService.verifyOtp(req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Too many");

        verify(userRepository).clearOtp(u.getId());
    }

    @Test
    void resetPassword_expiredOtp_throws() {
        User u = userWithOtp("123456", 0, LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("otp@propfind.in")).thenReturn(Optional.of(u));

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("otp@propfind.in");
        req.setOtp("123456");
        req.setNewPassword("newpassword1");

        assertThatThrownBy(() -> authService.resetPassword(req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("expired");
    }
}

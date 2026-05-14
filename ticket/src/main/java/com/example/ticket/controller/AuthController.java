package com.example.ticket.controller;

import com.example.ticket.dto.request.ForgotPasswordRequest;
import com.example.ticket.dto.request.LoginRequest;
import com.example.ticket.dto.request.RegisterRequest;
import com.example.ticket.dto.request.ResetPasswordRequest;
import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.AuthSessionResponse;
import com.example.ticket.dto.response.UserResponse;
import com.example.ticket.security.AuthCookieService;
import com.example.ticket.security.CookieUtils;
import com.example.ticket.service.AuthService;
import com.example.ticket.service.PasswordResetService;
import com.example.ticket.service.RecaptchaService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final PasswordResetService passwordResetService;
    private final RecaptchaService recaptchaService;

    public AuthController(
            AuthService authService,
            AuthCookieService authCookieService,
            PasswordResetService passwordResetService,
            RecaptchaService recaptchaService
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.passwordResetService = passwordResetService;
        this.recaptchaService = recaptchaService;
    }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        recaptchaService.verify(request.recaptchaToken(), "register", clientIp(servletRequest));
        AuthSessionResponse session = authService.register(request);
        authCookieService.addAuthCookies(response, session);
        return ResponseEntity.status(HttpStatus.CREATED).body(session.user());
    }

    @PostMapping("/login")
    UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        recaptchaService.verify(request.recaptchaToken(), "login", clientIp(servletRequest));
        AuthSessionResponse session = authService.login(request);
        authCookieService.addAuthCookies(response, session);
        return session.user();
    }

    @PostMapping("/refresh")
    UserResponse refresh(
            @CookieValue(value = CookieUtils.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthSessionResponse session = authService.refresh(refreshToken);
        authCookieService.addAuthCookies(response, session);
        return session.user();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @CookieValue(value = CookieUtils.ACCESS_TOKEN_COOKIE, required = false) String accessToken,
            @CookieValue(value = CookieUtils.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(accessToken, refreshToken);
        authCookieService.clearAuthCookies(response);
        return ApiResponse.ok("Logged out successfully", null);
    }

    @PostMapping("/forgot-password")
    ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        recaptchaService.verify(request.recaptchaToken(), "forgot_password", clientIp(servletRequest));
        passwordResetService.requestPasswordReset(request.email(), clientIp(servletRequest));
        return ApiResponse.ok("If this email exists, a password reset link has been sent.", null);
    }

    @PostMapping("/reset-password")
    ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
        return ApiResponse.ok("Password has been reset successfully.", null);
    }

    @PostMapping("/verify-email")
    ApiResponse<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ApiResponse.ok("Email has been verified successfully.", null);
    }

    @PostMapping("/resend-verification-email")
    ApiResponse<Void> resendVerificationEmail() {
        authService.resendVerificationEmail();
        return ApiResponse.ok("Verification email has been sent.", null);
    }

    @GetMapping("/me")
    UserResponse me() {
        return authService.currentUser();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

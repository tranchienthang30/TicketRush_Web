package com.example.ticket.service;

import com.example.ticket.dto.request.LoginRequest;
import com.example.ticket.dto.request.RegisterRequest;
import com.example.ticket.dto.response.AuthSessionResponse;
import com.example.ticket.dto.response.UserResponse;
import com.example.ticket.exception.AppException;
import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.AuthProvider;
import com.example.ticket.model.enums.UserStatus;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.security.JwtPrincipal;
import com.example.ticket.security.JwtTokenProvider;
import com.example.ticket.security.SessionPolicyService;
import com.example.ticket.security.TokenBlacklistService;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionPolicyService sessionPolicyService;
    private final AccountVerificationService accountVerificationService;
    private final ProviderNotificationService providerNotificationService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService,
            SessionPolicyService sessionPolicyService,
            AccountVerificationService accountVerificationService,
            ProviderNotificationService providerNotificationService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.sessionPolicyService = sessionPolicyService;
        this.accountVerificationService = accountVerificationService;
        this.providerNotificationService = providerNotificationService;
    }

    @Override
    public AuthSessionResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(HttpStatus.CONFLICT, "Email is already in use");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(blankToNull(request.phone()))
                .provider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .providerRequestStatus(Boolean.TRUE.equals(request.requestProviderAccess()) ? "PENDING" : null)
                .providerRequestedAt(Boolean.TRUE.equals(request.requestProviderAccess()) ? Instant.now() : null)
                .build();

        userRepository.save(user);
        accountVerificationService.sendVerificationEmail(user);
        if (Boolean.TRUE.equals(request.requestProviderAccess())) {
            providerNotificationService.notifyRequestSubmitted(user);
        }
        return createAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthSessionResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AppException(HttpStatus.FORBIDDEN, "This account is blocked");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
        }

        return createAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthSessionResponse refresh(String refreshToken) {
        if (refreshToken == null || tokenBlacklistService.isBlacklisted(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }
        User user = userRepository.findById(jwtTokenProvider.getUserId(refreshToken))
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AppException(HttpStatus.FORBIDDEN, "This account is blocked");
        }
        return createAuthResponse(user);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        tokenBlacklistService.blacklist(accessToken);
        tokenBlacklistService.blacklist(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        User user = userRepository.findById(currentUserId())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User does not exist"));
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AppException(HttpStatus.FORBIDDEN, "This account is blocked");
        }
        return UserResponse.from(user, currentSessionExpiresAt());
    }

    @Override
    public AuthSessionResponse authenticateGoogleUser(String email, String fullName, String providerId, String avatarUrl) {
        if (email == null || email.isBlank() || providerId == null || providerId.isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Google account information is incomplete");
        }

        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .orElseGet(() -> findOrCreateGoogleUser(normalizedEmail, fullName, providerId, avatarUrl));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AppException(HttpStatus.FORBIDDEN, "This account is blocked");
        }

        boolean changed = false;
        if (avatarUrl != null && !avatarUrl.isBlank() && !avatarUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarUrl);
            changed = true;
        }
        if ((user.getFullName() == null || user.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }

        return createAuthResponse(user);
    }

    @Override
    public void verifyEmail(String token) {
        accountVerificationService.verifyEmail(token);
    }

    @Override
    public void resendVerificationEmail() {
        User user = userRepository.findById(currentUserId())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User does not exist"));
        accountVerificationService.sendVerificationEmail(user);
    }

    private AuthSessionResponse createAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user, sessionPolicyService.accessDuration(user.getRole()));
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, sessionPolicyService.refreshDuration(user.getRole()));
        Instant accessExpiresAt = jwtTokenProvider.getExpiresAt(accessToken);
        Instant refreshExpiresAt = jwtTokenProvider.getExpiresAt(refreshToken);
        return new AuthSessionResponse(
                accessToken,
                refreshToken,
                UserResponse.from(user, accessExpiresAt),
                accessExpiresAt,
                refreshExpiresAt
        );
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "You need to sign in");
        }
        return principal.userId();
    }

    private Instant currentSessionExpiresAt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return principal.expiresAt();
        }
        return null;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private User findOrCreateGoogleUser(String email, String fullName, String providerId, String avatarUrl) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(existing -> attachGoogleProvider(existing, providerId, avatarUrl))
                .orElseGet(() -> {
                    User user = userRepository.save(User.builder()
                            .email(email)
                            .passwordHash(null)
                            .fullName((fullName == null || fullName.isBlank()) ? email : fullName.trim())
                            .avatarUrl(blankToNull(avatarUrl))
                            .provider(AuthProvider.GOOGLE)
                            .providerId(providerId)
                            .status(UserStatus.ACTIVE)
                            .emailVerified(false)
                            .build());
                    accountVerificationService.sendVerificationEmail(user);
                    return user;
                });
    }

    private User attachGoogleProvider(User existing, String providerId, String avatarUrl) {
        if (existing.getProviderId() != null && !existing.getProviderId().equals(providerId)) {
            throw new AppException(HttpStatus.CONFLICT, "This email is already linked to another Google account.");
        }
        existing.setProvider(AuthProvider.GOOGLE);
        existing.setProviderId(providerId);
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            existing.setAvatarUrl(avatarUrl);
        }
        return userRepository.save(existing);
    }

}

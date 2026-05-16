package com.example.ticket.service;

import com.example.ticket.exception.AppException;
import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.AuthProvider;
import com.example.ticket.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration EMAIL_RATE_WINDOW = Duration.ofMinutes(15);
    private static final Duration IP_RATE_WINDOW = Duration.ofHours(1);
    private static final int EMAIL_RATE_LIMIT = 3;
    private static final int IP_RATE_LIMIT = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String frontendUrl;

    public PasswordResetServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            StringRedisTemplate redisTemplate,
            EmailService emailService,
            @Value("${frontend.url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void requestPasswordReset(String email, String clientIp) {
        String normalizedEmail = normalizeEmail(email);
        enforceRateLimit("auth:reset-rate:email:" + normalizedEmail, EMAIL_RATE_LIMIT, EMAIL_RATE_WINDOW);
        enforceRateLimit("auth:reset-rate:ip:" + clientIp, IP_RATE_LIMIT, IP_RATE_WINDOW);

        userRepository.findByEmailIgnoreCase(normalizedEmail)
            .ifPresent(this::createTokenAndSendEmail);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String password) {
        String key = resetTokenKey(token);
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Reset token is invalid or expired");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Reset token is invalid or expired"));

        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
        redisTemplate.delete(key);
    }

    private void createTokenAndSendEmail(User user) {
        String token = generateToken();
        redisTemplate.opsForValue().set(resetTokenKey(token), user.getId().toString(), TOKEN_TTL);
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
    }

    private void enforceRateLimit(String key, int limit, Duration window) {
        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, window);
        }
        if (current != null && current > limit) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset requests. Please try again later.");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String resetTokenKey(String token) {
        return "auth:password-reset:token:" + sha256(token);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot hash reset token", ex);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

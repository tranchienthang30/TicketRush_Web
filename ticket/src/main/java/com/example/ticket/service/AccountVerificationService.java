package com.example.ticket.service;

import com.example.ticket.exception.AppException;
import com.example.ticket.model.entity.User;
import com.example.ticket.repository.UserRepository;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountVerificationService {
    private static final String TOKEN_KEY_PREFIX = "auth:email-verify:token:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final VerificationTokenService tokenService;
    private final EmailService emailService;
    private final String frontendUrl;

    public AccountVerificationService(
            UserRepository userRepository,
            VerificationTokenService tokenService,
            EmailService emailService,
            @Value("${frontend.url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public void sendVerificationEmail(User user) {
        sendVerificationEmail(user, false);
    }

    public void sendVerificationEmail(User user, boolean force) {
        if (!force && Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }
        String token = tokenService.createToken(TOKEN_KEY_PREFIX, user.getId().toString(), TOKEN_TTL);
        emailService.sendAccountVerificationEmail(
                user.getEmail(),
                user.getFullName(),
                frontendUrl + "/verify-email?token=" + token
        );
    }

    @Transactional
    public void verifyEmail(String token) {
        String userId = tokenService.consumeToken(TOKEN_KEY_PREFIX, token);
        if (userId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Verification token is invalid or expired");
        }
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Verification token is invalid or expired"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }
}

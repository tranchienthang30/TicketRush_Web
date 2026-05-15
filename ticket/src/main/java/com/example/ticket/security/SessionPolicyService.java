package com.example.ticket.security;

import com.example.ticket.model.enums.UserRole;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SessionPolicyService {
    private final Duration customerSessionDuration;
    private final Duration accessTokenDuration;
    private final Duration refreshTokenDuration;

    public SessionPolicyService(
            @Value("${app.session.customer-duration-ms:900000}") long customerSessionDurationMs,
            @Value("${app.jwt.expiration-ms:86400000}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.customerSessionDuration = Duration.ofMillis(customerSessionDurationMs);
        this.accessTokenDuration = Duration.ofMillis(accessExpirationMs);
        this.refreshTokenDuration = Duration.ofMillis(refreshExpirationMs);
    }

    public Duration accessDuration(UserRole role) {
        if (role == UserRole.CUSTOMER) {
            return customerSessionDuration;
        }
        return accessTokenDuration;
    }

    public Duration refreshDuration(UserRole role) {
        if (role == UserRole.CUSTOMER) {
            return customerSessionDuration;
        }
        return refreshTokenDuration;
    }
}

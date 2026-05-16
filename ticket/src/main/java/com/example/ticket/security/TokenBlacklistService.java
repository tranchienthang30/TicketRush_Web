package com.example.ticket.security;

import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:jti:";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(JwtTokenProvider jwtTokenProvider, StringRedisTemplate redisTemplate) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            Claims claims = jwtTokenProvider.parseClaims(token);
            String jti = claims.getId();
            Instant expiresAt = claims.getExpiration().toInstant();
            Duration ttl = Duration.between(Instant.now(), expiresAt);

            if (jti != null && !jti.isBlank() && !ttl.isNegative() && !ttl.isZero()) {
                redisTemplate.opsForValue().set(key(jti), "1", ttl);
            }
        } catch (Exception ignored) {
            // Expired or malformed tokens do not need a blacklist entry.
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            String jti = jwtTokenProvider.parseClaims(token).getId();
            if (jti == null || jti.isBlank()) {
                return true;
            }
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
        } catch (Exception ex) {
            return true;
        }
    }

    private String key(String jti) {
        return BLACKLIST_KEY_PREFIX + jti;
    }
}

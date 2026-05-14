package com.example.ticket.security;

import com.example.ticket.dto.response.AuthSessionResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {
    private final JwtTokenProvider jwtTokenProvider;

    public AuthCookieService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void addAuthCookies(HttpServletResponse response, AuthSessionResponse session) {
        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.createHttpOnlyCookie(
                CookieUtils.ACCESS_TOKEN_COOKIE,
                session.accessToken(),
                cookieDuration(session.accessExpiresAt())
        ).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.createHttpOnlyCookie(
                CookieUtils.REFRESH_TOKEN_COOKIE,
                session.refreshToken(),
                cookieDuration(session.refreshExpiresAt())
        ).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.clearCookie(CookieUtils.ACCESS_TOKEN_COOKIE).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.clearCookie(CookieUtils.REFRESH_TOKEN_COOKIE).toString());
    }

    private Duration cookieDuration(Instant expiresAt) {
        Duration duration = Duration.between(Instant.now(), expiresAt);
        if (duration.isNegative() || duration.isZero()) {
            return Duration.ZERO;
        }
        return duration;
    }
}

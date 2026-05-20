package com.example.ticket.security;

import com.example.ticket.dto.response.AuthSessionResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {
    private final JwtTokenProvider jwtTokenProvider;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthCookieService(
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.cookie.same-site:Lax}") String cookieSameSite
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    public void addAuthCookies(HttpServletResponse response, AuthSessionResponse session) {
        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.createHttpOnlyCookie(
                CookieUtils.ACCESS_TOKEN_COOKIE,
                session.accessToken(),
                cookieDuration(session.accessExpiresAt()),
                cookieSecure,
                cookieSameSite
        ).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.createHttpOnlyCookie(
                CookieUtils.REFRESH_TOKEN_COOKIE,
                session.refreshToken(),
                cookieDuration(session.refreshExpiresAt()),
                cookieSecure,
                cookieSameSite
        ).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                CookieUtils.clearCookie(CookieUtils.ACCESS_TOKEN_COOKIE, cookieSecure, cookieSameSite).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                CookieUtils.clearCookie(CookieUtils.REFRESH_TOKEN_COOKIE, cookieSecure, cookieSameSite).toString());
    }

    private Duration cookieDuration(Instant expiresAt) {
        Duration duration = Duration.between(Instant.now(), expiresAt);
        if (duration.isNegative() || duration.isZero()) {
            return Duration.ZERO;
        }
        return duration;
    }
}

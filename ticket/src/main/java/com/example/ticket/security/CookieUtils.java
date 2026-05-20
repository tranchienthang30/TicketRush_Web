package com.example.ticket.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;

public final class CookieUtils {
    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private CookieUtils() {
    }

    public static ResponseCookie createHttpOnlyCookie(String name, String value, Duration maxAge) {
        return createHttpOnlyCookie(name, value, maxAge, false, "Lax");
    }

    public static ResponseCookie createHttpOnlyCookie(
            String name,
            String value,
            Duration maxAge,
            boolean secure,
            String sameSite
    ) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(maxAge)
                .build();
    }

    public static ResponseCookie clearCookie(String name) {
        return clearCookie(name, false, "Lax");
    }

    public static ResponseCookie clearCookie(String name, boolean secure, String sameSite) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ZERO)
                .build();
    }

    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}

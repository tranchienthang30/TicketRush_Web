package com.example.ticket.security;

import com.example.ticket.dto.response.AuthSessionResponse;
import com.example.ticket.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(
            AuthService authService,
            AuthCookieService authCookieService,
            @Value("${frontend.url}") String frontendUrl
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String providerId = oauthUser.getAttribute("sub");
        String picture = oauthUser.getAttribute("picture");

        try {
            AuthSessionResponse session = authService.authenticateGoogleUser(email, name, providerId, picture);
            authCookieService.clearAuthCookies(response);
            authCookieService.addAuthCookies(response, session);
            response.sendRedirect(frontendUrl + "/oauth2/callback");
        } catch (Exception ex) {
            authCookieService.clearAuthCookies(response);
            String message = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + "/login?oauthError=" + message);
        }
    }
}

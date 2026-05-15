package com.example.ticket.service;

import com.example.ticket.exception.ApiException;
import com.example.ticket.security.JwtPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {
    public UUID resolve(UUID headerUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return principal.userId();
        }
        if (headerUserId != null) {
            return headerUserId;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "You need to sign in");
    }
}

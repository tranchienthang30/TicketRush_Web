package com.example.ticket.config;

import com.example.ticket.model.enums.UserRole;
import com.example.ticket.security.JwtPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userCachePolicy")
public class UserCachePolicy {
    public boolean allowCache() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.role() == UserRole.CUSTOMER;
        }

        return false;
    }
}

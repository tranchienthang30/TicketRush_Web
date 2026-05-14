package com.example.ticket.security;

import com.example.ticket.model.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record JwtPrincipal(UUID userId, String email, UserRole role, Instant expiresAt) {
}

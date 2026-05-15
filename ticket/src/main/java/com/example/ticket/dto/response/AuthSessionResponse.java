package com.example.ticket.dto.response;

import java.time.Instant;

public record AuthSessionResponse(
        String accessToken,
        String refreshToken,
        UserResponse user,
        Instant accessExpiresAt,
        Instant refreshExpiresAt
) {
}

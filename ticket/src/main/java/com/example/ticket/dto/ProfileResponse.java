package com.example.ticket.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String email,
        String fullName,
        String avatarUrl,
        String phone,
        String gender,
        LocalDate dateOfBirth,
        String role,
        String status,
        boolean emailVerified,
        String providerRequestStatus,
        OffsetDateTime providerRequestedAt,
        OffsetDateTime providerReviewedAt,
        String providerRejectionReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

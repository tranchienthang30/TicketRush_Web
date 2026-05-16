package com.example.ticket.dto.response;

import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.AuthProvider;
import com.example.ticket.model.enums.UserRole;
import com.example.ticket.model.enums.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        AuthProvider provider,
        Instant sessionExpiresAt,
        boolean emailVerified,
        UUID primaryOrganizationId,
        String providerRequestStatus,
        Instant providerRequestedAt,
        Instant providerReviewedAt,
        UUID providerReviewedBy,
        String providerRejectionReason
) {
    public static UserResponse from(User user) {
        return from(user, null);
    }

    public static UserResponse from(User user, Instant sessionExpiresAt) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.getProvider(),
                sessionExpiresAt,
                Boolean.TRUE.equals(user.getEmailVerified()),
                user.getPrimaryOrganizationId(),
                user.getProviderRequestStatus(),
                user.getProviderRequestedAt(),
                user.getProviderReviewedAt(),
                user.getProviderReviewedBy(),
                user.getProviderRejectionReason()
        );
    }
}

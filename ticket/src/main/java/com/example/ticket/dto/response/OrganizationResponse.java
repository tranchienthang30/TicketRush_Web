package com.example.ticket.dto.response;

import com.example.ticket.model.entity.Organization;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String businessEmail,
        UUID ownerId,
        Instant verifiedAt,
        Instant createdAt
) {
    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getBusinessEmail(),
                organization.getOwnerId(),
                organization.getVerifiedAt(),
                organization.getCreatedAt()
        );
    }
}

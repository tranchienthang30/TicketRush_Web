package com.example.ticket.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserMembershipResponse(
        UUID id,
        Long planId,
        String planName,
        String description,
        BigDecimal discountPercent,
        String status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        boolean active
) {
}

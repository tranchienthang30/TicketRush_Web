package com.example.ticket.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VirtualQueueStatusResponse(
        UUID eventId,
        String status,
        Long position,
        Long activeUsers,
        Long waitingUsers,
        OffsetDateTime accessExpiresAt,
        int releaseBatchSize,
        String message
) {
}

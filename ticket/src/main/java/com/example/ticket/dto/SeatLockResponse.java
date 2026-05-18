package com.example.ticket.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SeatLockResponse(
        UUID eventId,
        List<UUID> seatIds,
        OffsetDateTime lockExpiresAt
) {
}

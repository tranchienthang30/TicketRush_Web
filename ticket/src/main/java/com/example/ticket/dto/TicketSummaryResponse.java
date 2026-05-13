package com.example.ticket.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketSummaryResponse(
        UUID orderId,
        UUID eventId,
        String eventSlug,
        String title,
        String date,
        OffsetDateTime startTime,
        String location,
        String seat,
        String status,
        String image
) {
}

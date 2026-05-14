package com.example.ticket.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TicketDetailResponse(
        UUID orderId,
        UUID eventId,
        String eventSlug,
        String title,
        String location,
        String date,
        OffsetDateTime startTime,
        String orderStatus,
        BigDecimal totalAmount,
        String displayTotal,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt,
        List<TicketDetailItemResponse> tickets
) {
}

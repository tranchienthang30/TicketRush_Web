package com.example.ticket.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        UUID eventId,
        String eventTitle,
        String status,
        int ticketCount,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String displayTotal,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt
) {
}

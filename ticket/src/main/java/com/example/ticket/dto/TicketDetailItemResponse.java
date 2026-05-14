package com.example.ticket.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketDetailItemResponse(
        UUID orderItemId,
        UUID seatId,
        String seatCode,
        String ticketStatus,
        BigDecimal priceSnapshot,
        String qrCode,
        String qrContent,
        OffsetDateTime issuedAt,
        OffsetDateTime checkedInAt
) {
}

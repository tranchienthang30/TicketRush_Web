package com.example.ticket.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CheckoutTicketResponse(
        UUID orderItemId,
        UUID seatId,
        String seatCode,
        String ticketStatus,
        String qrCode,
        OffsetDateTime issuedAt
) {
}

package com.example.ticket.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CheckoutResultResponse(
        UUID orderId,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt,
        CheckoutSummaryResponse summary,
        List<CheckoutTicketResponse> tickets,
        String checkoutUrl
) {
}

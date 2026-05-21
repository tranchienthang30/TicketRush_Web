package com.example.ticket.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CheckoutSummaryResponse(
        UUID eventId,
        String eventTitle,
        int ticketCount,
        List<String> seatCodes,
        BigDecimal ticketSubtotal,
        BigDecimal serviceFee,
        BigDecimal membershipDiscount,
        BigDecimal voucherDiscount,
        BigDecimal totalAmount,
        String displayTotal,
        boolean membershipApplied,
        String appliedVoucherCode,
        OffsetDateTime seatLockExpiresAt
) {
}

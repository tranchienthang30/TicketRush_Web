package com.example.ticket.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderBookingSummaryResponse(
        UUID eventId,
        String title,
        long bookedTickets,
        BigDecimal grossRevenue,
        BigDecimal platformFeeRate,
        BigDecimal platformFeeAmount,
        BigDecimal providerRevenue
) {
}

package com.example.ticket.dto;

import java.math.BigDecimal;
import java.util.List;

public record MembershipPlanResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String displayPrice,
        Integer durationDays,
        BigDecimal discountPercent,
        boolean featured,
        List<String> perks
) {
}

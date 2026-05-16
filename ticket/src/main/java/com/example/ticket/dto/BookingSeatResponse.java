package com.example.ticket.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingSeatResponse(
        UUID id,
        UUID sectionId,
        String rowLabel,
        int seatNumber,
        String seatCode,
        BigDecimal price,
        String status,
        String seatTypeCode,
        Integer layoutX,
        Integer layoutY,
        boolean hidden,
        boolean accessible
) {
}

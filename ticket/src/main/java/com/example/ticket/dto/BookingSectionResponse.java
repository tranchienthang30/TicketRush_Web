package com.example.ticket.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BookingSectionResponse(
        UUID id,
        String name,
        BigDecimal basePrice,
        int rowCount,
        int seatsPerRow,
        int displayOrder,
        String seatTypeCode,
        String visualColorHex,
        List<BookingSeatResponse> seats
) {
}

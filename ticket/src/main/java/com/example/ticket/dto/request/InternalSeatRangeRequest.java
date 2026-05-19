package com.example.ticket.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InternalSeatRangeRequest(
        @NotNull(message = "Range start seat is required")
        @Min(value = 1, message = "Range start seat must be at least 1")
        Integer startSeat,

        @NotNull(message = "Range end seat is required")
        @Min(value = 1, message = "Range end seat must be at least 1")
        Integer endSeat,

        @NotBlank(message = "Seat type is required")
        String seatTypeCode,

        String seatTypeName,
        String visualColorHex,

        @NotNull(message = "Seat price is required")
        @DecimalMin(value = "0.0", message = "Seat price must be non-negative")
        BigDecimal price,

        String status,
        Boolean accessible
) {
}

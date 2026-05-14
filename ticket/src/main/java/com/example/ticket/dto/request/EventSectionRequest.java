package com.example.ticket.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EventSectionRequest(
        @NotBlank(message = "Section name is required")
        String name,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", message = "Base price must be non-negative")
        BigDecimal basePrice,

        @Min(value = 1, message = "Row count must be at least 1")
        Integer rowCount,

        @Min(value = 1, message = "Seats per row must be at least 1")
        Integer seatsPerRow
) {
}

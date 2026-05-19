package com.example.ticket.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InternalSeatRowRequest(
        @NotBlank(message = "Row label is required")
        String rowLabel,

        @NotNull(message = "Seat count is required")
        @Min(value = 1, message = "Seat count must be at least 1")
        Integer seatCount,

        @Valid
        @Size(max = 30, message = "Too many seat ranges in one row")
        List<InternalSeatRangeRequest> ranges
) {
}

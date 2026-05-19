package com.example.ticket.dto.request;

import jakarta.validation.constraints.Size;

public record SeatsioChartCreateRequest(
        @Size(max = 255, message = "Chart name must be at most 255 characters")
        String name,

        String venueType
) {
}

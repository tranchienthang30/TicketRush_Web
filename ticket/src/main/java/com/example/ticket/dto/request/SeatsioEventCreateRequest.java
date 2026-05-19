package com.example.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SeatsioEventCreateRequest(
        @NotBlank(message = "Seats.io chart key is required")
        String chartKey,

        @Size(max = 255, message = "Seats.io event name must be at most 255 characters")
        String name,

        String eventKey,

        LocalDate date
) {
}

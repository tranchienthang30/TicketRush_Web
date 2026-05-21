package com.example.ticket.dto.response;

public record SeatsioCategoryResponse(
        String key,
        String label,
        String color,
        boolean accessible
) {
}

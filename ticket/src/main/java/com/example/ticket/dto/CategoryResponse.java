package com.example.ticket.dto;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl
) {
}

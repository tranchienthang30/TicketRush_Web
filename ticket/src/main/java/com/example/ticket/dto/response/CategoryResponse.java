package com.example.ticket.dto.response;

import com.example.ticket.model.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl()
        );
    }
}

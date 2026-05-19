package com.example.ticket.dto.response;

public record SeatsioChartResponse(
        String key,
        String name,
        String status,
        String publishedVersionThumbnailUrl
) {
}

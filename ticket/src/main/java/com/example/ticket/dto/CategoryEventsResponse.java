package com.example.ticket.dto;

import java.util.List;

public record CategoryEventsResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        List<EventCardResponse> events
) {
}

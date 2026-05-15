package com.example.ticket.dto;

import java.util.UUID;

public record EventCardResponse(
        UUID id,
        String slug,
        String title,
        String date,
        String location,
        String price,
        String tag,
        String image,
        String category,
        Integer durationMinutes,
        String listingType,
        boolean bookable
) {
}

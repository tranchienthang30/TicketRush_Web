package com.example.ticket.dto;

import java.util.List;

public record EventPageResponse(
        List<EventCardResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

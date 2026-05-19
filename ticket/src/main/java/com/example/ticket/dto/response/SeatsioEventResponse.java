package com.example.ticket.dto.response;

public record SeatsioEventResponse(
        String eventKey,
        String chartKey,
        String name,
        String date
) {
}

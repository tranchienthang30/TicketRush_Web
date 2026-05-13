package com.example.ticket.dto;

import java.time.OffsetDateTime;

public record ActivityResponse(
        String title,
        String time,
        OffsetDateTime occurredAt
) {
}

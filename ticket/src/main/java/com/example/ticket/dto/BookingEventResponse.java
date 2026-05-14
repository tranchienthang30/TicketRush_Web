package com.example.ticket.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BookingEventResponse(
        UUID eventId,
        String slug,
        String title,
        String bannerUrl,
        String location,
        String status,
        OffsetDateTime startTime,
        OffsetDateTime saleStartTime,
        OffsetDateTime saleEndTime,
        long availableSeats,
        long soldSeats,
        List<BookingSectionResponse> sections
) {
}

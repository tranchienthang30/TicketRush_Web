package com.example.ticket.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record CreateEventRequest(
        @NotBlank(message = "Event title is required")
        String title,

        String description,
        String bannerUrl,

        @NotNull(message = "Category is required")
        Long categoryId,

        @NotBlank(message = "Venue is required")
        String locationName,

        @NotBlank(message = "City is required")
        String city,

        String address,

        @NotNull(message = "Start time is required")
        Instant startTime,

        @NotNull(message = "End time is required")
        Instant endTime,

        Instant saleStartTime,
        Instant saleEndTime,

        String seatProvider,
        String externalSeatChartKey,

        String payoutBankName,
        String payoutAccountName,
        String payoutAccountNumber,

        boolean termsAccepted,

        @Valid
        @Size(max = 30, message = "Too many sections")
        List<EventSectionRequest> sections
) {
    @AssertTrue(message = "End time must be after start time")
    public boolean isEventTimeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    @AssertTrue(message = "Sale end time must be after sale start time")
    public boolean isSaleTimeValid() {
        return saleStartTime == null || saleEndTime == null || saleEndTime.isAfter(saleStartTime);
    }

    @AssertTrue(message = "Terms must be accepted")
    public boolean isTermsAcceptedValid() {
        return termsAccepted;
    }
}

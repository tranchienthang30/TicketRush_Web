package com.example.ticket.dto.response;

import com.example.ticket.model.entity.Event;
import com.example.ticket.model.entity.EventSection;
import com.example.ticket.model.enums.EventStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID organizationId,
        Long categoryId,
        String title,
        String slug,
        String description,
        String genre,
        String country,
        String authorName,
        String directorName,
        String castMembers,
        String performerNames,
        String singerNames,
        String bannerUrl,
        Integer durationMinutes,
        String listingType,
        String locationName,
        String city,
        String address,
        Instant startTime,
        Instant endTime,
        Instant saleStartTime,
        Instant saleEndTime,
        EventStatus status,
        String seatProvider,
        String externalSeatChartKey,
        long totalSeats,
        BigDecimal minPrice,
        List<SectionResponse> sections
) {
    public static EventResponse from(Event event, List<EventSection> sections, long totalSeats) {
        BigDecimal minPrice = sections.stream()
                .map(EventSection::getBasePrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        return new EventResponse(
                event.getId(),
                event.getOrganizationId(),
                event.getCategoryId(),
                event.getTitle(),
                event.getSlug(),
                event.getDescription(),
                event.getGenre(),
                event.getCountry(),
                event.getAuthorName(),
                event.getDirectorName(),
                event.getCastMembers(),
                event.getPerformerNames(),
                event.getSingerNames(),
                event.getBannerUrl(),
                event.getDurationMinutes(),
                event.getListingType(),
                event.getLocationName(),
                event.getCity(),
                event.getAddress(),
                event.getStartTime(),
                event.getEndTime(),
                event.getSaleStartTime(),
                event.getSaleEndTime(),
                event.getStatus(),
                event.getSeatProvider(),
                event.getExternalSeatChartKey(),
                totalSeats,
                minPrice,
                sections.stream().map(SectionResponse::from).toList()
        );
    }

    public record SectionResponse(
            UUID id,
            String name,
            BigDecimal basePrice,
            Integer rowCount,
            Integer seatsPerRow,
            Integer displayOrder,
            int totalSeats
    ) {
        public static SectionResponse from(EventSection section) {
            return new SectionResponse(
                    section.getId(),
                    section.getName(),
                    section.getBasePrice(),
                    section.getRowCount(),
                    section.getSeatsPerRow(),
                    section.getDisplayOrder(),
                    section.getRowCount() * section.getSeatsPerRow()
            );
        }
    }
}

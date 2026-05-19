package com.example.ticket.dto.response;

import com.example.ticket.model.entity.Event;
import com.example.ticket.model.entity.EventSeat;
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
        String externalSeatWorkspaceKey,
        String externalSeatEventKey,
        long totalSeats,
        BigDecimal minPrice,
        List<SectionResponse> sections,
        List<InternalSeatRowResponse> internalSeatRows
) {
    public static EventResponse from(Event event, List<EventSection> sections, long totalSeats) {
        return from(event, sections, totalSeats, List.of());
    }

    public static EventResponse from(Event event, List<EventSection> sections, long totalSeats, List<EventSeat> seats) {
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
                event.getExternalSeatWorkspaceKey(),
                event.getExternalSeatEventKey(),
                totalSeats,
                minPrice,
                sections.stream().map(SectionResponse::from).toList(),
                buildInternalSeatRows(event, sections, seats)
        );
    }

    private static List<InternalSeatRowResponse> buildInternalSeatRows(Event event, List<EventSection> sections, List<EventSeat> seats) {
        if (!"INTERNAL".equals(event.getSeatProvider()) || seats == null || seats.isEmpty()) {
            return List.of();
        }
        java.util.Map<UUID, EventSection> sectionById = sections.stream()
                .collect(java.util.stream.Collectors.toMap(EventSection::getId, section -> section));

        return seats.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        EventSeat::getRowLabel,
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<EventSeat> rowSeats = entry.getValue().stream()
                            .sorted(java.util.Comparator.comparing(EventSeat::getSeatNumber))
                            .toList();
                    int seatCount = rowSeats.stream().mapToInt(EventSeat::getSeatNumber).max().orElse(0);
                    return new InternalSeatRowResponse(
                            entry.getKey(),
                            seatCount,
                            compactRanges(rowSeats, sectionById)
                    );
                })
                .toList();
    }

    private static List<InternalSeatRangeResponse> compactRanges(List<EventSeat> rowSeats, java.util.Map<UUID, EventSection> sectionById) {
        if (rowSeats.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<InternalSeatRangeResponse> ranges = new java.util.ArrayList<>();
        EventSeat start = rowSeats.get(0);
        EventSeat previous = start;

        for (int index = 1; index < rowSeats.size(); index++) {
            EventSeat current = rowSeats.get(index);
            if (!sameRange(previous, current) || current.getSeatNumber() != previous.getSeatNumber() + 1) {
                ranges.add(toRange(start, previous, sectionById));
                start = current;
            }
            previous = current;
        }
        ranges.add(toRange(start, previous, sectionById));
        return ranges;
    }

    private static boolean sameRange(EventSeat left, EventSeat right) {
        return java.util.Objects.equals(left.getSeatTypeCode(), right.getSeatTypeCode())
                && java.util.Objects.equals(left.getSectionId(), right.getSectionId())
                && java.util.Objects.equals(left.getPrice(), right.getPrice())
                && java.util.Objects.equals(left.getStatus(), right.getStatus())
                && java.util.Objects.equals(left.getAccessible(), right.getAccessible());
    }

    private static InternalSeatRangeResponse toRange(EventSeat start, EventSeat end, java.util.Map<UUID, EventSection> sectionById) {
        EventSection section = sectionById.get(start.getSectionId());
        return new InternalSeatRangeResponse(
                start.getSeatNumber(),
                end.getSeatNumber(),
                start.getSeatTypeCode(),
                section == null ? null : section.getName(),
                section == null ? null : section.getVisualColorHex(),
                start.getPrice(),
                start.getStatus() == null ? "AVAILABLE" : start.getStatus().name(),
                Boolean.TRUE.equals(start.getAccessible())
        );
    }

    public record SectionResponse(
            UUID id,
            String name,
            BigDecimal basePrice,
            Integer rowCount,
            Integer seatsPerRow,
            Integer displayOrder,
            String seatTypeCode,
            String visualColorHex,
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
                    section.getSeatTypeCode(),
                    section.getVisualColorHex(),
                    section.getRowCount() * section.getSeatsPerRow()
            );
        }
    }

    public record InternalSeatRowResponse(
            String rowLabel,
            int seatCount,
            List<InternalSeatRangeResponse> ranges
    ) {
    }

    public record InternalSeatRangeResponse(
            int startSeat,
            int endSeat,
            String seatTypeCode,
            String seatTypeName,
            String visualColorHex,
            BigDecimal price,
            String status,
            boolean accessible
    ) {
    }
}

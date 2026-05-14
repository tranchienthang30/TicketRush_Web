package com.example.ticket.service;

import com.example.ticket.dto.CategoryEventsResponse;
import com.example.ticket.dto.CategoryResponse;
import com.example.ticket.dto.BookingEventResponse;
import com.example.ticket.dto.BookingSectionResponse;
import com.example.ticket.dto.BookingSeatResponse;
import com.example.ticket.dto.EventCardResponse;
import com.example.ticket.dto.EventPageResponse;
import com.example.ticket.exception.ApiException;
import com.example.ticket.repository.EventQueryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private final EventQueryRepository eventRepository;

    public EventService(EventQueryRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<CategoryResponse> getCategories() {
        return eventRepository.findActiveCategories();
    }

    public List<CategoryEventsResponse> getGroupedEvents(int limitPerCategory) {
        int safeLimit = clamp(limitPerCategory, 1, 12);

        return eventRepository.findActiveCategories().stream()
                .map(category -> new CategoryEventsResponse(
                        category.id(),
                        category.name(),
                        category.slug(),
                        category.description(),
                        category.imageUrl(),
                        toEventCards(eventRepository.findPublishedEventsByCategory(category.id(), safeLimit))
                ))
                .filter(category -> !category.events().isEmpty())
                .toList();
    }

    public EventPageResponse searchEvents(Long categoryId, String query, String city, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clamp(size, 1, 50);
        long total = eventRepository.countPublishedEvents(categoryId, query, city);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        List<EventCardResponse> content = toEventCards(
                eventRepository.findPublishedEvents(categoryId, query, city, safePage, safeSize)
        );

        return new EventPageResponse(content, safePage, safeSize, total, totalPages);
    }

    public BookingEventResponse getBookingEvent(UUID eventId) {
        EventQueryRepository.BookingEventRow event = eventRepository.findPublishedEventForBooking(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        List<EventQueryRepository.BookingSectionRow> sections = eventRepository.findSectionsByEvent(eventId);
        Map<UUID, List<BookingSeatResponse>> seatsBySection = eventRepository.findSeatsByEvent(eventId).stream()
                .collect(Collectors.groupingBy(
                        EventQueryRepository.BookingSeatRow::sectionId,
                        Collectors.mapping(row -> new BookingSeatResponse(
                                row.id(),
                                row.sectionId(),
                                row.rowLabel(),
                                row.seatNumber(),
                                row.seatCode(),
                                row.price(),
                                row.status(),
                                row.seatTypeCode(),
                                row.layoutX(),
                                row.layoutY(),
                                row.hidden(),
                                row.accessible()
                        ), Collectors.toList())
                ));

        List<BookingSectionResponse> sectionResponses = sections.stream()
                .map(section -> new BookingSectionResponse(
                        section.id(),
                        section.name(),
                        section.basePrice(),
                        section.rowCount(),
                        section.seatsPerRow(),
                        section.displayOrder(),
                        seatsBySection.getOrDefault(section.id(), List.of())
                ))
                .toList();

        return new BookingEventResponse(
                event.id(),
                event.slug(),
                event.title(),
                event.bannerUrl(),
                event.location(),
                event.hallName(),
                event.status(),
                event.startTime(),
                event.saleStartTime(),
                event.saleEndTime(),
                event.availableSeats(),
                event.soldSeats(),
                sectionResponses
        );
    }

    private List<EventCardResponse> toEventCards(List<EventQueryRepository.EventRow> rows) {
        return rows.stream().map(this::toEventCard).toList();
    }

    private EventCardResponse toEventCard(EventQueryRepository.EventRow row) {
        return new EventCardResponse(
                row.id(),
                row.slug(),
                row.title(),
                formatDate(row.startTime()),
                row.location(),
                formatPrice(row.minPrice()),
                resolveTag(row),
                row.bannerUrl()
        );
    }

    private String formatDate(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.atZoneSameInstant(APP_ZONE).format(DATE_FORMATTER);
    }

    private String formatPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return "Free";
        }
        return "from " + NumberFormat.getNumberInstance(VIETNAM).format(price) + " VND";
    }

    private String resolveTag(EventQueryRepository.EventRow row) {
        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
        long knownSeats = row.availableSeats() + row.soldSeats();

        if (row.saleStartTime() != null && row.saleStartTime().isAfter(now)) {
            return "Coming Soon";
        }

        if (knownSeats > 0 && row.availableSeats() == 0) {
            return "Sold Out";
        }

        if (row.availableSeats() > 0 && row.availableSeats() <= 5) {
            return "Selling Fast";
        }

        if (row.soldSeats() > 0) {
            return "Hot";
        }

        return "New";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

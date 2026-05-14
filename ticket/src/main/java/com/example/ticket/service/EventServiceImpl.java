package com.example.ticket.service;

import com.example.ticket.dto.request.CreateEventRequest;
import com.example.ticket.dto.request.EventSectionRequest;
import com.example.ticket.dto.BookingEventResponse;
import com.example.ticket.dto.BookingSectionResponse;
import com.example.ticket.dto.BookingSeatResponse;
import com.example.ticket.dto.CategoryEventsResponse;
import com.example.ticket.dto.CategoryResponse;
import com.example.ticket.dto.EventCardResponse;
import com.example.ticket.dto.EventPageResponse;
import com.example.ticket.dto.response.EventResponse;
import com.example.ticket.exception.ApiException;
import com.example.ticket.exception.AppException;
import com.example.ticket.model.entity.Event;
import com.example.ticket.model.entity.EventSeat;
import com.example.ticket.model.entity.EventSection;
import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.EventStatus;
import com.example.ticket.model.enums.SeatStatus;
import com.example.ticket.model.enums.UserRole;
import com.example.ticket.repository.CategoryRepository;
import com.example.ticket.repository.EventQueryRepository;
import com.example.ticket.repository.EventRepository;
import com.example.ticket.repository.EventSeatRepository;
import com.example.ticket.repository.EventSectionRepository;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.security.JwtPrincipal;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventServiceImpl implements EventService {
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private final EventRepository eventRepository;
    private final EventQueryRepository eventQueryRepository;
    private final EventSectionRepository sectionRepository;
    private final EventSeatRepository seatRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public EventServiceImpl(
            EventRepository eventRepository,
            EventQueryRepository eventQueryRepository,
            EventSectionRepository sectionRepository,
            EventSeatRepository seatRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.eventRepository = eventRepository;
        this.eventQueryRepository = eventQueryRepository;
        this.sectionRepository = sectionRepository;
        this.seatRepository = seatRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    public EventResponse createEvent(CreateEventRequest request) {
        User user = currentUser();
        if (user.getRole() != UserRole.PROVIDER && user.getRole() != UserRole.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "You need provider access before creating movies");
        }
        if (user.getPrimaryOrganizationId() == null && user.getRole() != UserRole.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "You need a verified business profile before creating movies");
        }
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Category does not exist");
        }

        String seatProvider = normalizeSeatProvider(request.seatProvider());
        if ("INTERNAL".equals(seatProvider) && (request.sections() == null || request.sections().isEmpty())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "At least one section is required for internal seat maps");
        }
        if ("SEATS_IO".equals(seatProvider) && isBlank(request.externalSeatChartKey())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Seats.io chart key is required");
        }

        Event event = Event.builder()
                .providerId(user.getId())
                .organizationId(user.getPrimaryOrganizationId())
                .categoryId(request.categoryId())
                .title(request.title().trim())
                .slug(uniqueSlug(request.title()))
                .description(blankToNull(request.description()))
                .bannerUrl(blankToNull(request.bannerUrl()))
                .locationName(request.locationName().trim())
                .city(request.city().trim())
                .address(blankToNull(request.address()))
                .startTime(request.startTime())
                .endTime(request.endTime())
                .saleStartTime(request.saleStartTime())
                .saleEndTime(request.saleEndTime())
                .status(EventStatus.PUBLISHED)
                .seatProvider(seatProvider)
                .externalSeatChartKey(blankToNull(request.externalSeatChartKey()))
                .payoutBankName(blankToNull(request.payoutBankName()))
                .payoutAccountName(blankToNull(request.payoutAccountName()))
                .payoutAccountNumber(blankToNull(request.payoutAccountNumber()))
                .providerTermsAcceptedAt(Instant.now())
                .build();

        eventRepository.save(event);

        List<EventSection> sections = createSections(event, request.sections());
        if ("INTERNAL".equals(seatProvider)) {
            createSeats(event, sections);
        }

        long totalSeats = "INTERNAL".equals(seatProvider)
                ? seatRepository.countByEventId(event.getId())
                : sections.stream().mapToLong(section -> section.getRowCount() * section.getSeatsPerRow()).sum();
        return EventResponse.from(event, sections, totalSeats);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> publicEvents() {
        return eventRepository.findByStatusOrderByStartTimeAsc(EventStatus.PUBLISHED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> myEvents() {
        UUID userId = currentUserId();
        return eventRepository.findByProviderIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse eventBySlug(String slug) {
        return toResponse(eventRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Event does not exist")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return eventQueryRepository.findActiveCategories();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryEventsResponse> getGroupedEvents(int limitPerCategory) {
        int safeLimit = clamp(limitPerCategory, 1, 12);

        return eventQueryRepository.findActiveCategories().stream()
                .map(category -> new CategoryEventsResponse(
                        category.id(),
                        category.name(),
                        category.slug(),
                        category.description(),
                        category.imageUrl(),
                        toEventCards(eventQueryRepository.findPublishedEventsByCategory(category.id(), safeLimit))
                ))
                .filter(category -> !category.events().isEmpty())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventPageResponse searchEvents(Long categoryId, String query, String city, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clamp(size, 1, 50);
        long total = eventQueryRepository.countPublishedEvents(categoryId, query, city);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        List<EventCardResponse> content = toEventCards(
                eventQueryRepository.findPublishedEvents(categoryId, query, city, safePage, safeSize)
        );

        return new EventPageResponse(content, safePage, safeSize, total, totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingEventResponse getBookingEvent(UUID eventId) {
        EventQueryRepository.BookingEventRow event = eventQueryRepository.findPublishedEventForBooking(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movie not found"));

        List<EventQueryRepository.BookingSectionRow> sections = eventQueryRepository.findSectionsByEvent(eventId);
        Map<UUID, List<BookingSeatResponse>> seatsBySection = eventQueryRepository.findSeatsByEvent(eventId).stream()
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

    private List<EventSection> createSections(Event event, List<EventSectionRequest> sectionRequests) {
        List<EventSection> sections = new ArrayList<>();
        if (sectionRequests == null) {
            return sections;
        }
        int order = 0;
        for (EventSectionRequest request : sectionRequests) {
            EventSection section = new EventSection();
            section.setEvent(event);
            section.setName(request.name().trim());
            section.setBasePrice(request.basePrice());
            section.setRowCount(request.rowCount());
            section.setSeatsPerRow(request.seatsPerRow());
            section.setDisplayOrder(order++);
            sections.add(sectionRepository.save(section));
        }
        return sections;
    }

    private void createSeats(Event event, List<EventSection> sections) {
        for (EventSection section : sections) {
            for (int row = 0; row < section.getRowCount(); row++) {
                String rowLabel = rowLabel(row);
                for (int seatNumber = 1; seatNumber <= section.getSeatsPerRow(); seatNumber++) {
                    EventSeat seat = new EventSeat();
                    seat.setEventId(event.getId());
                    seat.setSectionId(section.getId());
                    seat.setRowLabel(rowLabel);
                    seat.setSeatNumber(seatNumber);
                    seat.setSeatCode(section.getName().toUpperCase(Locale.ROOT).replaceAll("\\s+", "-") + "-" + rowLabel + seatNumber);
                    seat.setPrice(section.getBasePrice());
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setVersion(0);
                    seatRepository.save(seat);
                }
            }
        }
    }

    private EventResponse toResponse(Event event) {
        List<EventSection> sections = sectionRepository.findByEvent_IdOrderByDisplayOrderAsc(event.getId());
        long totalSeats = seatRepository.countByEventId(event.getId());
        if (totalSeats == 0 && "SEATS_IO".equals(event.getSeatProvider())) {
            totalSeats = sections.stream().mapToLong(section -> section.getRowCount() * section.getSeatsPerRow()).sum();
        }
        return EventResponse.from(event, sections, totalSeats);
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

    private User currentUser() {
        return userRepository.findById(currentUserId())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User does not exist"));
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "You need to sign in");
        }
        return principal.userId();
    }

    private String uniqueSlug(String title) {
        String base = slugify(title);
        String slug = base;
        int suffix = 2;
        while (eventRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private String slugify(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        return slug.isBlank() ? "event" : slug;
    }

    private String rowLabel(int index) {
        StringBuilder label = new StringBuilder();
        int value = index;
        do {
            label.insert(0, (char) ('A' + (value % 26)));
            value = value / 26 - 1;
        } while (value >= 0);
        return label.toString();
    }

    private String normalizeSeatProvider(String seatProvider) {
        if (seatProvider == null || seatProvider.isBlank()) {
            return "INTERNAL";
        }
        String normalized = seatProvider.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("INTERNAL") && !normalized.equals("SEATS_IO")) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Seat provider is invalid");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

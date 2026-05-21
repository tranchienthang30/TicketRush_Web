package com.example.ticket.service;

import com.example.ticket.dto.request.CreateEventRequest;
import com.example.ticket.dto.request.EventSectionRequest;
import com.example.ticket.dto.request.InternalSeatRangeRequest;
import com.example.ticket.dto.request.InternalSeatRowRequest;
import com.example.ticket.dto.request.SeatsioEventCreateRequest;
import com.example.ticket.dto.BookingEventResponse;
import com.example.ticket.dto.BookingSectionResponse;
import com.example.ticket.dto.BookingSeatResponse;
import com.example.ticket.dto.CategoryEventsResponse;
import com.example.ticket.dto.CategoryResponse;
import com.example.ticket.dto.EventCardResponse;
import com.example.ticket.dto.EventPageResponse;
import com.example.ticket.dto.response.EventResponse;
import com.example.ticket.dto.response.ProviderBookingSummaryResponse;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.05");
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private final EventRepository eventRepository;
    private final EventQueryRepository eventQueryRepository;
    private final EventSectionRepository sectionRepository;
    private final EventSeatRepository seatRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SeatsioProviderService seatsioProviderService;

    public EventServiceImpl(
            EventRepository eventRepository,
            EventQueryRepository eventQueryRepository,
            EventSectionRepository sectionRepository,
            EventSeatRepository seatRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            SeatsioProviderService seatsioProviderService
    ) {
        this.eventRepository = eventRepository;
        this.eventQueryRepository = eventQueryRepository;
        this.sectionRepository = sectionRepository;
        this.seatRepository = seatRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.seatsioProviderService = seatsioProviderService;
    }

    @Override
    public EventResponse createEvent(CreateEventRequest request) {
        User user = currentUser();
        if (user.getRole() != UserRole.PROVIDER && user.getRole() != UserRole.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "You need approved provider access before creating events");
        }
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Category does not exist");
        }

        String seatProvider = normalizeSeatProvider(request.seatProvider());
        String listingType = normalizeListingType(request.listingType());
        SaleWindow saleWindow = resolveSaleWindow(request, listingType);
        validateSections(request, seatProvider);
        SeatsioKeys seatsioKeys = resolveSeatsioKeys(user, request, seatProvider);

        Event event = Event.builder()
                .providerId(user.getId())
                .organizationId(null)
                .categoryId(request.categoryId())
                .title(request.title().trim())
                .slug(uniqueSlug(request.title()))
                .description(blankToNull(request.description()))
                .genre(blankToNull(request.genre()))
                .country(blankToNull(request.country()))
                .authorName(blankToNull(request.authorName()))
                .directorName(blankToNull(request.directorName()))
                .castMembers(blankToNull(request.castMembers()))
                .performerNames(blankToNull(request.performerNames()))
                .singerNames(blankToNull(request.singerNames()))
                .bannerUrl(blankToNull(request.bannerUrl()))
                .durationMinutes(resolveDurationMinutes(request))
                .listingType(listingType)
                .locationName(request.locationName().trim())
                .city(request.city().trim())
                .address(blankToNull(request.address()))
                .startTime(request.startTime())
                .endTime(request.endTime())
                .saleStartTime(saleWindow.saleStartTime())
                .saleEndTime(saleWindow.saleEndTime())
                .status(EventStatus.PUBLISHED)
                .seatProvider(seatProvider)
                .externalSeatChartKey(seatsioKeys.chartKey())
                .externalSeatWorkspaceKey(seatsioKeys.workspaceKey())
                .externalSeatEventKey(seatsioKeys.eventKey())
                .payoutBankName(blankToNull(request.payoutBankName()))
                .payoutAccountName(blankToNull(request.payoutAccountName()))
                .payoutAccountNumber(blankToNull(request.payoutAccountNumber()))
                .providerTermsAcceptedAt(Instant.now())
                .build();

        eventRepository.save(event);

        List<EventSection> sections = "INTERNAL".equals(seatProvider) && hasInternalSeatRows(request)
                ? createInternalSeatSections(event, request.internalSeatRows())
                : createSections(event, request.sections());
        if ("INTERNAL".equals(seatProvider)) {
            if (hasInternalSeatRows(request)) {
                createSeatsFromRows(event, sections, request.internalSeatRows());
            } else {
                createSeats(event, sections);
            }
        }

        long totalSeats = "INTERNAL".equals(seatProvider)
                ? seatRepository.countByEventId(event.getId())
                : sections.stream().mapToLong(section -> section.getRowCount() * section.getSeatsPerRow()).sum();
        return EventResponse.from(event, sections, totalSeats);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse myEvent(UUID eventId) {
        User user = currentUser();
        Event event = ownedEvent(eventId, user);
        return toResponse(event);
    }

    @Override
    public EventResponse updateEvent(UUID eventId, CreateEventRequest request) {
        User user = currentUser();
        Event event = ownedEvent(eventId, user);
        if (hasSaleStarted(event)) {
            throw new AppException(HttpStatus.CONFLICT, "Can't edit while starting sell");
        }
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Category does not exist");
        }

        String seatProvider = normalizeSeatProvider(request.seatProvider());
        String listingType = normalizeListingType(request.listingType());
        SaleWindow saleWindow = resolveSaleWindow(request, listingType);
        if (!seatProvider.equals(event.getSeatProvider())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Seat provider cannot be changed after event creation");
        }
        validateSections(request, seatProvider);
        SeatsioKeys seatsioKeys = resolveSeatsioKeys(user, request, seatProvider);

        event.setCategoryId(request.categoryId());
        event.setTitle(request.title().trim());
        event.setDescription(blankToNull(request.description()));
        event.setGenre(blankToNull(request.genre()));
        event.setCountry(blankToNull(request.country()));
        event.setAuthorName(blankToNull(request.authorName()));
        event.setDirectorName(blankToNull(request.directorName()));
        event.setCastMembers(blankToNull(request.castMembers()));
        event.setPerformerNames(blankToNull(request.performerNames()));
        event.setSingerNames(blankToNull(request.singerNames()));
        event.setBannerUrl(blankToNull(request.bannerUrl()));
        event.setDurationMinutes(resolveDurationMinutes(request));
        event.setListingType(listingType);
        event.setLocationName(request.locationName().trim());
        event.setCity(request.city().trim());
        event.setAddress(blankToNull(request.address()));
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setSaleStartTime(saleWindow.saleStartTime());
        event.setSaleEndTime(saleWindow.saleEndTime());
        event.setExternalSeatChartKey(seatsioKeys.chartKey());
        event.setExternalSeatWorkspaceKey(seatsioKeys.workspaceKey());
        event.setExternalSeatEventKey(seatsioKeys.eventKey());
        event.setPayoutBankName(blankToNull(request.payoutBankName()));
        event.setPayoutAccountName(blankToNull(request.payoutAccountName()));
        event.setPayoutAccountNumber(blankToNull(request.payoutAccountNumber()));
        event.setProviderTermsAcceptedAt(Instant.now());
        Event saved = eventRepository.save(event);

        if ("INTERNAL".equals(seatProvider) && hasInternalSeatRows(request)) {
            replaceInternalSeatMap(saved, request.internalSeatRows());
        } else if ("SEATS_IO".equals(seatProvider)) {
            sectionRepository.deleteByEvent_Id(eventId);
            sectionRepository.flush();
            createSections(saved, request.sections());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderBookingSummaryResponse providerBookingSummary(UUID eventId) {
        User user = currentUser();
        ownedEvent(eventId, user);
        EventQueryRepository.ProviderBookingSummaryRow row = eventQueryRepository.findProviderBookingSummary(eventId);
        BigDecimal grossRevenue = row.grossRevenue() == null ? BigDecimal.ZERO : row.grossRevenue();
        BigDecimal platformFeeAmount = grossRevenue.multiply(PLATFORM_FEE_RATE);
        return new ProviderBookingSummaryResponse(
                row.eventId(),
                row.title(),
                row.bookedTickets(),
                grossRevenue,
                PLATFORM_FEE_RATE,
                platformFeeAmount,
                grossRevenue.subtract(platformFeeAmount)
        );
    }

    private void replaceInternalSeatMap(Event event, List<InternalSeatRowRequest> rows) {
        if (seatRepository.existsOrderItemByEventId(event.getId())) {
            throw new AppException(HttpStatus.CONFLICT, "Seat map cannot be rebuilt after tickets have been ordered");
        }
        if (seatRepository.existsByEventIdAndStatus(event.getId(), SeatStatus.LOCKED)) {
            throw new AppException(HttpStatus.CONFLICT, "Seat map cannot be rebuilt while seats are locked for checkout");
        }

        seatRepository.deleteByEventId(event.getId());
        seatRepository.flush();
        sectionRepository.deleteByEvent_Id(event.getId());
        sectionRepository.flush();

        List<EventSection> sections = createInternalSeatSections(event, rows);
        createSeatsFromRows(event, sections, rows);
    }

    private SeatsioKeys resolveSeatsioKeys(User user, CreateEventRequest request, String seatProvider) {
        if (!"SEATS_IO".equals(seatProvider)) {
            return new SeatsioKeys(null, null, null);
        }

        String chartKey = blankToNull(request.externalSeatChartKey());
        if (chartKey == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Seats.io chart key is required");
        }

        String workspaceKey = blankToNull(request.externalSeatWorkspaceKey());
        if (workspaceKey == null) {
            var workspace = seatsioProviderService.workspaceForProvider(user.getId());
            if (workspace != null) {
                workspaceKey = workspace.getWorkspaceKey();
            }
        }
        if (workspaceKey == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Seats.io workspace key is required");
        }

        String eventKey = blankToNull(request.externalSeatEventKey());
        if (eventKey == null) {
            LocalDate eventDate = request.startTime().atZone(APP_ZONE).toLocalDate();
            eventKey = seatsioProviderService.createEvent(
                    user.getId(),
                    new SeatsioEventCreateRequest(chartKey, request.title(), null, eventDate)
            ).eventKey();
        }

        return new SeatsioKeys(chartKey, workspaceKey, eventKey);
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
    public BookingEventResponse getBookingEvent(UUID eventId, UUID viewerUserId) {
        EventQueryRepository.BookingEventRow event = eventQueryRepository.findPublishedEventForBooking(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event is not available for booking"));

        List<EventQueryRepository.BookingSectionRow> sections = eventQueryRepository.findSectionsByEvent(eventId);
        Map<UUID, List<BookingSeatResponse>> seatsBySection = eventQueryRepository.findSeatsByEvent(eventId, viewerUserId).stream()
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
                                row.seatTypeName(),
                                row.visualColorHex(),
                                row.layoutX(),
                                row.layoutY(),
                                row.hidden(),
                                row.accessible(),
                                row.lockExpiresAt(),
                                row.lockOwnerUserId(),
                                row.lockedByCurrentUser()
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
                        section.seatTypeCode(),
                        section.visualColorHex(),
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
                event.seatProvider(),
                event.externalSeatWorkspaceKey(),
                event.externalSeatChartKey(),
                event.externalSeatEventKey(),
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
            section.setRowCount(request.rowCount() == null ? 1 : request.rowCount());
            section.setSeatsPerRow(request.seatsPerRow() == null ? 1 : request.seatsPerRow());
            section.setDisplayOrder(order++);
            sections.add(sectionRepository.save(section));
        }
        return sections;
    }

    private List<EventSection> createInternalSeatSections(Event event, List<InternalSeatRowRequest> rows) {
        Map<String, SeatTypeSummary> summaries = new java.util.LinkedHashMap<>();
        for (InternalSeatRowRequest row : rows) {
            for (int seatNumber = 1; seatNumber <= row.seatCount(); seatNumber++) {
                SeatAssignment assignment = assignmentForSeat(row, seatNumber);
                String key = seatTypeKey(assignment.seatTypeName());
                summaries.compute(key, (ignored, summary) -> {
                    if (summary == null) {
                        return new SeatTypeSummary(
                                assignment.seatTypeName(),
                                assignment.seatTypeCode(),
                                assignment.visualColorHex(),
                                assignment.price(),
                                1
                        );
                    }
                    return summary.increment(assignment.price());
                });
            }
        }

        List<EventSection> sections = new ArrayList<>();
        int order = 0;
        for (SeatTypeSummary summary : summaries.values()) {
            EventSection section = new EventSection();
            section.setEvent(event);
            section.setName(summary.name());
            section.setBasePrice(summary.minimumPrice());
            section.setRowCount(1);
            section.setSeatsPerRow(summary.seatCount());
            section.setDisplayOrder(order++);
            section.setSeatTypeCode(summary.seatTypeCode());
            section.setVisualColorHex(summary.visualColorHex());
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
                    seat.setSeatTypeCode("STANDARD");
                    seat.setLayoutX(seatNumber);
                    seat.setLayoutY(row + 1);
                    seat.setAccessible(false);
                    seat.setHidden(false);
                    seat.setVersion(0);
                    seatRepository.save(seat);
                }
            }
        }
    }

    private void createSeatsFromRows(Event event, List<EventSection> sections, List<InternalSeatRowRequest> rows) {
        Map<String, EventSection> sectionByType = sections.stream()
                .collect(Collectors.toMap(
                        section -> seatTypeKey(section.getName()),
                        section -> section,
                        (left, right) -> left
                ));

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            InternalSeatRowRequest row = rows.get(rowIndex);
            String rowLabel = row.rowLabel().trim().toUpperCase(Locale.ROOT);
            for (int seatNumber = 1; seatNumber <= row.seatCount(); seatNumber++) {
                SeatAssignment assignment = assignmentForSeat(row, seatNumber);
                EventSection section = sectionByType.get(seatTypeKey(assignment.seatTypeName()));
                if (section == null) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Internal seat section mapping is invalid");
                }

                EventSeat seat = new EventSeat();
                seat.setEventId(event.getId());
                seat.setSectionId(section.getId());
                seat.setRowLabel(rowLabel);
                seat.setSeatNumber(seatNumber);
                seat.setSeatCode(rowLabel + seatNumber);
                seat.setPrice(assignment.price());
                seat.setStatus(assignment.status());
                seat.setSeatTypeCode(assignment.seatTypeCode());
                seat.setLayoutX(seatNumber);
                seat.setLayoutY(rowIndex + 1);
                seat.setAccessible(assignment.accessible());
                seat.setHidden(false);
                seat.setVersion(0);
                seatRepository.save(seat);
            }
        }
    }

    private EventResponse toResponse(Event event) {
        List<EventSection> sections = sectionRepository.findByEvent_IdOrderByDisplayOrderAsc(event.getId());
        long totalSeats = seatRepository.countByEventId(event.getId());
        if (totalSeats == 0 && "SEATS_IO".equals(event.getSeatProvider())) {
            totalSeats = sections.stream().mapToLong(section -> section.getRowCount() * section.getSeatsPerRow()).sum();
        }
        List<EventSeat> seats = "INTERNAL".equals(event.getSeatProvider())
                ? seatRepository.findByEventIdOrderByLayoutYAscLayoutXAscSeatNumberAsc(event.getId())
                : List.of();
        return EventResponse.from(event, sections, totalSeats, seats);
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
                row.bannerUrl(),
                row.categoryName(),
                row.durationMinutes(),
                row.listingType(),
                isBookingAvailable(row)
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
        String listingType = normalizeListingType(row.listingType());

        if (row.saleStartTime() != null && row.saleStartTime().isAfter(now)) {
            return "Coming Soon";
        }
        if (knownSeats > 0 && row.availableSeats() == 0) {
            return "Sold Out";
        }
        if ("SPECIAL".equals(listingType)) {
            return "Special";
        }
        if ("UPCOMING".equals(listingType)) {
            return "Upcoming";
        }
        if (row.availableSeats() > 0 && row.availableSeats() <= 5) {
            return "Selling Fast";
        }
        if (row.soldSeats() > 0) {
            return "Hot";
        }
        return "New";
    }

    private boolean isBookingAvailable(EventQueryRepository.EventRow row) {
        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);

        if (row.saleStartTime() != null && row.saleStartTime().isAfter(now)) {
            return false;
        }
        if (row.saleEndTime() != null && row.saleEndTime().isBefore(now)) {
            return false;
        }
        return row.availableSeats() > 0;
    }

    private boolean hasSaleStarted(Event event) {
        return event.getSaleStartTime() != null && !event.getSaleStartTime().isAfter(Instant.now());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private User currentUser() {
        return userRepository.findById(currentUserId())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User does not exist"));
    }

    private Event ownedEvent(UUID eventId, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return eventRepository.findById(eventId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Event does not exist"));
        }
        return eventRepository.findByIdAndProviderId(eventId, user.getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Event does not exist"));
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

    private int resolveDurationMinutes(CreateEventRequest request) {
        if (request.durationMinutes() != null) {
            return request.durationMinutes();
        }
        return Math.max(1, (int) ChronoUnit.MINUTES.between(request.startTime(), request.endTime()));
    }

    private String normalizeListingType(String listingType) {
        if (listingType == null || listingType.isBlank()) {
            return "NOW_SHOWING";
        }
        String normalized = listingType.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "NOW_SHOWING", "UPCOMING", "SPECIAL" -> normalized;
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "Event listing type is invalid");
        };
    }

    private SaleWindow resolveSaleWindow(CreateEventRequest request, String listingType) {
        Instant saleStartTime = request.saleStartTime();
        Instant saleEndTime = request.saleEndTime();
        Instant now = Instant.now();

        if ("NOW_SHOWING".equals(listingType) || "SPECIAL".equals(listingType)) {
            if (saleStartTime == null || saleStartTime.isAfter(now)) {
                saleStartTime = now;
            }
            if (saleEndTime == null || saleEndTime.isBefore(now)) {
                saleEndTime = request.endTime().isAfter(now)
                        ? request.endTime()
                        : now.plus(30, ChronoUnit.DAYS);
            }
        }

        return new SaleWindow(saleStartTime, saleEndTime);
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

    private void validateSections(CreateEventRequest request, String seatProvider) {
        if ("INTERNAL".equals(seatProvider) && hasInternalSeatRows(request)) {
            validateInternalSeatRows(request.internalSeatRows());
            return;
        }
        if (request.sections() == null || request.sections().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "SEATS_IO".equals(seatProvider)
                            ? "At least one seats.io category price is required"
                            : "At least one section is required for internal seat maps");
        }
        if ("INTERNAL".equals(seatProvider)) {
            for (EventSectionRequest section : request.sections()) {
                if (section.rowCount() == null || section.rowCount() < 1
                        || section.seatsPerRow() == null || section.seatsPerRow() < 1) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Rows and seats per row are required for internal seat maps");
                }
            }
        }
    }

    private void validateInternalSeatRows(List<InternalSeatRowRequest> rows) {
        Set<String> rowLabels = new HashSet<>();
        Map<String, String> colorBySeatTypeName = new HashMap<>();
        for (InternalSeatRowRequest row : rows) {
            if (!rowLabels.add(row.rowLabel().trim().toUpperCase(Locale.ROOT))) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Seat row labels must be unique");
            }
            List<InternalSeatRangeRequest> ranges = row.ranges() == null ? List.of() : row.ranges();
            boolean[] occupied = new boolean[row.seatCount() + 1];

            for (InternalSeatRangeRequest range : ranges) {
                if (range.startSeat() > range.endSeat()) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Seat range start must be before end");
                }
                if (range.endSeat() > row.seatCount()) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Seat range must be inside the row seat count");
                }
                normalizeSeatType(range.seatTypeCode());
                String seatTypeName = normalizeSeatTypeName(range);
                String visualColor = normalizeVisualColor(range);
                String existingColor = colorBySeatTypeName.putIfAbsent(seatTypeKey(seatTypeName), visualColor);
                if (existingColor != null && !existingColor.equals(visualColor)) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "One seat type name must use one color");
                }
                normalizeSeatStatus(range.status());

                for (int seatNumber = range.startSeat(); seatNumber <= range.endSeat(); seatNumber++) {
                    if (occupied[seatNumber]) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Seat ranges in one row cannot overlap");
                    }
                    occupied[seatNumber] = true;
                }
            }
        }
    }

    private boolean hasInternalSeatRows(CreateEventRequest request) {
        return request.internalSeatRows() != null && !request.internalSeatRows().isEmpty();
    }

    private SeatAssignment assignmentForSeat(InternalSeatRowRequest row, int seatNumber) {
        List<InternalSeatRangeRequest> ranges = row.ranges() == null ? List.of() : row.ranges();
        for (InternalSeatRangeRequest range : ranges) {
            if (range.startSeat() <= seatNumber && seatNumber <= range.endSeat()) {
                return new SeatAssignment(
                        normalizeSeatType(range.seatTypeCode()),
                        normalizeSeatTypeName(range),
                        normalizeVisualColor(range),
                        range.price(),
                        normalizeSeatStatus(range.status()),
                        Boolean.TRUE.equals(range.accessible())
                );
            }
        }
        return new SeatAssignment(
                "STANDARD",
                "Standard",
                "#CBD5E1",
                defaultInternalSeatPrice(ranges),
                SeatStatus.AVAILABLE,
                false
        );
    }

    private BigDecimal defaultInternalSeatPrice(List<InternalSeatRangeRequest> ranges) {
        return ranges.stream()
                .filter(range -> "STANDARD".equals(normalizeSeatType(range.seatTypeCode())))
                .map(InternalSeatRangeRequest::price)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private String normalizeSeatType(String value) {
        String normalized = value == null || value.isBlank()
                ? "STANDARD"
                : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "STANDARD", "VIP", "COUPLE", "SWEETBOX", "WHEELCHAIR" -> normalized;
            case "ACCESSIBLE" -> "WHEELCHAIR";
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "Seat type is invalid");
        };
    }

    private String seatTypeLabel(String seatTypeCode) {
        return switch (normalizeSeatType(seatTypeCode)) {
            case "VIP" -> "VIP";
            case "COUPLE" -> "Couple";
            case "SWEETBOX" -> "Sweetbox";
            case "WHEELCHAIR" -> "Accessible";
            default -> "Standard";
        };
    }

    private String normalizeSeatTypeName(InternalSeatRangeRequest range) {
        String value = range.seatTypeName();
        if (value == null || value.isBlank()) {
            return seatTypeLabel(range.seatTypeCode());
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 60) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Seat type name is too long");
        }
        return normalized;
    }

    private String normalizeVisualColor(InternalSeatRangeRequest range) {
        String color = range.visualColorHex();
        if (color == null || color.isBlank()) {
            return defaultSeatTypeColor(range.seatTypeCode());
        }
        String normalized = color.trim().toUpperCase(Locale.ROOT);
        if (!HEX_COLOR.matcher(normalized).matches()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Seat type color must be a valid hex color");
        }
        return normalized;
    }

    private String defaultSeatTypeColor(String seatTypeCode) {
        return switch (normalizeSeatType(seatTypeCode)) {
            case "VIP" -> "#F97316";
            case "COUPLE", "SWEETBOX" -> "#E11D48";
            case "WHEELCHAIR" -> "#16A34A";
            default -> "#CBD5E1";
        };
    }

    private String seatTypeKey(String seatTypeName) {
        return seatTypeName.trim().toUpperCase(Locale.ROOT);
    }

    private SeatStatus normalizeSeatStatus(String status) {
        if (status == null || status.isBlank()) {
            return SeatStatus.AVAILABLE;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "AVAILABLE" -> SeatStatus.AVAILABLE;
            case "SOLD", "BOOKED" -> SeatStatus.SOLD;
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "Seat status is invalid");
        };
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SeatsioKeys(
            String chartKey,
            String workspaceKey,
            String eventKey
    ) {
    }

    private record SaleWindow(
            Instant saleStartTime,
            Instant saleEndTime
    ) {
    }

    private record SeatAssignment(
            String seatTypeCode,
            String seatTypeName,
            String visualColorHex,
            BigDecimal price,
            SeatStatus status,
            boolean accessible
    ) {
    }

    private record SeatTypeSummary(
            String name,
            String seatTypeCode,
            String visualColorHex,
            BigDecimal minimumPrice,
            int seatCount
    ) {
        private SeatTypeSummary increment(BigDecimal price) {
            return new SeatTypeSummary(
                    name,
                    seatTypeCode,
                    visualColorHex,
                    minimumPrice.min(price),
                    seatCount + 1
            );
        }
    }
}

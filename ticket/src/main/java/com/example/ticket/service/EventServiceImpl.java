package com.example.ticket.service;

import com.example.ticket.dto.request.CreateEventRequest;
import com.example.ticket.dto.request.EventSectionRequest;
import com.example.ticket.dto.response.EventResponse;
import com.example.ticket.exception.AppException;
import com.example.ticket.model.entity.Event;
import com.example.ticket.model.entity.EventSeat;
import com.example.ticket.model.entity.EventSection;
import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.EventStatus;
import com.example.ticket.model.enums.SeatStatus;
import com.example.ticket.model.enums.UserRole;
import com.example.ticket.repository.CategoryRepository;
import com.example.ticket.repository.EventRepository;
import com.example.ticket.repository.EventSeatRepository;
import com.example.ticket.repository.EventSectionRepository;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.security.JwtPrincipal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
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

    private final EventRepository eventRepository;
    private final EventSectionRepository sectionRepository;
    private final EventSeatRepository seatRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public EventServiceImpl(
            EventRepository eventRepository,
            EventSectionRepository sectionRepository,
            EventSeatRepository seatRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.eventRepository = eventRepository;
        this.sectionRepository = sectionRepository;
        this.seatRepository = seatRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    public EventResponse createEvent(CreateEventRequest request) {
        User user = currentUser();
        if (user.getRole() != UserRole.ORGANIZER && user.getRole() != UserRole.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "You need a verified organization before creating events");
        }
        if (user.getPrimaryOrganizationId() == null && user.getRole() != UserRole.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "You need a verified organization before creating events");
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
                .organizerId(user.getId())
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
                .organizerTermsAcceptedAt(Instant.now())
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
        return eventRepository.findByOrganizerIdOrderByCreatedAtDesc(userId)
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

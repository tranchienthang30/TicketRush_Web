package com.example.ticket.controller;

import com.example.ticket.dto.BookingEventResponse;
import com.example.ticket.dto.CategoryEventsResponse;
import com.example.ticket.dto.EventPageResponse;
import com.example.ticket.dto.request.CreateEventRequest;
import com.example.ticket.service.CurrentUserService;
import com.example.ticket.dto.response.EventResponse;
import com.example.ticket.dto.response.ProviderBookingSummaryResponse;
import com.example.ticket.service.EventService;
import com.example.ticket.service.VirtualQueueService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventService eventService;
    private final CurrentUserService currentUserService;
    private final VirtualQueueService virtualQueueService;

    public EventController(
            EventService eventService,
            CurrentUserService currentUserService,
            VirtualQueueService virtualQueueService
    ) {
        this.eventService = eventService;
        this.currentUserService = currentUserService;
        this.virtualQueueService = virtualQueueService;
    }

    @GetMapping("/events")
    public EventPageResponse getEvents(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return eventService.searchEvents(categoryId, query, city, page, size);
    }

    @GetMapping("/events/grouped")
    public List<CategoryEventsResponse> getGroupedEvents(
            @RequestParam(defaultValue = "4") int limitPerCategory
    ) {
        return eventService.getGroupedEvents(limitPerCategory);
    }

    @GetMapping("/events/{eventId}/booking")
    public BookingEventResponse getBookingEvent(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID eventId
    ) {
        UUID viewerUserId = currentUserService.resolve(userId);
        virtualQueueService.requireAccess(eventId, viewerUserId);
        return eventService.getBookingEvent(eventId, viewerUserId);
    }

    @GetMapping("/events/legacy")
    List<EventResponse> publicEvents() {
        return eventService.publicEvents();
    }

    @GetMapping("/events/slug/{slug}")
    EventResponse bySlug(@PathVariable String slug) {
        return eventService.eventBySlug(slug);
    }

    @GetMapping("/events/my-events")
    List<EventResponse> myEvents() {
        return eventService.myEvents();
    }

    @GetMapping("/events/my-events/{eventId}")
    EventResponse myEvent(@PathVariable UUID eventId) {
        return eventService.myEvent(eventId);
    }

    @GetMapping("/events/my-events/{eventId}/booking-summary")
    ProviderBookingSummaryResponse myEventBookingSummary(@PathVariable UUID eventId) {
        return eventService.providerBookingSummary(eventId);
    }

    @PostMapping("/events")
    ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @PutMapping("/events/{eventId}")
    EventResponse update(@PathVariable UUID eventId, @Valid @RequestBody CreateEventRequest request) {
        return eventService.updateEvent(eventId, request);
    }

}

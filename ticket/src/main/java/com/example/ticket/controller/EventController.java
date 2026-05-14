package com.example.ticket.controller;

import com.example.ticket.dto.request.CreateEventRequest;
import com.example.ticket.dto.response.EventResponse;
import com.example.ticket.service.EventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    List<EventResponse> publicEvents() {
        return eventService.publicEvents();
    }

    @GetMapping("/slug/{slug}")
    EventResponse bySlug(@PathVariable String slug) {
        return eventService.eventBySlug(slug);
    }

    @GetMapping("/my-events")
    List<EventResponse> myEvents() {
        return eventService.myEvents();
    }

    @PostMapping
    ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }
}

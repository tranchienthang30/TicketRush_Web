package com.example.ticket.controller;

import com.example.ticket.dto.CategoryEventsResponse;
import com.example.ticket.dto.CategoryResponse;
import com.example.ticket.dto.EventPageResponse;
import com.example.ticket.service.EventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> getCategories() {
        return eventService.getCategories();
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
}

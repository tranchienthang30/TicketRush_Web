package com.example.ticket.service;

import com.example.ticket.dto.HomeResponse;
import org.springframework.stereotype.Service;

@Service
public class HomeService {
    private final EventService eventService;

    public HomeService(EventService eventService) {
        this.eventService = eventService;
    }

    public HomeResponse getHome() {
        return new HomeResponse(eventService.getGroupedEvents(4));
    }
}

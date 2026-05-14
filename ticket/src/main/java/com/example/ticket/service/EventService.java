package com.example.ticket.service;

import com.example.ticket.dto.request.CreateEventRequest;
import com.example.ticket.dto.response.EventResponse;
import java.util.List;
import java.util.UUID;

public interface EventService {
    EventResponse createEvent(CreateEventRequest request);

    List<EventResponse> publicEvents();

    List<EventResponse> myEvents();

    EventResponse eventBySlug(String slug);
}

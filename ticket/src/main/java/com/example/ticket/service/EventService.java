package com.example.ticket.service;

import com.example.ticket.dto.BookingEventResponse;
import com.example.ticket.dto.CategoryEventsResponse;
import com.example.ticket.dto.CategoryResponse;
import com.example.ticket.dto.EventPageResponse;
import com.example.ticket.dto.request.CreateEventRequest;
import com.example.ticket.dto.response.EventResponse;
import java.util.List;
import java.util.UUID;

public interface EventService {
    EventResponse createEvent(CreateEventRequest request);

    List<EventResponse> publicEvents();

    List<EventResponse> myEvents();

    EventResponse eventBySlug(String slug);

    List<CategoryResponse> getCategories();

    List<CategoryEventsResponse> getGroupedEvents(int limitPerCategory);

    EventPageResponse searchEvents(Long categoryId, String query, String city, int page, int size);

    BookingEventResponse getBookingEvent(UUID eventId, UUID viewerUserId);
}

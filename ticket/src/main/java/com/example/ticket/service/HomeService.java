package com.example.ticket.service;

import com.example.ticket.config.CacheNames;
import com.example.ticket.dto.HomeResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class HomeService {
    private final EventService eventService;

    public HomeService(EventService eventService) {
        this.eventService = eventService;
    }

    @Cacheable(cacheNames = CacheNames.USER_HOME, condition = "@userCachePolicy.allowCache()")
    public HomeResponse getHome() {
        return new HomeResponse(eventService.getGroupedEvents(4));
    }
}


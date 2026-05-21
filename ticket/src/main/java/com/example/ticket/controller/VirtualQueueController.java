package com.example.ticket.controller;

import com.example.ticket.dto.ApiMessageResponse;
import com.example.ticket.dto.VirtualQueueStatusResponse;
import com.example.ticket.service.CurrentUserService;
import com.example.ticket.service.VirtualQueueService;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/virtual-queue")
public class VirtualQueueController {
    private final VirtualQueueService virtualQueueService;
    private final CurrentUserService currentUserService;

    public VirtualQueueController(
            VirtualQueueService virtualQueueService,
            CurrentUserService currentUserService
    ) {
        this.virtualQueueService = virtualQueueService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/events/{eventId}/join")
    public VirtualQueueStatusResponse join(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID eventId
    ) {
        return virtualQueueService.join(eventId, currentUserService.resolve(userId));
    }

    @GetMapping("/events/{eventId}/status")
    public VirtualQueueStatusResponse status(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID eventId
    ) {
        return virtualQueueService.status(eventId, currentUserService.resolve(userId));
    }

    @DeleteMapping("/events/{eventId}")
    public ApiMessageResponse leave(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID eventId
    ) {
        virtualQueueService.leave(eventId, currentUserService.resolve(userId));
        return new ApiMessageResponse("Left virtual queue");
    }
}

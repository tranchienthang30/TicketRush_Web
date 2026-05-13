package com.example.ticket.controller;

import com.example.ticket.dto.OrderSummaryResponse;
import com.example.ticket.dto.ProfileDashboardResponse;
import com.example.ticket.dto.ProfileResponse;
import com.example.ticket.dto.TicketSummaryResponse;
import com.example.ticket.dto.UpdateProfileRequest;
import com.example.ticket.service.CurrentUserService;
import com.example.ticket.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class ProfileController {
    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    public ProfileController(ProfileService profileService, CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ProfileResponse getProfile(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId
    ) {
        return profileService.getProfile(currentUserService.resolve(userId));
    }

    @PutMapping
    public ProfileResponse updateProfile(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @RequestBody UpdateProfileRequest request
    ) {
        return profileService.updateProfile(currentUserService.resolve(userId), request);
    }

    @GetMapping("/dashboard")
    public ProfileDashboardResponse getDashboard(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId
    ) {
        return profileService.getDashboard(currentUserService.resolve(userId));
    }

    @GetMapping("/tickets")
    public List<TicketSummaryResponse> getTickets(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @RequestParam(defaultValue = "upcoming") String status,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return profileService.getTickets(currentUserService.resolve(userId), status, limit);
    }

    @GetMapping("/orders")
    public List<OrderSummaryResponse> getOrders(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId
    ) {
        return profileService.getOrders(currentUserService.resolve(userId));
    }
}

package com.example.ticket.controller;

import com.example.ticket.dto.request.SeatsioChartCreateRequest;
import com.example.ticket.dto.request.SeatsioEventCreateRequest;
import com.example.ticket.dto.response.SeatsioChartResponse;
import com.example.ticket.dto.response.SeatsioEventResponse;
import com.example.ticket.dto.response.SeatsioWorkspaceResponse;
import com.example.ticket.service.CurrentUserService;
import com.example.ticket.service.SeatsioProviderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider/seatsio")
public class SeatsioProviderController {
    private final SeatsioProviderService seatsioProviderService;
    private final CurrentUserService currentUserService;

    public SeatsioProviderController(
            SeatsioProviderService seatsioProviderService,
            CurrentUserService currentUserService
    ) {
        this.seatsioProviderService = seatsioProviderService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/workspace")
    public SeatsioWorkspaceResponse getWorkspace() {
        return seatsioProviderService.getWorkspace(currentUserService.resolve(null));
    }

    @PostMapping("/workspace")
    public SeatsioWorkspaceResponse ensureWorkspace() {
        return seatsioProviderService.ensureWorkspace(currentUserService.resolve(null));
    }

    @PostMapping("/charts")
    public SeatsioChartResponse createChart(@Valid @RequestBody SeatsioChartCreateRequest request) {
        return seatsioProviderService.createChart(currentUserService.resolve(null), request);
    }

    @PostMapping("/events")
    public SeatsioEventResponse createEvent(@Valid @RequestBody SeatsioEventCreateRequest request) {
        return seatsioProviderService.createEvent(currentUserService.resolve(null), request);
    }
}

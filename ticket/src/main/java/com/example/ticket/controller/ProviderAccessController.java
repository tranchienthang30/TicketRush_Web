package com.example.ticket.controller;

import com.example.ticket.dto.request.RejectProviderRequest;
import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.UserResponse;
import com.example.ticket.service.ProviderAccessService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProviderAccessController {
    private final ProviderAccessService providerAccessService;

    public ProviderAccessController(ProviderAccessService providerAccessService) {
        this.providerAccessService = providerAccessService;
    }

    @PostMapping("/api/providers/request")
    public ApiResponse<UserResponse> requestProviderAccess() {
        return ApiResponse.ok(
                "Provider request has been submitted. Please verify your email and wait for admin approval.",
                providerAccessService.requestProviderAccess()
        );
    }

    @GetMapping("/api/admin/provider-requests")
    public List<UserResponse> pendingProviderRequests() {
        return providerAccessService.pendingRequests();
    }

    @PostMapping("/api/admin/provider-requests/{userId}/approve")
    public ApiResponse<UserResponse> approve(@PathVariable UUID userId) {
        return ApiResponse.ok("Provider request has been approved.", providerAccessService.approve(userId));
    }

    @PostMapping("/api/admin/provider-requests/{userId}/reject")
    public ApiResponse<UserResponse> reject(
            @PathVariable UUID userId,
            @Valid @RequestBody(required = false) RejectProviderRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok("Provider request has been rejected.", providerAccessService.reject(userId, reason));
    }
}

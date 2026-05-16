package com.example.ticket.controller;

import com.example.ticket.dto.request.RegisterOrganizationRequest;
import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.OrganizationResponse;
import com.example.ticket.exception.AppException;
import com.example.ticket.service.OrganizationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/providers", "/api/organizations"})
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/register")
    ApiResponse<Void> register(@Valid @RequestBody RegisterOrganizationRequest request) {
        throw new AppException(HttpStatus.GONE, "Organization registration has been replaced by admin-reviewed provider requests.");
    }

    @PostMapping("/verify")
    ApiResponse<OrganizationResponse> verify(@RequestParam String token) {
        throw new AppException(HttpStatus.GONE, "Organization verification has been replaced by admin-reviewed provider requests.");
    }

    @GetMapping("/me")
    List<OrganizationResponse> myOrganizations() {
        return organizationService.myOrganizations();
    }
}

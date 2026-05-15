package com.example.ticket.service;

import com.example.ticket.dto.request.RegisterOrganizationRequest;
import com.example.ticket.dto.response.OrganizationResponse;
import java.util.List;

public interface OrganizationService {
    void requestOrganizationRegistration(RegisterOrganizationRequest request);

    OrganizationResponse verifyOrganization(String token);

    List<OrganizationResponse> myOrganizations();
}

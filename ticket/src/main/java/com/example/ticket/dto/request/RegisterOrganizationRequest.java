package com.example.ticket.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterOrganizationRequest(
        @NotBlank(message = "Provider name is required")
        @Size(max = 255, message = "Provider name must be at most 255 characters")
        String name,

        @Email(message = "Business email is invalid")
        @NotBlank(message = "Business email is required")
        String businessEmail
) {
}

package com.example.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Email(message = "Email is invalid")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must be at most 255 characters")
        String fullName,

        String avatarUrl,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        @Pattern(regexp = "^(\\+?\\d{8,15})?$", message = "Phone must contain only digits and may start with +")
        String phone,

        String gender,

        LocalDate dateOfBirth
) {
}

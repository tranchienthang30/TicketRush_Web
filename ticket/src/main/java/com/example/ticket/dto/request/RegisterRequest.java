package com.example.ticket.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Email is invalid")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must include at least 1 letter and 1 number")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must be at most 255 characters")
        String fullName,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        @Pattern(regexp = "^(\\+?\\d{8,15})?$", message = "Phone must contain only digits and may start with +")
        String phone,

        String recaptchaToken,

        Boolean requestProviderAccess
) {
}

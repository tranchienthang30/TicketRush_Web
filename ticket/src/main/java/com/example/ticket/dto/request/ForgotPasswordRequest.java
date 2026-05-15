package com.example.ticket.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @Email(message = "Email is invalid")
        @NotBlank(message = "Email is required")
        String email,

        String recaptchaToken
) {
}

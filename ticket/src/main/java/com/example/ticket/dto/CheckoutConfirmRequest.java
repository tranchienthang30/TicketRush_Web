package com.example.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CheckoutConfirmRequest(
        @NotNull UUID eventId,
        @NotEmpty List<UUID> seatIds,
        String voucherCode,
        @NotBlank String paymentMethod,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String phone
) {
}

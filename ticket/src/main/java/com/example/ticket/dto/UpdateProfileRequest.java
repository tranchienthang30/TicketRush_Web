package com.example.ticket.dto;

import java.time.LocalDate;

public record UpdateProfileRequest(
        String fullName,
        String avatarUrl,
        String phone,
        String gender,
        LocalDate dateOfBirth
) {
}

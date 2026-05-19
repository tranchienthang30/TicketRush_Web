package com.example.ticket.dto.request;

import com.example.ticket.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        UserRole role
) {
}

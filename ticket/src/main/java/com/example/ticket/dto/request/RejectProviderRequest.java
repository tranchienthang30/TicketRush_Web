package com.example.ticket.dto.request;

import jakarta.validation.constraints.Size;

public record RejectProviderRequest(
        @Size(max = 1000, message = "Reason must be at most 1000 characters")
        String reason
) {
}

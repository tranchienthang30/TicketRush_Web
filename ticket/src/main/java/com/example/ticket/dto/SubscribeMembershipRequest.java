package com.example.ticket.dto;

import jakarta.validation.constraints.NotNull;

public record SubscribeMembershipRequest(
        @NotNull Long planId
) {
}

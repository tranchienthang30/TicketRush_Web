package com.example.ticket.dto;

import java.util.List;

public record HomeResponse(
        List<CategoryEventsResponse> featuredCategories
) {
}

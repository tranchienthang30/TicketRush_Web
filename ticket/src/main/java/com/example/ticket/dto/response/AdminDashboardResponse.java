package com.example.ticket.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalEvents,
        BigDecimal platformRevenue,
        BigDecimal platformFeeRate,
        long totalTicketsSold,
        BigDecimal grossTicketRevenue,
        List<RoleCountResponse> usersByRole,
        List<StatusCountResponse> eventsByStatus
) {
    public record RoleCountResponse(String role, long count) {
    }

    public record StatusCountResponse(String status, long count) {
    }
}

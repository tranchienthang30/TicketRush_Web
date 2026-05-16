package com.example.ticket.dto;

import java.util.List;

public record ProfileDashboardResponse(
        ProfileResponse profile,
        UserMembershipResponse membership,
        List<StatResponse> stats,
        List<TicketSummaryResponse> upcomingTickets,
        List<ActivityResponse> recentActivity
) {
}

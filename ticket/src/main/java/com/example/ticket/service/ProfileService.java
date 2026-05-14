package com.example.ticket.service;

import com.example.ticket.dto.ActivityResponse;
import com.example.ticket.dto.OrderSummaryResponse;
import com.example.ticket.dto.ProfileDashboardResponse;
import com.example.ticket.dto.ProfileResponse;
import com.example.ticket.dto.StatResponse;
import com.example.ticket.dto.TicketDetailItemResponse;
import com.example.ticket.dto.TicketDetailResponse;
import com.example.ticket.dto.TicketSummaryResponse;
import com.example.ticket.dto.UpdateProfileRequest;
import com.example.ticket.exception.ApiException;
import com.example.ticket.repository.ProfileQueryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProfileService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.ENGLISH);
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private final ProfileQueryRepository profileRepository;
    private final MembershipService membershipService;

    public ProfileService(ProfileQueryRepository profileRepository, MembershipService membershipService) {
        this.profileRepository = profileRepository;
        this.membershipService = membershipService;
    }

    public ProfileResponse getProfile(UUID userId) {
        return profileRepository.findProfile(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User profile not found"));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        getProfile(userId);
        profileRepository.updateProfile(userId, request);
        return getProfile(userId);
    }

    public ProfileDashboardResponse getDashboard(UUID userId) {
        return new ProfileDashboardResponse(
                getProfile(userId),
                membershipService.getCurrentMembership(userId),
                getStats(userId),
                getTickets(userId, "upcoming", 5),
                getRecentActivity(userId)
        );
    }

    public List<TicketSummaryResponse> getTickets(UUID userId, String status, int limit) {
        int safeLimit = clamp(limit, 1, 50);
        return profileRepository.findTickets(userId, status, safeLimit).stream()
                .map(row -> new TicketSummaryResponse(
                        row.orderId(),
                        row.eventId(),
                        row.eventSlug(),
                        row.title(),
                        formatDate(row.startTime()),
                        row.startTime(),
                        row.location(),
                        row.seat(),
                        row.status(),
                        row.image()
                ))
                .toList();
    }

    public List<OrderSummaryResponse> getOrders(UUID userId) {
        return profileRepository.findOrders(userId).stream()
                .map(row -> new OrderSummaryResponse(
                        row.id(),
                        row.eventId(),
                        row.eventTitle(),
                        row.status(),
                        row.ticketCount(),
                        row.subtotal(),
                        row.discountAmount(),
                        row.totalAmount(),
                        formatMoney(row.totalAmount()),
                        row.createdAt(),
                        row.paidAt()
                ))
                .toList();
    }

    public TicketDetailResponse getTicketDetail(UUID userId, UUID orderId) {
        ProfileQueryRepository.TicketDetailHeaderRow header = profileRepository.findTicketDetailHeader(userId, orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket order not found"));

        List<TicketDetailItemResponse> items = profileRepository.findTicketDetailItems(userId, orderId).stream()
                .map(item -> new TicketDetailItemResponse(
                        item.orderItemId(),
                        item.seatId(),
                        item.seatCode(),
                        item.ticketStatus(),
                        item.priceSnapshot(),
                        item.qrCode(),
                        buildQrContent(header, item),
                        item.issuedAt(),
                        item.checkedInAt()
                ))
                .toList();

        return new TicketDetailResponse(
                header.orderId(),
                header.eventId(),
                header.eventSlug(),
                header.title(),
                header.location(),
                formatDateTime(header.startTime()),
                header.startTime(),
                header.orderStatus(),
                header.totalAmount(),
                formatMoney(header.totalAmount()),
                header.createdAt(),
                header.paidAt(),
                items
        );
    }

    private List<StatResponse> getStats(UUID userId) {
        ProfileQueryRepository.ProfileStatsRow stats = profileRepository.findStats(userId);
        return List.of(
                new StatResponse("Tickets booked", String.valueOf(stats.ticketsBooked())),
                new StatResponse("Upcoming movies", String.valueOf(stats.upcomingEvents())),
                new StatResponse("Savings", formatMoney(stats.savings())),
                new StatResponse("Total spend", formatMoney(stats.totalSpent()))
        );
    }

    private List<ActivityResponse> getRecentActivity(UUID userId) {
        return profileRepository.findRecentActivities(userId, 6).stream()
                .map(row -> new ActivityResponse(row.title(), formatRelativeTime(row.occurredAt()), row.occurredAt()))
                .toList();
    }

    private String formatDate(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.atZoneSameInstant(APP_ZONE).format(DATE_FORMATTER);
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.atZoneSameInstant(APP_ZONE).format(DATE_TIME_FORMATTER);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return "0 VND";
        }
        return NumberFormat.getNumberInstance(VIETNAM).format(value) + " VND";
    }

    private String formatRelativeTime(OffsetDateTime occurredAt) {
        if (occurredAt == null) {
            return "";
        }

        Duration duration = Duration.between(occurredAt, OffsetDateTime.now(APP_ZONE));
        long days = duration.toDays();
        long hours = duration.toHours();
        long minutes = duration.toMinutes();

        if (days > 0) {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        }
        if (hours > 0) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        }
        if (minutes > 0) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        }
        return "Just now";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String buildQrContent(
            ProfileQueryRepository.TicketDetailHeaderRow header,
            ProfileQueryRepository.TicketDetailItemRow item
    ) {
        List<String> parts = new ArrayList<>();
        parts.add("TR-TICKET");
        parts.add(header.orderId().toString());
        parts.add(header.eventId().toString());
        parts.add(item.seatCode());
        parts.add(item.ticketStatus());

        if (item.qrCode() != null && !item.qrCode().isBlank()) {
            parts.add(item.qrCode());
        }

        return String.join("|", parts);
    }
}

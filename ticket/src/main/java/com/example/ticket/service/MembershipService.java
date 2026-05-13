package com.example.ticket.service;

import com.example.ticket.dto.MembershipPlanResponse;
import com.example.ticket.dto.UserMembershipResponse;
import com.example.ticket.exception.ApiException;
import com.example.ticket.repository.MembershipQueryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MembershipService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private final MembershipQueryRepository membershipRepository;

    public MembershipService(MembershipQueryRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public List<MembershipPlanResponse> getPlans() {
        return membershipRepository.findActivePlans().stream()
                .map(this::toPlanResponse)
                .toList();
    }

    public UserMembershipResponse getCurrentMembership(UUID userId) {
        return membershipRepository.findCurrentMembership(userId)
                .map(this::toMembershipResponse)
                .orElse(null);
    }

    @Transactional
    public UserMembershipResponse subscribe(UUID userId, long planId) {
        MembershipQueryRepository.MembershipPlanRow plan = membershipRepository.findActivePlanById(planId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership plan not found"));

        OffsetDateTime startAt = OffsetDateTime.now(APP_ZONE);
        OffsetDateTime endAt = startAt.plusDays(plan.durationDays());

        membershipRepository.cancelActiveMemberships(userId);
        membershipRepository.insertMembership(userId, plan, startAt, endAt);

        return getCurrentMembership(userId);
    }

    @Transactional
    public void cancel(UUID userId) {
        membershipRepository.cancelActiveMemberships(userId);
    }

    private MembershipPlanResponse toPlanResponse(MembershipQueryRepository.MembershipPlanRow row) {
        return new MembershipPlanResponse(
                row.id(),
                row.name(),
                row.description(),
                row.price(),
                formatMoney(row.price()),
                row.durationDays(),
                row.discountPercent(),
                isFeatured(row),
                perksFor(row)
        );
    }

    private UserMembershipResponse toMembershipResponse(MembershipQueryRepository.UserMembershipRow row) {
        return new UserMembershipResponse(
                row.id(),
                row.planId(),
                row.planName(),
                row.description(),
                row.discountPercent(),
                row.status(),
                row.startAt(),
                row.endAt(),
                row.active()
        );
    }

    private boolean isFeatured(MembershipQueryRepository.MembershipPlanRow row) {
        return row.name().equalsIgnoreCase("Gold")
                || row.discountPercent().compareTo(BigDecimal.TEN) >= 0;
    }

    private List<String> perksFor(MembershipQueryRepository.MembershipPlanRow row) {
        String discount = row.discountPercent().stripTrailingZeros().toPlainString() + "% discount on eligible orders";
        return switch (row.name().toLowerCase(Locale.ROOT)) {
            case "silver" -> List.of(discount, "Member voucher access", "Faster support queue");
            case "gold" -> List.of(discount, "Early access to selected events", "Priority booking support");
            case "premium" -> List.of(discount, "Best member voucher access", "VIP support for group bookings");
            default -> List.of(discount, row.durationDays() + " days of active membership");
        };
    }

    private String formatMoney(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return "Free";
        }
        return NumberFormat.getNumberInstance(VIETNAM).format(value) + " VND";
    }
}

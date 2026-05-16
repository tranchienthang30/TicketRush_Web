package com.example.ticket.controller;

import com.example.ticket.dto.ApiMessageResponse;
import com.example.ticket.dto.MembershipPlanResponse;
import com.example.ticket.dto.SubscribeMembershipRequest;
import com.example.ticket.dto.UserMembershipResponse;
import com.example.ticket.service.CurrentUserService;
import com.example.ticket.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MembershipController {
    private final MembershipService membershipService;
    private final CurrentUserService currentUserService;

    public MembershipController(MembershipService membershipService, CurrentUserService currentUserService) {
        this.membershipService = membershipService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/membership/plans")
    public List<MembershipPlanResponse> getPlans() {
        return membershipService.getPlans();
    }

    @GetMapping("/me/membership")
    public UserMembershipResponse getCurrentMembership(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId
    ) {
        return membershipService.getCurrentMembership(currentUserService.resolve(userId));
    }

    @PostMapping("/me/membership/subscribe")
    public UserMembershipResponse subscribe(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody SubscribeMembershipRequest request
    ) {
        return membershipService.subscribe(currentUserService.resolve(userId), request.planId());
    }

    @PatchMapping("/me/membership/cancel")
    public ApiMessageResponse cancel(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId
    ) {
        membershipService.cancel(currentUserService.resolve(userId));
        return new ApiMessageResponse("Membership cancelled");
    }
}

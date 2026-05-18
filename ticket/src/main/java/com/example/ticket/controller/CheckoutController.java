package com.example.ticket.controller;

import com.example.ticket.dto.CheckoutConfirmRequest;
import com.example.ticket.dto.CheckoutPreviewRequest;
import com.example.ticket.dto.CheckoutResultResponse;
import com.example.ticket.dto.CheckoutSummaryResponse;
import com.example.ticket.dto.ApiMessageResponse;
import com.example.ticket.dto.SeatLockRequest;
import com.example.ticket.dto.SeatLockResponse;
import com.example.ticket.service.CheckoutService;
import com.example.ticket.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;
    private final CurrentUserService currentUserService;

    public CheckoutController(CheckoutService checkoutService, CurrentUserService currentUserService) {
        this.checkoutService = checkoutService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/preview")
    public CheckoutSummaryResponse preview(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody CheckoutPreviewRequest request
    ) {
        return checkoutService.preview(currentUserService.resolve(userId), request);
    }

    @PostMapping("/seats/lock")
    public SeatLockResponse lockSeats(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody SeatLockRequest request
    ) {
        return checkoutService.lockSeats(currentUserService.resolve(userId), request);
    }

    @PostMapping("/seats/release")
    public SeatLockResponse releaseSeats(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody SeatLockRequest request
    ) {
        return checkoutService.releaseSeats(currentUserService.resolve(userId), request);
    }

    @PostMapping("/confirm")
    public CheckoutResultResponse confirm(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody CheckoutConfirmRequest request
    ) {
        return checkoutService.confirm(currentUserService.resolve(userId), request);
    }

    @PostMapping("/payos/complete")
    public ApiMessageResponse completePayOSPayment(
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @RequestParam long orderCode
    ) {
        checkoutService.completePayOSPayment(currentUserService.resolve(userId), orderCode);
        return new ApiMessageResponse("Payment completed successfully");
    }
}

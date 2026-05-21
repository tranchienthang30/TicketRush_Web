package com.example.ticket.service;

import com.example.ticket.dto.CheckoutConfirmRequest;
import com.example.ticket.dto.CheckoutPreviewRequest;
import com.example.ticket.dto.CheckoutResultResponse;
import com.example.ticket.dto.CheckoutSummaryResponse;
import com.example.ticket.dto.CheckoutTicketResponse;
import com.example.ticket.dto.SeatLockRequest;
import com.example.ticket.dto.SeatLockResponse;
import com.example.ticket.exception.ApiException;
import com.example.ticket.repository.CheckoutQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.type.PaymentLinkData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CheckoutService {
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal SERVICE_FEE = new BigDecimal("15000.00");
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");
    private static final String PAYMENT_METHOD_BANK_QR = "card";

    private final CheckoutQueryRepository checkoutRepository;
    private final PayOSPaymentService payOSPaymentService;
    private final PaymentConfirmationEmailService paymentConfirmationEmailService;
    private final VirtualQueueService virtualQueueService;
    private final int bookingLockMinutes;

    public CheckoutService(
            CheckoutQueryRepository checkoutRepository,
            PayOSPaymentService payOSPaymentService,
            PaymentConfirmationEmailService paymentConfirmationEmailService,
            VirtualQueueService virtualQueueService,
            @Value("${app.booking.lock-minutes:10}") int bookingLockMinutes
    ) {
        this.checkoutRepository = checkoutRepository;
        this.payOSPaymentService = payOSPaymentService;
        this.paymentConfirmationEmailService = paymentConfirmationEmailService;
        this.virtualQueueService = virtualQueueService;
        this.bookingLockMinutes = bookingLockMinutes;
    }

    public CheckoutSummaryResponse preview(UUID userId, CheckoutPreviewRequest request) {
        virtualQueueService.requireAccess(request.eventId(), userId);
        CheckoutEvaluation evaluation = evaluate(
                userId,
                request.eventId(),
                request.seatIds(),
                request.voucherCode(),
                SeatValidationMode.PREVIEW,
                false
        );
        return toSummaryResponse(evaluation);
    }

    @Transactional
    public SeatLockResponse lockSeats(UUID userId, SeatLockRequest request) {
        virtualQueueService.requireAccess(request.eventId(), userId);
        List<UUID> seatIds = normalizeSeatIds(request.seatIds());
        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
        OffsetDateTime lockExpiresAt = now.plusMinutes(bookingLockMinutes);

        CheckoutQueryRepository.EventCheckoutRow event = checkoutRepository.findBookableEvent(request.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event is not available for booking"));

        List<CheckoutQueryRepository.SeatCheckoutRow> seats = checkoutRepository.findSeatsByIdsForUpdate(seatIds);
        validateSeatOwnershipAndAvailability(userId, event.id(), seats, seatIds, now, SeatValidationMode.LOCK);
        int lockedCount = checkoutRepository.setSeatLocks(userId, event.id(), seatIds, lockExpiresAt);
        if (lockedCount != seatIds.size()) {
            throw new ApiException(HttpStatus.CONFLICT, "One or more seats cannot be locked right now");
        }

        return new SeatLockResponse(event.id(), seatIds, lockExpiresAt);
    }

    @Transactional
    public SeatLockResponse releaseSeats(UUID userId, SeatLockRequest request) {
        List<UUID> seatIds = normalizeSeatIds(request.seatIds());
        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);

        CheckoutQueryRepository.EventCheckoutRow event = checkoutRepository.findPublishedEvent(request.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        List<CheckoutQueryRepository.SeatCheckoutRow> seats = checkoutRepository.findSeatsByIdsForUpdate(seatIds);
        validateSeatOwnershipAndAvailability(userId, event.id(), seats, seatIds, now, SeatValidationMode.RELEASE);
        checkoutRepository.releaseSeatLocksForUser(userId, event.id(), seatIds);

        return new SeatLockResponse(event.id(), seatIds, null);
    }

    @Transactional
    public void completePayOSPayment(UUID userId, long orderCode) {
        CheckoutQueryRepository.OrderStatusRow order = checkoutRepository
                .findOrderStatusByPayOSOrderCodeAndUserId(orderCode, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found for this payment code"));

        if (isSuccessfulOrder(order.status())) {
            return;
        }

        PaymentLinkData paymentLink = payOSPaymentService.getPaymentLinkInformation(orderCode);
        String payOSStatus = paymentLink.getStatus();
        if (!isSuccessfulOrder(payOSStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment is not successful yet");
        }

        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
        int updated = checkoutRepository.markOrderPaidIfPending(order.orderId(), now);
        if (updated == 0) {
            CheckoutQueryRepository.OrderStatusRow latest = checkoutRepository.findOrderStatus(order.orderId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
            if (!isSuccessfulOrder(latest.status())) {
                throw new ApiException(HttpStatus.CONFLICT, "Order is not in pending state");
            }
            return;
        }

        checkoutRepository.markOrderSeatsSold(order.orderId());
        checkoutRepository.issueTicketsForOrder(order.orderId(), now);
        sendOrderSuccessEmail(order.orderId());
    }

    @Transactional
    public void cancelPayOSPayment(UUID userId, long orderCode) {
        CheckoutQueryRepository.OrderStatusRow order = checkoutRepository
                .findOrderStatusByPayOSOrderCodeAndUserId(orderCode, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found for this payment code"));

        if (isSuccessfulOrder(order.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "Order is already paid");
        }

        if ("CANCELLED".equalsIgnoreCase(order.status()) || "EXPIRED".equalsIgnoreCase(order.status())) {
            checkoutRepository.releaseSeatLocksForOrder(order.orderId());
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
        int updated = checkoutRepository.markOrderCancelledIfPending(order.orderId(), now);
        if (updated == 0) {
            CheckoutQueryRepository.OrderStatusRow latest = checkoutRepository.findOrderStatus(order.orderId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
            if (isSuccessfulOrder(latest.status())) {
                throw new ApiException(HttpStatus.CONFLICT, "Order is already paid");
            }
            if ("CANCELLED".equalsIgnoreCase(latest.status()) || "EXPIRED".equalsIgnoreCase(latest.status())) {
                checkoutRepository.releaseSeatLocksForOrder(order.orderId());
                return;
            }
            throw new ApiException(HttpStatus.CONFLICT, "Order is not in pending state");
        }

        checkoutRepository.releaseSeatLocksForOrder(order.orderId());
    }

    @Transactional
    public CheckoutResultResponse confirm(UUID userId, CheckoutConfirmRequest request) {
        virtualQueueService.requireAccess(request.eventId(), userId);
        CheckoutEvaluation evaluation = evaluate(
                userId,
                request.eventId(),
                request.seatIds(),
                request.voucherCode(),
                SeatValidationMode.CONFIRM,
                true
        );
        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
        OffsetDateTime expiresAt = resolveCheckoutExpiryForConfirm(evaluation.seats(), now);
        UUID orderId = UUID.randomUUID();
        boolean payWithBankQr = PAYMENT_METHOD_BANK_QR.equalsIgnoreCase(request.paymentMethod());
        Long payosOrderCode = payWithBankQr ? payOSPaymentService.generateOrderCode(orderId) : null;

        checkoutRepository.insertOrder(
                orderId,
                userId,
                evaluation.event().id(),
                evaluation.dbSubtotal(),
                evaluation.totalDiscount(),
                evaluation.totalAmount(),
                evaluation.voucher() == null ? null : evaluation.voucher().id(),
                expiresAt,
                payWithBankQr ? "PENDING" : "SUCCESS",
                payWithBankQr ? null : now,
                payosOrderCode
        );

        for (CheckoutQueryRepository.SeatCheckoutRow seat : evaluation.seats()) {
            checkoutRepository.insertOrderItemPending(
                    UUID.randomUUID(),
                    orderId,
                    seat.id(),
                    seat.price()
            );
        }

        if (evaluation.voucher() != null && evaluation.voucherDiscount().compareTo(BigDecimal.ZERO) > 0) {
            int updated = checkoutRepository.incrementVoucherUsage(evaluation.voucher().id());
            if (updated == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Voucher is no longer available");
            }

            try {
                checkoutRepository.insertVoucherRedemption(
                        UUID.randomUUID(),
                        evaluation.voucher().id(),
                        userId,
                        orderId,
                        evaluation.voucherDiscount(),
                        now
                );
            } catch (DataIntegrityViolationException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Voucher has already been redeemed");
            }
        }

        if (payWithBankQr) {
            String checkoutUrl = payOSPaymentService.createCheckoutUrl(
                    orderId,
                    payosOrderCode,
                    evaluation.totalAmount(),
                    evaluation.event().title(),
                    request.fullName().trim(),
                    request.email().trim(),
                    request.phone().trim(),
                    evaluation.seats(),
                    expiresAt
            );

            log.info("Checkout created in pending state for orderId={}, redirecting to payOS", orderId);
            virtualQueueService.leave(evaluation.event().id(), userId);
            return new CheckoutResultResponse(
                    orderId,
                    "PENDING",
                    now,
                    null,
                    toSummaryResponse(evaluation),
                    List.of(),
                    checkoutUrl
            );
        }

        checkoutRepository.markOrderSeatsSold(orderId);
        checkoutRepository.issueTicketsForOrder(orderId, now);
        List<CheckoutTicketResponse> tickets = checkoutRepository.findOrderTickets(orderId).stream()
                .map(row -> new CheckoutTicketResponse(
                        row.orderItemId(),
                        row.seatId(),
                        row.seatCode(),
                        row.ticketStatus(),
                        row.qrCode(),
                        row.issuedAt()
                ))
                .toList();
        sendOrderSuccessEmail(orderId);
        virtualQueueService.leave(evaluation.event().id(), userId);

        return new CheckoutResultResponse(
                orderId,
                "SUCCESS",
                now,
                now,
                toSummaryResponse(evaluation),
                tickets,
                null
        );
    }

    private void sendOrderSuccessEmail(UUID orderId) {
        List<PaymentConfirmationEmailService.TicketQrItem> ticketQrItems = checkoutRepository
                .findOrderTicketQrDetails(orderId).stream()
                .map(ticket -> new PaymentConfirmationEmailService.TicketQrItem(
                        ticket.seatCode(),
                        ticket.qrCode()
                ))
                .toList();

        checkoutRepository.findOrderEmailDetails(orderId).ifPresent(emailRow ->
                paymentConfirmationEmailService.sendOrderSuccessEmail(
                        new PaymentConfirmationEmailService.OrderSuccessEmailPayload(
                                emailRow.orderId(),
                                emailRow.email(),
                                emailRow.fullName(),
                                emailRow.eventTitle(),
                                emailRow.seatCodes(),
                                emailRow.totalAmount(),
                                ticketQrItems
                        )
                ));
    }

    private boolean isSuccessfulOrder(String status) {
        return status != null && ("SUCCESS".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status));
    }

    private CheckoutEvaluation evaluate(
            UUID userId,
            UUID eventId,
            List<UUID> rawSeatIds,
            String voucherCode,
            SeatValidationMode validationMode,
            boolean forUpdate
    ) {
        List<UUID> seatIds = normalizeSeatIds(rawSeatIds);
        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);

        CheckoutQueryRepository.EventCheckoutRow event = checkoutRepository.findBookableEvent(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event is not available for booking"));

        List<CheckoutQueryRepository.SeatCheckoutRow> seats = forUpdate
                ? checkoutRepository.findSeatsByIdsForUpdate(seatIds)
                : checkoutRepository.findSeatsByIds(seatIds);

        validateSeatOwnershipAndAvailability(userId, event.id(), seats, seatIds, now, validationMode);

        BigDecimal ticketSubtotal = seats.stream()
                .map(CheckoutQueryRepository.SeatCheckoutRow::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal membershipRate = checkoutRepository.findActiveMembershipDiscount(userId)
                .orElse(BigDecimal.ZERO);
        boolean membershipApplied = membershipRate.compareTo(BigDecimal.ZERO) > 0;

        BigDecimal membershipDiscount = percentage(ticketSubtotal, membershipRate);
        BigDecimal dbSubtotal = ticketSubtotal.add(SERVICE_FEE);
        membershipDiscount = membershipDiscount.min(dbSubtotal);

        BigDecimal voucherDiscount = BigDecimal.ZERO;
        CheckoutQueryRepository.VoucherRow voucher = null;
        String normalizedVoucherCode = normalizeVoucherCode(voucherCode);

        if (normalizedVoucherCode != null) {
            voucher = checkoutRepository.findVoucherByCode(normalizedVoucherCode)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Voucher code is invalid"));

            validateVoucher(voucher, userId, membershipApplied);
            BigDecimal afterMembership = ticketSubtotal.subtract(membershipDiscount).max(BigDecimal.ZERO);

            BigDecimal minOrder = defaultMoney(voucher.minOrderAmount());
            if (afterMembership.compareTo(minOrder) < 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Order must be at least " + formatMoney(minOrder) + " to apply voucher " + voucher.code()
                );
            }

            voucherDiscount = calculateVoucherDiscount(voucher, afterMembership);
        }

        BigDecimal totalDiscount = membershipDiscount.add(voucherDiscount);
        BigDecimal totalAmount = dbSubtotal.subtract(totalDiscount).max(BigDecimal.ZERO);

        List<String> seatCodes = seats.stream()
                .map(CheckoutQueryRepository.SeatCheckoutRow::seatCode)
                .sorted()
                .toList();

        return new CheckoutEvaluation(
                event,
                seats,
                voucher,
                seatCodes,
                ticketSubtotal,
                SERVICE_FEE,
                membershipDiscount,
                voucherDiscount,
                totalDiscount,
                dbSubtotal,
                totalAmount,
                membershipApplied
        );
    }

    private List<UUID> normalizeSeatIds(List<UUID> rawSeatIds) {
        if (rawSeatIds == null || rawSeatIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one seat is required");
        }
        Set<UUID> uniqueSeatIds = new LinkedHashSet<>(rawSeatIds);
        return new ArrayList<>(uniqueSeatIds);
    }

    private void validateSeatOwnershipAndAvailability(
            UUID userId,
            UUID eventId,
            List<CheckoutQueryRepository.SeatCheckoutRow> seats,
            List<UUID> requestedSeatIds,
            OffsetDateTime now,
            SeatValidationMode mode
    ) {
        if (seats.size() != requestedSeatIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Some selected seats do not exist");
        }

        for (CheckoutQueryRepository.SeatCheckoutRow seat : seats) {
            if (!eventId.equals(seat.eventId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Selected seats must belong to the same event");
            }

            boolean lockExpired = isLockExpired(seat.lockExpiresAt(), now);
            boolean lockedByCurrentUser = userId != null && userId.equals(seat.lockedBy()) && !lockExpired;
            String status = String.valueOf(seat.status()).toUpperCase(Locale.ROOT);

            if (mode == SeatValidationMode.RELEASE) {
                if ("SOLD".equals(status)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Seat " + seat.seatCode() + " is already sold");
                }
                if ("LOCKED".equals(status) && !lockedByCurrentUser && !lockExpired) {
                    throw new ApiException(HttpStatus.CONFLICT, "Seat " + seat.seatCode() + " is locked by another user");
                }
                continue;
            }

            if (mode == SeatValidationMode.LOCK) {
                if ("SOLD".equals(status)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Seat " + seat.seatCode() + " is already sold");
                }
                if ("LOCKED".equals(status) && !lockedByCurrentUser && !lockExpired) {
                    throw new ApiException(HttpStatus.CONFLICT, "Seat " + seat.seatCode() + " was just locked by another user");
                }
                continue;
            }

            if (mode == SeatValidationMode.CONFIRM) {
                if (!"LOCKED".equals(status) || !lockedByCurrentUser) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "Seat " + seat.seatCode() + " must be locked by your account before checkout"
                    );
                }
                continue;
            }

            if ("SOLD".equals(status)) {
                throw new ApiException(HttpStatus.CONFLICT, "Seat " + seat.seatCode() + " is already sold");
            }
            if ("LOCKED".equals(status) && !lockedByCurrentUser && !lockExpired) {
                throw new ApiException(HttpStatus.CONFLICT, "Seat " + seat.seatCode() + " is locked by another user");
            }
        }
    }

    private boolean isLockExpired(OffsetDateTime lockExpiresAt, OffsetDateTime now) {
        return lockExpiresAt != null && !lockExpiresAt.isAfter(now);
    }

    private void validateVoucher(CheckoutQueryRepository.VoucherRow voucher, UUID userId, boolean membershipApplied) {
        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
        if (!voucher.active() || voucher.startAt().isAfter(now) || voucher.endAt().isBefore(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Voucher is not active");
        }

        if (voucher.usageLimit() != null && voucher.usedCount() >= voucher.usageLimit()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Voucher usage limit has been reached");
        }

        if (voucher.membershipRequired() && !membershipApplied) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Voucher requires an active membership");
        }

        if (checkoutRepository.isVoucherRedeemedByUser(voucher.id(), userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Voucher has already been redeemed by this user");
        }
    }

    private BigDecimal calculateVoucherDiscount(CheckoutQueryRepository.VoucherRow voucher, BigDecimal amount) {
        BigDecimal discount;
        if ("PERCENT".equalsIgnoreCase(voucher.discountType())) {
            discount = percentage(amount, voucher.discountValue());
            if (voucher.maxDiscount() != null) {
                discount = discount.min(voucher.maxDiscount());
            }
        } else {
            discount = defaultMoney(voucher.discountValue());
        }

        return discount.min(amount).max(BigDecimal.ZERO);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal percent) {
        BigDecimal safeAmount = defaultMoney(amount);
        BigDecimal safePercent = defaultMoney(percent);
        return safeAmount.multiply(safePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private CheckoutSummaryResponse toSummaryResponse(CheckoutEvaluation evaluation) {
        return new CheckoutSummaryResponse(
                evaluation.event().id(),
                evaluation.event().title(),
                evaluation.seats().size(),
                evaluation.seatCodes(),
                evaluation.ticketSubtotal(),
                evaluation.serviceFee(),
                evaluation.membershipDiscount(),
                evaluation.voucherDiscount(),
                evaluation.totalAmount(),
                formatMoney(evaluation.totalAmount()),
                evaluation.membershipApplied(),
                evaluation.voucher() == null ? null : evaluation.voucher().code(),
                resolveSeatLockExpiry(evaluation.seats())
        );
    }

    private OffsetDateTime resolveSeatLockExpiry(List<CheckoutQueryRepository.SeatCheckoutRow> seats) {
        return seats.stream()
                .map(CheckoutQueryRepository.SeatCheckoutRow::lockExpiresAt)
                .filter(value -> value != null)
                .min(OffsetDateTime::compareTo)
                .orElse(null);
    }

    private OffsetDateTime resolveCheckoutExpiryForConfirm(
            List<CheckoutQueryRepository.SeatCheckoutRow> seats,
            OffsetDateTime now
    ) {
        OffsetDateTime minLockExpiry = resolveSeatLockExpiry(seats);
        if (minLockExpiry == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Selected seats are not locked anymore. Please reselect seats.");
        }
        if (!minLockExpiry.isAfter(now.plusSeconds(5))) {
            throw new ApiException(HttpStatus.CONFLICT, "Seat lock is about to expire. Please reselect seats.");
        }
        return minLockExpiry;
    }

    private String normalizeVoucherCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safe = defaultMoney(value);
        return NumberFormat.getNumberInstance(VIETNAM).format(safe) + " VND";
    }

    private record CheckoutEvaluation(
            CheckoutQueryRepository.EventCheckoutRow event,
            List<CheckoutQueryRepository.SeatCheckoutRow> seats,
            CheckoutQueryRepository.VoucherRow voucher,
            List<String> seatCodes,
            BigDecimal ticketSubtotal,
            BigDecimal serviceFee,
            BigDecimal membershipDiscount,
            BigDecimal voucherDiscount,
            BigDecimal totalDiscount,
            BigDecimal dbSubtotal,
            BigDecimal totalAmount,
            boolean membershipApplied
    ) {
    }

    private enum SeatValidationMode {
        PREVIEW,
        LOCK,
        RELEASE,
        CONFIRM
    }
}

package com.example.ticket.service;

import com.example.ticket.exception.ApiException;
import com.example.ticket.repository.CheckoutQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PaymentWebhookService {
    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile(
            "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"
    );

    private final PayOSPaymentService payOSPaymentService;
    private final CheckoutQueryRepository checkoutRepository;
    private final PaymentConfirmationEmailService paymentConfirmationEmailService;

    public PaymentWebhookService(
            PayOSPaymentService payOSPaymentService,
            CheckoutQueryRepository checkoutRepository,
            PaymentConfirmationEmailService paymentConfirmationEmailService
    ) {
        this.payOSPaymentService = payOSPaymentService;
        this.checkoutRepository = checkoutRepository;
        this.paymentConfirmationEmailService = paymentConfirmationEmailService;
    }

    @Transactional
    public String processWebhook(Webhook webhook) {
        WebhookData verifiedData = payOSPaymentService.verifyWebhookData(webhook);
        String paymentCode = verifiedData.getCode();
        String description = verifiedData.getDescription();
        Long orderCode = verifiedData.getOrderCode();
        CheckoutQueryRepository.OrderStatusRow order;

        if (orderCode != null) {
            order = checkoutRepository.findOrderStatusByPayOSOrderCode(orderCode)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found for payOS orderCode"));
        } else {
            UUID orderIdFromDescription = extractOrderId(description);
            order = checkoutRepository.findOrderStatus(orderIdFromDescription)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found for webhook"));
        }
        UUID orderId = order.orderId();

        if (isSuccessfulStatus(order.status())) {
            log.info("payOS webhook ignored because order already paid. orderId={}", orderId);
            return "Order already processed";
        }

        if (!"00".equals(paymentCode)) {
            OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
            int cancelled = checkoutRepository.markOrderCancelledIfPending(orderId, now);
            if (cancelled > 0) {
                checkoutRepository.releaseSeatLocksForOrder(orderId);
                log.info("payOS webhook marked order as cancelled and released seat locks. orderId={} code={}",
                        orderId, paymentCode);
            }
            log.warn("payOS webhook received non-success payment code. orderId={} code={} description={}",
                    orderId, paymentCode, description);
            return "Payment is not successful yet";
        }

        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
        int updated = checkoutRepository.markOrderPaidIfPending(orderId, now);
        if (updated == 0) {
            CheckoutQueryRepository.OrderStatusRow latest = checkoutRepository.findOrderStatus(orderId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found for webhook"));
            if (isSuccessfulStatus(latest.status())) {
                log.info("payOS webhook idempotent update detected. orderId={}", orderId);
                return "Order already processed";
            }
            throw new ApiException(HttpStatus.CONFLICT, "Order is not in pending state");
        }

        checkoutRepository.markOrderSeatsSold(orderId);
        checkoutRepository.issueTicketsForOrder(orderId, now);
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
                )
        );

        log.info("payOS webhook processed successfully for orderId={}", orderId);
        return "Payment processed successfully";
    }

    private UUID extractOrderId(String description) {
        if (description == null || description.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook description is empty");
        }
        Matcher matcher = ORDER_ID_PATTERN.matcher(description);
        if (!matcher.find()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot extract order id from webhook description");
        }
        try {
            return UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid order id format in webhook description");
        }
    }

    private boolean isSuccessfulStatus(String status) {
        return "PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }
}

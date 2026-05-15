package com.example.ticket.service;

import com.example.ticket.config.PayOSProperties;
import com.example.ticket.exception.ApiException;
import com.example.ticket.repository.CheckoutQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PayOSPaymentService {
    private static final Logger log = LoggerFactory.getLogger(PayOSPaymentService.class);
    /**
     * payOS valid range upper bound observed from API validation errors.
     * Keep order code in [PAYOS_ORDER_CODE_MIN, PAYOS_ORDER_CODE_MAX).
     */
    private static final long PAYOS_ORDER_CODE_MIN = 10_000_000_000L;
    private static final long PAYOS_ORDER_CODE_MAX = 9_007_199_254_740_991L;

    private final PayOS payOS;
    private final PayOSProperties properties;

    public PayOSPaymentService(PayOS payOS, PayOSProperties properties) {
        this.payOS = payOS;
        this.properties = properties;
    }

    public String createCheckoutUrl(
            UUID orderId,
            long orderCode,
            BigDecimal totalAmount,
            String eventTitle,
            String buyerName,
            String buyerEmail,
            String buyerPhone,
            List<CheckoutQueryRepository.SeatCheckoutRow> seats,
            OffsetDateTime expiresAt
    ) {
        String description = buildOrderReference(orderId);

        PaymentData paymentData = PaymentData.builder()
                .orderCode(orderCode)
                .amount(toIntegerAmount(totalAmount))
                .description(description)
                .returnUrl(properties.getReturnUrl())
                .cancelUrl(properties.getCancelUrl())
                .buyerName(buyerName)
                .buyerEmail(buyerEmail)
                .buyerPhone(buyerPhone)
                .expiredAt(expiresAt.toEpochSecond())
                .items(buildItems(eventTitle, seats))
                .build();

        try {
            CheckoutResponseData response = payOS.createPaymentLink(paymentData);
            String checkoutUrl = response.getCheckoutUrl();
            if (checkoutUrl == null || checkoutUrl.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "payOS returned an empty checkout URL");
            }
            log.info("Created payOS link for orderId={} orderCode={} paymentLinkId={}",
                    orderId, orderCode, response.getPaymentLinkId());
            return checkoutUrl;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to create payOS payment link for orderId={}", orderId, exception);
            String detail = exception.getMessage() == null ? "" : " - " + exception.getMessage();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Unable to create payOS payment link" + detail);
        }
    }

    public WebhookData verifyWebhookData(Webhook webhook) {
        try {
            return payOS.verifyPaymentWebhookData(webhook);
        } catch (Exception exception) {
            log.warn("Invalid payOS webhook signature or payload", exception);
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid payOS webhook signature");
        }
    }

    public PaymentLinkData getPaymentLinkInformation(long orderCode) {
        try {
            return payOS.getPaymentLinkInformation(orderCode);
        } catch (Exception exception) {
            log.error("Failed to fetch payOS payment link information for orderCode={}", orderCode, exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Unable to verify payOS payment status");
        }
    }

    public String buildOrderReference(UUID orderId) {
        String shortId = orderId.toString().replace("-", "");
        return "TR" + shortId.substring(0, 20);
    }

    public long generateOrderCode(UUID orderId) {
        return buildOrderCode(orderId);
    }

    private int toIntegerAmount(BigDecimal amount) {
        try {
            return amount.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Total amount must be an integer VND value");
        }
    }

    private long buildOrderCode(UUID orderId) {
        long mixed = orderId.getMostSignificantBits() ^ orderId.getLeastSignificantBits();
        long positive = mixed == Long.MIN_VALUE ? 0L : Math.abs(mixed);
        long span = PAYOS_ORDER_CODE_MAX - PAYOS_ORDER_CODE_MIN;
        return PAYOS_ORDER_CODE_MIN + (positive % span);
    }

    private List<ItemData> buildItems(String eventTitle, List<CheckoutQueryRepository.SeatCheckoutRow> seats) {
        String safeTitle = eventTitle == null || eventTitle.isBlank() ? "TicketRush Order" : eventTitle;
        return seats.stream()
                .map(seat -> ItemData.builder()
                        .name((safeTitle + " - " + seat.seatCode()).toUpperCase(Locale.ROOT))
                        .quantity(1)
                        .price(toIntegerAmount(seat.price()))
                        .build())
                .toList();
    }
}

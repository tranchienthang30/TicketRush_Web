package com.example.ticket.controller;

import com.example.ticket.dto.ApiMessageResponse;
import com.example.ticket.service.PaymentWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.type.Webhook;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentWebhookController {
    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiMessageResponse> receiveWebhook(@RequestBody Webhook webhook) {
        log.info("Received payOS webhook payload");
        String message = paymentWebhookService.processWebhook(webhook);
        return ResponseEntity.ok(new ApiMessageResponse(message));
    }
}

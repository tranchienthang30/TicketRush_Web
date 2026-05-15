package com.example.ticket.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentConfirmationEmailService {
    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmationEmailService.class);
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public PaymentConfirmationEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@ticketrush.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendOrderSuccessEmail(OrderSuccessEmailPayload payload) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(payload.email());
            helper.setSubject("TicketRush - Payment successful for order " + payload.orderId());
            helper.setText(buildHtml(payload), true);
            mailSender.send(message);
            log.info("Sent payment confirmation email for orderId={} to={}", payload.orderId(), payload.email());
        } catch (MessagingException | MailException exception) {
            log.error("Failed to send payment confirmation email for orderId={} to={}",
                    payload.orderId(), payload.email(), exception);
        }
    }

    private String buildHtml(OrderSuccessEmailPayload payload) {
        String fullName = sanitize(payload.fullName());
        String eventTitle = sanitize(payload.eventTitle());
        String seatCodes = sanitize(payload.seatCodes());
        String totalAmount = formatMoney(payload.totalAmount());
        String ticketQrRows = buildTicketQrRows(payload.tickets());

        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#172033;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f8;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e5e7eb;">
                          <tr>
                            <td style="background:#0f172a;color:#ffffff;padding:24px 28px;font-size:24px;font-weight:800;">
                              Ticket<span style="color:#f97316;">Rush</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <h1 style="margin:0 0 10px;font-size:24px;color:#0f172a;">Payment successful</h1>
                              <p style="margin:0 0 20px;color:#475569;line-height:1.6;">
                                Hi %s, your booking was confirmed successfully. Thank you for choosing TicketRush.
                              </p>

                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;">
                                <tr>
                                  <td style="background:#f8fafc;padding:12px 16px;width:38%%;font-weight:700;color:#475569;">Order ID</td>
                                  <td style="padding:12px 16px;color:#0f172a;font-weight:700;">%s</td>
                                </tr>
                                <tr>
                                  <td style="background:#f8fafc;padding:12px 16px;font-weight:700;color:#475569;">Movie/Event</td>
                                  <td style="padding:12px 16px;color:#0f172a;">%s</td>
                                </tr>
                                <tr>
                                  <td style="background:#f8fafc;padding:12px 16px;font-weight:700;color:#475569;">Seat codes</td>
                                  <td style="padding:12px 16px;color:#0f172a;">%s</td>
                                </tr>
                                <tr>
                                  <td style="background:#f8fafc;padding:12px 16px;font-weight:700;color:#475569;">Total paid</td>
                                  <td style="padding:12px 16px;color:#f97316;font-weight:800;">%s</td>
                                </tr>
                              </table>

                              <p style="margin:20px 0 0;color:#64748b;line-height:1.6;font-size:13px;">
                                You can view your ticket QR codes anytime in the "My Tickets" section.
                              </p>

                              <h2 style="margin:26px 0 12px;font-size:18px;color:#0f172a;">Ticket QR codes</h2>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;">
                                %s
                              </table>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(fullName, payload.orderId(), eventTitle, seatCodes, totalAmount, ticketQrRows);
    }

    private String buildTicketQrRows(List<TicketQrItem> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return """
                    <tr>
                      <td style="padding:14px 16px;color:#475569;">No ticket QR found.</td>
                    </tr>
                    """;
        }

        StringBuilder builder = new StringBuilder();
        for (TicketQrItem ticket : tickets) {
            String seatCode = sanitize(ticket.seatCode());
            String qrCode = sanitize(ticket.qrCode());
            String qrImageUrl = buildQrImageUrl(ticket.qrCode());
            builder.append("""
                    <tr>
                      <td style="padding:14px 16px;border-top:1px solid #e5e7eb;">
                        <p style="margin:0 0 8px;font-weight:700;color:#0f172a;">Seat %s</p>
                        <p style="margin:0 0 10px;font-size:12px;color:#64748b;">Ticket code: %s</p>
                        <img src="%s" alt="QR for seat %s" width="140" height="140" style="display:block;border-radius:8px;border:1px solid #e2e8f0;"/>
                      </td>
                    </tr>
                    """.formatted(seatCode, qrCode, qrImageUrl, seatCode));
        }
        return builder.toString();
    }

    private String buildQrImageUrl(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            return "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=TicketRush";
        }
        String encoded = URLEncoder.encode(qrCode, StandardCharsets.UTF_8);
        return "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + encoded;
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "-";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatMoney(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(VIETNAM).format(value == null ? BigDecimal.ZERO : value);
    }

    public record OrderSuccessEmailPayload(
            UUID orderId,
            String email,
            String fullName,
            String eventTitle,
            String seatCodes,
            BigDecimal totalAmount,
            List<TicketQrItem> tickets
    ) {
    }

    public record TicketQrItem(
            String seatCode,
            String qrCode
    ) {
    }
}

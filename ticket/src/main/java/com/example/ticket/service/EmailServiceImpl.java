package com.example.ticket.service;

import com.example.ticket.exception.AppException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@ticketrush.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordResetEmail(String to, String fullName, String resetLink) {
        sendHtml(to, "Reset your TicketRush password", buildPasswordResetHtml(fullName, resetLink));
    }

    @Override
    public void sendAccountVerificationEmail(String to, String fullName, String verificationLink) {
        sendHtml(to, "Verify your TicketRush email", buildVerificationHtml(
                "Verify your email",
                fullName,
                "Please verify this email address so TicketRush can send ticket QR codes and important purchase updates to you.",
                "Verify email",
                verificationLink
        ));
    }

    @Override
    public void sendOrganizationVerificationEmail(String to, String organizationName, String verificationLink) {
        sendHtml(to, "Verify your TicketRush organization", buildVerificationHtml(
                "Verify your organization",
                organizationName,
                "Please confirm this business email to activate your organization and unlock event management tools.",
                "Verify organization",
                verificationLink
        ));
    }

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            throw new AppException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Email service is unavailable. Please check SMTP configuration."
            );
        }
    }

    private String buildPasswordResetHtml(String fullName, String resetLink) {
        String displayName = (fullName == null || fullName.isBlank()) ? "there" : escapeHtml(fullName);
        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#172033;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f8;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e5e7eb;">
                          <tr>
                            <td style="background:#0f172a;color:#ffffff;padding:24px 28px;font-size:24px;font-weight:800;">
                              STAR<span style="color:#f97316;">LIGHT</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <h1 style="margin:0 0 12px;font-size:24px;color:#0f172a;">Reset your password</h1>
                              <p style="margin:0 0 18px;line-height:1.6;color:#475569;">Hi %s,</p>
                              <p style="margin:0 0 24px;line-height:1.6;color:#475569;">
                                We received a request to reset your TicketRush password. This link is valid for 15 minutes.
                              </p>
                              <p style="margin:0 0 28px;">
                                <a href="%s" style="display:inline-block;background:#f97316;color:#ffffff;text-decoration:none;font-weight:700;padding:13px 20px;border-radius:10px;">
                                  Reset password
                                </a>
                              </p>
                              <p style="margin:0 0 8px;line-height:1.6;color:#64748b;font-size:13px;">
                                If the button does not work, paste this link into your browser:
                              </p>
                              <p style="word-break:break-all;margin:0;color:#2563eb;font-size:13px;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 28px;background:#f8fafc;color:#64748b;font-size:12px;">
                              If you did not request this, you can safely ignore this email.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(displayName, resetLink, resetLink);
    }

    private String buildVerificationHtml(String title, String name, String body, String cta, String link) {
        String displayName = (name == null || name.isBlank()) ? "there" : escapeHtml(name);
        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#172033;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f8;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e5e7eb;">
                          <tr>
                            <td style="background:#0f172a;color:#ffffff;padding:24px 28px;font-size:24px;font-weight:800;">
                              STAR<span style="color:#f97316;">LIGHT</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <h1 style="margin:0 0 12px;font-size:24px;color:#0f172a;">%s</h1>
                              <p style="margin:0 0 18px;line-height:1.6;color:#475569;">Hi %s,</p>
                              <p style="margin:0 0 24px;line-height:1.6;color:#475569;">%s</p>
                              <p style="margin:0 0 28px;">
                                <a href="%s" style="display:inline-block;background:#f97316;color:#ffffff;text-decoration:none;font-weight:700;padding:13px 20px;border-radius:10px;">
                                  %s
                                </a>
                              </p>
                              <p style="margin:0 0 8px;line-height:1.6;color:#64748b;font-size:13px;">If the button does not work, paste this link into your browser:</p>
                              <p style="word-break:break-all;margin:0;color:#2563eb;font-size:13px;">%s</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(title), displayName, escapeHtml(body), link, escapeHtml(cta), link);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

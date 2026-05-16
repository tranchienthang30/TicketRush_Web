package com.example.ticket.service;

import com.example.ticket.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProviderNotificationService {
    private final EmailService emailService;
    private final String adminEmail;

    public ProviderNotificationService(
            EmailService emailService,
            @Value("${app.admin.email:}") String adminEmail
    ) {
        this.emailService = emailService;
        this.adminEmail = adminEmail;
    }

    public void notifyRequestSubmitted(User user) {
        emailService.sendProviderRequestSubmittedEmail(user.getEmail(), user.getFullName());
        if (adminEmail != null && !adminEmail.isBlank()) {
            emailService.sendProviderRequestAdminEmail(adminEmail, user.getFullName(), user.getEmail());
        }
    }

    public void notifyApproved(User user) {
        emailService.sendProviderApprovedEmail(user.getEmail(), user.getFullName());
    }

    public void notifyRejected(User user, String reason) {
        emailService.sendProviderRejectedEmail(user.getEmail(), user.getFullName(), reason);
    }
}

package com.example.ticket.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String fullName, String resetLink);

    void sendAccountVerificationEmail(String to, String fullName, String verificationLink);

    void sendOrganizationVerificationEmail(String to, String organizationName, String verificationLink);

    void sendProviderRequestSubmittedEmail(String to, String fullName);

    void sendProviderRequestAdminEmail(String to, String fullName, String requesterEmail);

    void sendProviderApprovedEmail(String to, String fullName);

    void sendProviderRejectedEmail(String to, String fullName, String reason);
}

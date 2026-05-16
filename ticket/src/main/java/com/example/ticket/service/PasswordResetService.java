package com.example.ticket.service;

public interface PasswordResetService {
    void requestPasswordReset(String email, String clientIp);

    void resetPassword(String token, String password);
}

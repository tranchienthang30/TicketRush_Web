package com.example.ticket.service;

import com.example.ticket.dto.request.LoginRequest;
import com.example.ticket.dto.request.RegisterRequest;
import com.example.ticket.dto.response.AuthSessionResponse;
import com.example.ticket.dto.response.UserResponse;

public interface AuthService {
    AuthSessionResponse register(RegisterRequest request);

    AuthSessionResponse login(LoginRequest request);

    AuthSessionResponse refresh(String refreshToken);

    void logout(String accessToken, String refreshToken);

    UserResponse currentUser();

    AuthSessionResponse authenticateGoogleUser(String email, String fullName, String providerId, String avatarUrl);

    void verifyEmail(String token);

    void resendVerificationEmail();
}

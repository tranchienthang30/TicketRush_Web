package com.example.ticket.service;

public interface RecaptchaService {
    void verify(String token, String expectedAction, String clientIp);
}

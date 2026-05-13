package com.example.ticket.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {
    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    public UUID resolve(UUID headerUserId) {
        return headerUserId == null ? DEMO_USER_ID : headerUserId;
    }
}

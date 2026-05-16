package com.example.ticket.controller;

import com.example.ticket.dto.response.UserResponse;
import com.example.ticket.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    UserResponse me() {
        return authService.currentUser();
    }
}

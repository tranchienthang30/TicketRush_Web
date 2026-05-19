package com.example.ticket.controller;

import com.example.ticket.dto.request.AdminUpdateUserRoleRequest;
import com.example.ticket.dto.response.AdminDashboardResponse;
import com.example.ticket.dto.response.AdminSystemResponse;
import com.example.ticket.dto.response.UserResponse;
import com.example.ticket.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return adminService.users();
    }

    @PutMapping("/users/{userId}/role")
    public UserResponse updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRoleRequest request
    ) {
        return adminService.updateUserRole(userId, request.role());
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
    }

    @GetMapping("/system")
    public AdminSystemResponse system() {
        return adminService.system();
    }
}

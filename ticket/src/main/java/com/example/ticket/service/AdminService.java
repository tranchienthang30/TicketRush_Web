package com.example.ticket.service;

import com.example.ticket.dto.response.AdminDashboardResponse;
import com.example.ticket.dto.response.AdminSystemResponse;
import com.example.ticket.dto.response.UserResponse;
import com.example.ticket.model.enums.UserRole;
import java.util.List;
import java.util.UUID;

public interface AdminService {
    AdminDashboardResponse dashboard();

    List<UserResponse> users();

    UserResponse updateUserRole(UUID userId, UserRole role);

    void deleteUser(UUID userId);

    AdminSystemResponse system();
}

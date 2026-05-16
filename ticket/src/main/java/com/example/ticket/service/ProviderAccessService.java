package com.example.ticket.service;

import com.example.ticket.dto.response.UserResponse;
import com.example.ticket.exception.AppException;
import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.UserRole;
import com.example.ticket.model.enums.UserStatus;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.security.JwtPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProviderAccessService {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final UserRepository userRepository;
    private final AccountVerificationService accountVerificationService;
    private final ProviderNotificationService providerNotificationService;

    public ProviderAccessService(
            UserRepository userRepository,
            AccountVerificationService accountVerificationService,
            ProviderNotificationService providerNotificationService
    ) {
        this.userRepository = userRepository;
        this.accountVerificationService = accountVerificationService;
        this.providerNotificationService = providerNotificationService;
    }

    public UserResponse requestProviderAccess() {
        User user = currentUser();
        if (user.getRole() == UserRole.ADMIN) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Admin accounts do not need provider access");
        }
        if (user.getRole() == UserRole.PROVIDER || STATUS_APPROVED.equals(user.getProviderRequestStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "This account already has provider access");
        }
        if (STATUS_PENDING.equals(user.getProviderRequestStatus())) {
            accountVerificationService.sendVerificationEmail(user, true);
            return UserResponse.from(user, currentSessionExpiresAt());
        }

        user.setProviderRequestStatus(STATUS_PENDING);
        user.setProviderRequestedAt(Instant.now());
        user.setProviderReviewedAt(null);
        user.setProviderReviewedBy(null);
        user.setProviderRejectionReason(null);
        User saved = userRepository.save(user);

        accountVerificationService.sendVerificationEmail(saved, true);
        providerNotificationService.notifyRequestSubmitted(saved);
        return UserResponse.from(saved, currentSessionExpiresAt());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> pendingRequests() {
        requireAdmin();
        return userRepository.findByProviderRequestStatusOrderByProviderRequestedAtAsc(STATUS_PENDING)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse approve(UUID userId) {
        User admin = requireAdmin();
        User user = findRequestUser(userId);
        if (user.getRole() == UserRole.ADMIN) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Admin accounts cannot be converted to provider");
        }
        user.setRole(UserRole.PROVIDER);
        user.setProviderRequestStatus(STATUS_APPROVED);
        user.setProviderReviewedAt(Instant.now());
        user.setProviderReviewedBy(admin.getId());
        user.setProviderRejectionReason(null);
        User saved = userRepository.save(user);
        providerNotificationService.notifyApproved(saved);
        return UserResponse.from(saved);
    }

    public UserResponse reject(UUID userId, String reason) {
        User admin = requireAdmin();
        User user = findRequestUser(userId);
        if (user.getRole() == UserRole.PROVIDER) {
            throw new AppException(HttpStatus.CONFLICT, "This account is already a provider");
        }
        user.setRole(UserRole.CUSTOMER);
        user.setProviderRequestStatus(STATUS_REJECTED);
        user.setProviderReviewedAt(Instant.now());
        user.setProviderReviewedBy(admin.getId());
        user.setProviderRejectionReason(blankToNull(reason));
        User saved = userRepository.save(user);
        providerNotificationService.notifyRejected(saved, reason);
        return UserResponse.from(saved);
    }

    private User findRequestUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Provider request was not found"));
        if (!STATUS_PENDING.equals(user.getProviderRequestStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "This provider request is not pending");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AppException(HttpStatus.FORBIDDEN, "This account is blocked");
        }
        return user;
    }

    private User requireAdmin() {
        User user = currentUser();
        if (user.getRole() != UserRole.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Admin permission is required");
        }
        return user;
    }

    private User currentUser() {
        return userRepository.findById(currentUserId())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User does not exist"));
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "You need to sign in");
        }
        return principal.userId();
    }

    private Instant currentSessionExpiresAt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return principal.expiresAt();
        }
        return null;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

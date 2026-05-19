package com.example.ticket.service;

import com.example.ticket.dto.response.AdminDashboardResponse;
import com.example.ticket.dto.response.AdminSystemResponse;
import com.example.ticket.dto.response.UserResponse;
import com.example.ticket.exception.AppException;
import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.UserRole;
import com.example.ticket.repository.AdminQueryRepository;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.security.JwtPrincipal;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.05");

    private final AdminQueryRepository adminQueryRepository;
    private final UserRepository userRepository;
    private final HealthEndpoint healthEndpoint;
    private final MeterRegistry meterRegistry;

    public AdminServiceImpl(
            AdminQueryRepository adminQueryRepository,
            UserRepository userRepository,
            HealthEndpoint healthEndpoint,
            MeterRegistry meterRegistry
    ) {
        this.adminQueryRepository = adminQueryRepository;
        this.userRepository = userRepository;
        this.healthEndpoint = healthEndpoint;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        requireAdmin();
        AdminQueryRepository.TicketRevenueRow revenue = adminQueryRepository.ticketRevenue();
        BigDecimal grossRevenue = revenue.grossRevenue() == null ? BigDecimal.ZERO : revenue.grossRevenue();
        return new AdminDashboardResponse(
                adminQueryRepository.countUsers(),
                adminQueryRepository.countEvents(),
                grossRevenue.multiply(PLATFORM_FEE_RATE),
                PLATFORM_FEE_RATE,
                revenue.ticketsSold(),
                grossRevenue,
                adminQueryRepository.usersByRole(),
                adminQueryRepository.eventsByStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> users() {
        requireAdmin();
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(UUID userId, UserRole role) {
        JwtPrincipal admin = requireAdmin();
        if (admin.userId().equals(userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Admins cannot change their own role");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User does not exist"));
        user.setRole(role);
        if (role == UserRole.PROVIDER) {
            user.setProviderRequestStatus("APPROVED");
            user.setProviderReviewedAt(java.time.Instant.now());
            user.setProviderReviewedBy(admin.userId());
            user.setProviderRejectionReason(null);
        } else if (role == UserRole.CUSTOMER) {
            user.setProviderRequestStatus(null);
            user.setProviderReviewedAt(null);
            user.setProviderReviewedBy(null);
            user.setProviderRejectionReason(null);
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        JwtPrincipal admin = requireAdmin();
        if (admin.userId().equals(userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Admins cannot delete their own account");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User does not exist"));
        adminQueryRepository.deleteUserOwnedData(userId);
        userRepository.delete(user);
    }

    @Override
    public AdminSystemResponse system() {
        requireAdmin();
        HealthComponent health = healthEndpoint.health();
        double uptimeMs = safeValue("process.uptime") * 1000;
        List<AdminSystemResponse.MetricResponse> metrics = List.of(
                metric("process.uptime", "Uptime", uptimeMs, "ms"),
                metric("process.cpu.usage", "Process CPU", value("process.cpu.usage"), "ratio"),
                metric("system.cpu.usage", "System CPU", value("system.cpu.usage"), "ratio"),
                metric("jvm.memory.used", "Heap used", value("jvm.memory.used", "area", "heap"), "bytes"),
                metric("jvm.memory.max", "Heap max", value("jvm.memory.max", "area", "heap"), "bytes"),
                metric("jvm.threads.live", "Live threads", value("jvm.threads.live"), "count"),
                metric("http.server.requests", "HTTP requests", timerCount("http.server.requests"), "count")
        );

        return new AdminSystemResponse(
                health.getStatus().getCode(),
                formatDuration(Duration.ofMillis(Math.max(0, Math.round(uptimeMs)))),
                value("process.cpu.usage"),
                value("system.cpu.usage"),
                value("jvm.memory.used", "area", "heap"),
                value("jvm.memory.max", "area", "heap"),
                value("jvm.threads.live"),
                timerCount("http.server.requests"),
                timerMean("http.server.requests"),
                metrics
        );
    }

    private AdminSystemResponse.MetricResponse metric(String name, String label, Double value, String unit) {
        return new AdminSystemResponse.MetricResponse(name, label, value, unit);
    }

    private Double value(String meterName, String... tags) {
        try {
            var search = meterRegistry.find(meterName);
            for (int index = 0; index + 1 < tags.length; index += 2) {
                search = search.tag(tags[index], tags[index + 1]);
            }
            Double value = search.gauge() == null ? null : search.gauge().value();
            if (value != null) {
                return value;
            }
            return search.timer() == null ? null : search.timer().mean(java.util.concurrent.TimeUnit.SECONDS);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private double safeValue(String meterName) {
        Double value = value(meterName);
        return value == null ? 0 : value;
    }

    private Double timerCount(String meterName) {
        try {
            return meterRegistry.find(meterName).timer() == null
                    ? null
                    : (double) meterRegistry.find(meterName).timer().count();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Double timerMean(String meterName) {
        try {
            return meterRegistry.find(meterName).timer() == null
                    ? null
                    : meterRegistry.find(meterName).timer().mean(java.util.concurrent.TimeUnit.SECONDS);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.toSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        if (hours > 0) {
            return "%dh %dm".formatted(hours, minutes);
        }
        if (minutes > 0) {
            return "%dm %ds".formatted(minutes, remainingSeconds);
        }
        return "%ds".formatted(remainingSeconds);
    }

    private JwtPrincipal requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "You need to sign in");
        }
        if (principal.role() != UserRole.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Admin permission is required");
        }
        return principal;
    }
}

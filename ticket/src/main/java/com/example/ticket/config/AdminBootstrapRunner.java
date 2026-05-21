package com.example.ticket.config;

import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.UserRole;
import com.example.ticket.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final String adminEmail;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            @Value("${app.admin.email:}") String adminEmail
    ) {
        this.userRepository = userRepository;
        this.adminEmail = adminEmail;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = normalize(adminEmail);
        if (email == null) {
            return;
        }

        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(this::promoteAdmin, () ->
                log.warn("ADMIN_EMAIL is set to {}, but no matching user exists yet", email));
    }

    private void promoteAdmin(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        user.setRole(UserRole.ADMIN);
        user.setProviderRequestStatus(null);
        user.setProviderRejectionReason(null);
        userRepository.save(user);
        log.info("Promoted {} to ADMIN from ADMIN_EMAIL", user.getEmail());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

package com.example.ticket.repository;

import com.example.ticket.model.entity.User;
import com.example.ticket.model.enums.AuthProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByProviderRequestStatusOrderByProviderRequestedAtAsc(String providerRequestStatus);
}

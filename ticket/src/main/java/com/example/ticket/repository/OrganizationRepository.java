package com.example.ticket.repository;

import com.example.ticket.model.entity.Organization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    boolean existsByBusinessEmailIgnoreCase(String businessEmail);

    Optional<Organization> findByBusinessEmailIgnoreCase(String businessEmail);

    List<Organization> findByOwnerId(UUID ownerId);
}

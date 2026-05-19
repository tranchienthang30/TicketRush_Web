package com.example.ticket.repository;

import com.example.ticket.model.entity.ProviderSeatWorkspace;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderSeatWorkspaceRepository extends JpaRepository<ProviderSeatWorkspace, UUID> {
}

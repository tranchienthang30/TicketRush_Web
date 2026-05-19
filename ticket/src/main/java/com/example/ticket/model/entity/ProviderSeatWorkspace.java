package com.example.ticket.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "provider_seat_workspaces")
@Getter
@Setter
public class ProviderSeatWorkspace {
    @Id
    @Column(name = "provider_id")
    private UUID providerId;

    @Column(name = "seatsio_workspace_id")
    private Long seatsioWorkspaceId;

    @Column(name = "workspace_name", nullable = false)
    private String workspaceName;

    @Column(name = "workspace_key", nullable = false, unique = true)
    private String workspaceKey;

    @Column(name = "workspace_secret_key", nullable = false)
    private String workspaceSecretKey;

    @Column(name = "is_test", nullable = false)
    private boolean test;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

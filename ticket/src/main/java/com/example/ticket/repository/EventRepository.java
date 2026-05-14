package com.example.ticket.repository;

import com.example.ticket.model.entity.Event;
import com.example.ticket.model.enums.EventStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {
    boolean existsBySlug(String slug);

    Optional<Event> findBySlug(String slug);

    List<Event> findByStatusOrderByStartTimeAsc(EventStatus status);

    List<Event> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
}

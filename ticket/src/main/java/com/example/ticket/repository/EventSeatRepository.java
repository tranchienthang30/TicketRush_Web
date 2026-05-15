package com.example.ticket.repository;

import com.example.ticket.model.entity.EventSeat;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSeatRepository extends JpaRepository<EventSeat, UUID> {
    long countByEventId(UUID eventId);
}

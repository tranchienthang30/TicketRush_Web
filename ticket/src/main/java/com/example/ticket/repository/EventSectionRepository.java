package com.example.ticket.repository;

import com.example.ticket.model.entity.EventSection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSectionRepository extends JpaRepository<EventSection, UUID> {
    List<EventSection> findByEvent_IdOrderByDisplayOrderAsc(UUID eventId);

    void deleteByEvent_Id(UUID eventId);
}

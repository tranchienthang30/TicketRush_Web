package com.example.ticket.repository;

import com.example.ticket.model.entity.EventSeat;
import com.example.ticket.model.enums.SeatStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventSeatRepository extends JpaRepository<EventSeat, UUID> {
    long countByEventId(UUID eventId);

    boolean existsByEventIdAndStatus(UUID eventId, SeatStatus status);

    void deleteByEventId(UUID eventId);

    List<EventSeat> findByEventIdOrderByLayoutYAscLayoutXAscSeatNumberAsc(UUID eventId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM order_items oi
                JOIN event_seats es ON es.id = oi.event_seat_id
                WHERE es.event_id = :eventId
            )
            """, nativeQuery = true)
    boolean existsOrderItemByEventId(@Param("eventId") UUID eventId);
}

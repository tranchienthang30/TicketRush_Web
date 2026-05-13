package com.example.ticket.repository;

import com.example.ticket.dto.ProfileResponse;
import com.example.ticket.dto.UpdateProfileRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProfileQueryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProfileQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ProfileResponse> findProfile(UUID userId) {
        String sql = """
                SELECT id, email, full_name, avatar_url, phone, gender, date_of_birth,
                       role::text AS role, status::text AS status, created_at, updated_at
                FROM users
                WHERE id = :userId
                """;

        List<ProfileResponse> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("userId", userId),
                this::mapProfile);
        return rows.stream().findFirst();
    }

    public void updateProfile(UUID userId, UpdateProfileRequest request) {
        String sql = """
                UPDATE users
                SET full_name = COALESCE(NULLIF(:fullName, ''), full_name),
                    avatar_url = :avatarUrl,
                    phone = :phone,
                    gender = :gender,
                    date_of_birth = :dateOfBirth
                WHERE id = :userId
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("fullName", request.fullName())
                .addValue("avatarUrl", request.avatarUrl())
                .addValue("phone", request.phone())
                .addValue("gender", request.gender())
                .addValue("dateOfBirth", request.dateOfBirth()));
    }

    public ProfileStatsRow findStats(UUID userId) {
        String sql = """
                SELECT
                    COALESCE((
                        SELECT COUNT(*)
                        FROM orders o
                        JOIN order_items oi ON oi.order_id = o.id
                        WHERE o.user_id = :userId
                        AND oi.ticket_status IN ('VALID', 'USED')
                    ), 0) AS tickets_booked,
                    COALESCE((
                        SELECT COUNT(DISTINCT o.event_id)
                        FROM orders o
                        JOIN events e ON e.id = o.event_id
                        JOIN order_items oi ON oi.order_id = o.id
                        WHERE o.user_id = :userId
                        AND o.status IN ('PAID', 'PENDING')
                        AND oi.ticket_status IN ('VALID', 'NOT_ISSUED')
                        AND e.start_time >= now()
                    ), 0) AS upcoming_events,
                    COALESCE((
                        SELECT SUM(o.discount_amount)
                        FROM orders o
                        WHERE o.user_id = :userId AND o.status = 'PAID'
                    ), 0) AS savings,
                    COALESCE((
                        SELECT SUM(o.total_amount)
                        FROM orders o
                        WHERE o.user_id = :userId AND o.status = 'PAID'
                    ), 0) AS total_spent
                """;

        return jdbcTemplate.queryForObject(sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> new ProfileStatsRow(
                        rs.getLong("tickets_booked"),
                        rs.getLong("upcoming_events"),
                        rs.getBigDecimal("savings"),
                        rs.getBigDecimal("total_spent")
                ));
    }

    public List<TicketRow> findTickets(UUID userId, String status, int limit) {
        String statusFilter = switch (status == null ? "upcoming" : status.toLowerCase()) {
            case "past" -> "AND e.start_time < now()\n";
            case "all" -> "";
            default -> "AND e.start_time >= now()\n";
        };

        String sql = """
                SELECT
                    o.id AS order_id,
                    e.id AS event_id,
                    e.slug AS event_slug,
                    e.title,
                    e.start_time,
                    COALESCE(NULLIF(e.location_name, ''), NULLIF(e.city, ''), e.address) AS location,
                    STRING_AGG(es.seat_code, ', ' ORDER BY es.seat_code) AS seat,
                    o.status::text AS order_status,
                    e.banner_url
                FROM orders o
                JOIN events e ON e.id = o.event_id
                JOIN order_items oi ON oi.order_id = o.id
                JOIN event_seats es ON es.id = oi.event_seat_id
                WHERE o.user_id = :userId
                AND o.status IN ('PAID', 'PENDING')
                AND oi.ticket_status IN ('VALID', 'USED', 'NOT_ISSUED')
                %s
                GROUP BY o.id, e.id, e.slug, e.title, e.start_time, e.location_name, e.city, e.address, o.status, e.banner_url
                ORDER BY e.start_time ASC
                LIMIT :limit
                """.formatted(statusFilter);

        return jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", limit), this::mapTicketRow);
    }

    public List<OrderRow> findOrders(UUID userId) {
        String sql = """
                SELECT
                    o.id,
                    o.event_id,
                    e.title AS event_title,
                    o.status::text AS status,
                    COUNT(oi.id) AS ticket_count,
                    o.subtotal,
                    o.discount_amount,
                    o.total_amount,
                    o.created_at,
                    o.paid_at
                FROM orders o
                JOIN events e ON e.id = o.event_id
                LEFT JOIN order_items oi ON oi.order_id = o.id
                WHERE o.user_id = :userId
                GROUP BY o.id, o.event_id, e.title, o.status, o.subtotal, o.discount_amount,
                         o.total_amount, o.created_at, o.paid_at
                ORDER BY o.created_at DESC
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("userId", userId), this::mapOrderRow);
    }

    public List<ActivityRow> findRecentActivities(UUID userId, int limit) {
        String sql = """
                SELECT title, occurred_at
                FROM (
                    SELECT
                        'Membership changed to ' || mp.name AS title,
                        um.created_at AS occurred_at
                    FROM user_memberships um
                    JOIN membership_plans mp ON mp.id = um.plan_id
                    WHERE um.user_id = :userId

                    UNION ALL

                    SELECT
                        'Booked ' || COUNT(oi.id) || ' ticket(s) for ' || e.title AS title,
                        COALESCE(o.paid_at, o.created_at) AS occurred_at
                    FROM orders o
                    JOIN events e ON e.id = o.event_id
                    JOIN order_items oi ON oi.order_id = o.id
                    WHERE o.user_id = :userId
                    GROUP BY o.id, e.title, o.paid_at, o.created_at
                ) activity
                ORDER BY occurred_at DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", limit), (rs, rowNum) -> new ActivityRow(
                rs.getString("title"),
                rs.getObject("occurred_at", OffsetDateTime.class)
        ));
    }

    private ProfileResponse mapProfile(ResultSet rs, int rowNum) throws SQLException {
        return new ProfileResponse(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getString("avatar_url"),
                rs.getString("phone"),
                rs.getString("gender"),
                rs.getObject("date_of_birth", LocalDate.class),
                rs.getString("role"),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private TicketRow mapTicketRow(ResultSet rs, int rowNum) throws SQLException {
        return new TicketRow(
                rs.getObject("order_id", UUID.class),
                rs.getObject("event_id", UUID.class),
                rs.getString("event_slug"),
                rs.getString("title"),
                rs.getObject("start_time", OffsetDateTime.class),
                rs.getString("location"),
                rs.getString("seat"),
                rs.getString("order_status"),
                rs.getString("banner_url")
        );
    }

    private OrderRow mapOrderRow(ResultSet rs, int rowNum) throws SQLException {
        return new OrderRow(
                rs.getObject("id", UUID.class),
                rs.getObject("event_id", UUID.class),
                rs.getString("event_title"),
                rs.getString("status"),
                rs.getInt("ticket_count"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("total_amount"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("paid_at", OffsetDateTime.class)
        );
    }

    public record ProfileStatsRow(
            long ticketsBooked,
            long upcomingEvents,
            BigDecimal savings,
            BigDecimal totalSpent
    ) {
    }

    public record TicketRow(
            UUID orderId,
            UUID eventId,
            String eventSlug,
            String title,
            OffsetDateTime startTime,
            String location,
            String seat,
            String status,
            String image
    ) {
    }

    public record OrderRow(
            UUID id,
            UUID eventId,
            String eventTitle,
            String status,
            int ticketCount,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            OffsetDateTime createdAt,
            OffsetDateTime paidAt
    ) {
    }

    public record ActivityRow(
            String title,
            OffsetDateTime occurredAt
    ) {
    }
}

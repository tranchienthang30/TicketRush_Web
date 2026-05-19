package com.example.ticket.repository;

import com.example.ticket.dto.response.AdminDashboardResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminQueryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countUsers() {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", new MapSqlParameterSource(), Long.class);
        return value == null ? 0 : value;
    }

    public long countEvents() {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", new MapSqlParameterSource(), Long.class);
        return value == null ? 0 : value;
    }

    public TicketRevenueRow ticketRevenue() {
        String sql = """
                SELECT
                    COUNT(oi.id) FILTER (
                        WHERE o.status IN ('PAID', 'SUCCESS')
                        AND oi.ticket_status IN ('VALID', 'USED')
                    ) AS tickets_sold,
                    COALESCE(SUM(oi.price_snapshot) FILTER (
                        WHERE o.status IN ('PAID', 'SUCCESS')
                        AND oi.ticket_status IN ('VALID', 'USED')
                    ), 0) AS gross_revenue
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                """;

        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), (rs, rowNum) -> new TicketRevenueRow(
                rs.getLong("tickets_sold"),
                rs.getBigDecimal("gross_revenue")
        ));
    }

    public List<AdminDashboardResponse.RoleCountResponse> usersByRole() {
        String sql = """
                SELECT role::text AS role, COUNT(*) AS total
                FROM users
                GROUP BY role
                ORDER BY role
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource(), (rs, rowNum) ->
                new AdminDashboardResponse.RoleCountResponse(rs.getString("role"), rs.getLong("total")));
    }

    public List<AdminDashboardResponse.StatusCountResponse> eventsByStatus() {
        String sql = """
                SELECT status::text AS status, COUNT(*) AS total
                FROM events
                GROUP BY status
                ORDER BY status
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource(), (rs, rowNum) ->
                new AdminDashboardResponse.StatusCountResponse(rs.getString("status"), rs.getLong("total")));
    }

    public void deleteUserOwnedData(UUID userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        jdbcTemplate.update("""
                UPDATE users
                SET primary_organization_id = NULL
                WHERE id = :userId
                OR primary_organization_id IN (
                    SELECT id FROM organizations WHERE owner_id = :userId
                )
                """, params);

        jdbcTemplate.update("""
                UPDATE event_seats
                SET status = 'AVAILABLE',
                    locked_by = NULL,
                    lock_expires_at = NULL
                WHERE locked_by = :userId
                """, params);

        jdbcTemplate.update("""
                DELETE FROM voucher_redemptions vr
                WHERE vr.user_id = :userId
                OR vr.order_id IN (
                    SELECT o.id
                    FROM orders o
                    WHERE o.user_id = :userId
                    OR o.event_id IN (SELECT e.id FROM events e WHERE e.provider_id = :userId)
                )
                """, params);

        jdbcTemplate.update("""
                DELETE FROM order_items oi
                USING orders o
                WHERE oi.order_id = o.id
                AND (
                    o.user_id = :userId
                    OR o.event_id IN (SELECT e.id FROM events e WHERE e.provider_id = :userId)
                )
                """, params);

        jdbcTemplate.update("""
                DELETE FROM orders o
                WHERE o.user_id = :userId
                OR o.event_id IN (SELECT e.id FROM events e WHERE e.provider_id = :userId)
                """, params);

        jdbcTemplate.update("""
                DELETE FROM events
                WHERE provider_id = :userId
                """, params);
    }

    public record TicketRevenueRow(long ticketsSold, BigDecimal grossRevenue) {
    }
}

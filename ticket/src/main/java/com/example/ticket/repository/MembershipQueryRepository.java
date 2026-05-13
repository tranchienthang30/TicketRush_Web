package com.example.ticket.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MembershipQueryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MembershipQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MembershipPlanRow> findActivePlans() {
        String sql = """
                SELECT id, name, description, price, duration_days, discount_percent
                FROM membership_plans
                WHERE is_active = true
                ORDER BY price ASC, id ASC
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource(), this::mapPlanRow);
    }

    public Optional<MembershipPlanRow> findActivePlanById(long planId) {
        String sql = """
                SELECT id, name, description, price, duration_days, discount_percent
                FROM membership_plans
                WHERE id = :planId AND is_active = true
                """;

        List<MembershipPlanRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("planId", planId),
                this::mapPlanRow);
        return rows.stream().findFirst();
    }

    public Optional<UserMembershipRow> findCurrentMembership(UUID userId) {
        String sql = """
                SELECT
                    um.id,
                    um.plan_id,
                    mp.name AS plan_name,
                    mp.description,
                    mp.discount_percent,
                    um.status,
                    um.start_at,
                    um.end_at,
                    (um.status = 'ACTIVE' AND um.start_at <= now() AND um.end_at >= now()) AS active
                FROM user_memberships um
                JOIN membership_plans mp ON mp.id = um.plan_id
                WHERE um.user_id = :userId
                ORDER BY
                    CASE WHEN um.status = 'ACTIVE' AND um.end_at >= now() THEN 0 ELSE 1 END,
                    um.end_at DESC
                LIMIT 1
                """;

        List<UserMembershipRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("userId", userId),
                this::mapUserMembershipRow);
        return rows.stream().findFirst();
    }

    public void cancelActiveMemberships(UUID userId) {
        String sql = """
                UPDATE user_memberships
                SET status = 'CANCELLED'
                WHERE user_id = :userId AND status = 'ACTIVE'
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("userId", userId));
    }

    public UUID insertMembership(UUID userId, MembershipPlanRow plan, OffsetDateTime startAt, OffsetDateTime endAt) {
        UUID id = UUID.randomUUID();
        String sql = """
                INSERT INTO user_memberships (id, user_id, plan_id, start_at, end_at, status, created_at)
                VALUES (:id, :userId, :planId, :startAt, :endAt, 'ACTIVE', now())
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId)
                .addValue("planId", plan.id())
                .addValue("startAt", startAt)
                .addValue("endAt", endAt));
        return id;
    }

    private MembershipPlanRow mapPlanRow(ResultSet rs, int rowNum) throws SQLException {
        return new MembershipPlanRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBigDecimal("price"),
                rs.getInt("duration_days"),
                rs.getBigDecimal("discount_percent")
        );
    }

    private UserMembershipRow mapUserMembershipRow(ResultSet rs, int rowNum) throws SQLException {
        return new UserMembershipRow(
                rs.getObject("id", UUID.class),
                rs.getLong("plan_id"),
                rs.getString("plan_name"),
                rs.getString("description"),
                rs.getBigDecimal("discount_percent"),
                rs.getString("status"),
                rs.getObject("start_at", OffsetDateTime.class),
                rs.getObject("end_at", OffsetDateTime.class),
                rs.getBoolean("active")
        );
    }

    public record MembershipPlanRow(
            long id,
            String name,
            String description,
            BigDecimal price,
            int durationDays,
            BigDecimal discountPercent
    ) {
    }

    public record UserMembershipRow(
            UUID id,
            long planId,
            String planName,
            String description,
            BigDecimal discountPercent,
            String status,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            boolean active
    ) {
    }
}

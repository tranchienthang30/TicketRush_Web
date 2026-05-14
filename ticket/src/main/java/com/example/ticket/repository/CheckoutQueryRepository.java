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
public class CheckoutQueryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CheckoutQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<EventCheckoutRow> findPublishedEvent(UUID eventId) {
        String sql = """
                SELECT
                    e.id,
                    e.title,
                    COALESCE(NULLIF(e.location_name, ''), NULLIF(e.city, ''), e.address) AS location
                FROM events e
                WHERE e.id = :eventId
                AND e.status = 'PUBLISHED'
                """;

        List<EventCheckoutRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("eventId", eventId),
                (rs, rowNum) -> new EventCheckoutRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("title"),
                        rs.getString("location")
                ));
        return rows.stream().findFirst();
    }

    public List<SeatCheckoutRow> findSeatsByIds(List<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT
                    es.id,
                    es.event_id,
                    es.seat_code,
                    es.price,
                    es.status::text AS status
                FROM event_seats es
                WHERE es.id IN (:seatIds)
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("seatIds", seatIds),
                (rs, rowNum) -> new SeatCheckoutRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("event_id", UUID.class),
                        rs.getString("seat_code"),
                        rs.getBigDecimal("price"),
                        rs.getString("status")
                ));
    }

    public Optional<BigDecimal> findActiveMembershipDiscount(UUID userId) {
        String sql = """
                SELECT mp.discount_percent
                FROM user_memberships um
                JOIN membership_plans mp ON mp.id = um.plan_id
                WHERE um.user_id = :userId
                AND um.status = 'ACTIVE'
                AND um.start_at <= now()
                AND um.end_at >= now()
                ORDER BY um.end_at DESC
                LIMIT 1
                """;

        List<BigDecimal> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getBigDecimal("discount_percent"));
        return rows.stream().findFirst();
    }

    public Optional<VoucherRow> findVoucherByCode(String code) {
        String sql = """
                SELECT
                    v.id,
                    v.code,
                    v.discount_type::text AS discount_type,
                    v.discount_value,
                    v.max_discount,
                    v.min_order_amount,
                    v.membership_required,
                    v.is_active,
                    v.start_at,
                    v.end_at,
                    v.usage_limit,
                    v.used_count
                FROM vouchers v
                WHERE lower(v.code) = lower(:code)
                LIMIT 1
                """;

        List<VoucherRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("code", code),
                this::mapVoucherRow);
        return rows.stream().findFirst();
    }

    public boolean isVoucherRedeemedByUser(UUID voucherId, UUID userId) {
        String sql = """
                SELECT EXISTS(
                    SELECT 1
                    FROM voucher_redemptions vr
                    WHERE vr.voucher_id = :voucherId
                    AND vr.user_id = :userId
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                .addValue("voucherId", voucherId)
                .addValue("userId", userId), Boolean.class);
        return exists != null && exists;
    }

    public int markSeatSold(UUID seatId, UUID eventId) {
        String sql = """
                UPDATE event_seats
                SET status = 'SOLD',
                    locked_by = NULL,
                    lock_expires_at = NULL,
                    version = version + 1,
                    updated_at = now()
                WHERE id = :seatId
                AND event_id = :eventId
                AND status = 'AVAILABLE'
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("seatId", seatId)
                .addValue("eventId", eventId));
    }

    public void insertOrder(
            UUID id,
            UUID userId,
            UUID eventId,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            UUID voucherId,
            OffsetDateTime expiresAt,
            OffsetDateTime paidAt
    ) {
        String sql = """
                INSERT INTO orders (
                    id, user_id, event_id, status, subtotal, discount_amount, total_amount,
                    voucher_id, expires_at, created_at, paid_at, cancelled_at
                )
                VALUES (
                    :id, :userId, :eventId, 'PAID', :subtotal, :discountAmount, :totalAmount,
                    :voucherId, :expiresAt, now(), :paidAt, NULL
                )
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId)
                .addValue("eventId", eventId)
                .addValue("subtotal", subtotal)
                .addValue("discountAmount", discountAmount)
                .addValue("totalAmount", totalAmount)
                .addValue("voucherId", voucherId)
                .addValue("expiresAt", expiresAt)
                .addValue("paidAt", paidAt));
    }

    public void insertOrderItem(
            UUID id,
            UUID orderId,
            UUID eventSeatId,
            BigDecimal priceSnapshot,
            String qrCode,
            OffsetDateTime issuedAt
    ) {
        String sql = """
                INSERT INTO order_items (
                    id, order_id, event_seat_id, price_snapshot, qr_code,
                    ticket_status, issued_at, checked_in_at
                )
                VALUES (
                    :id, :orderId, :eventSeatId, :priceSnapshot, :qrCode,
                    'VALID', :issuedAt, NULL
                )
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("orderId", orderId)
                .addValue("eventSeatId", eventSeatId)
                .addValue("priceSnapshot", priceSnapshot)
                .addValue("qrCode", qrCode)
                .addValue("issuedAt", issuedAt));
    }

    public int incrementVoucherUsage(UUID voucherId) {
        String sql = """
                UPDATE vouchers
                SET used_count = used_count + 1
                WHERE id = :voucherId
                AND is_active = true
                AND start_at <= now()
                AND end_at >= now()
                AND (usage_limit IS NULL OR used_count < usage_limit)
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource("voucherId", voucherId));
    }

    public void insertVoucherRedemption(
            UUID id,
            UUID voucherId,
            UUID userId,
            UUID orderId,
            BigDecimal discountAmount,
            OffsetDateTime usedAt
    ) {
        String sql = """
                INSERT INTO voucher_redemptions (
                    id, voucher_id, user_id, order_id, discount_amount, used_at
                )
                VALUES (
                    :id, :voucherId, :userId, :orderId, :discountAmount, :usedAt
                )
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("voucherId", voucherId)
                .addValue("userId", userId)
                .addValue("orderId", orderId)
                .addValue("discountAmount", discountAmount)
                .addValue("usedAt", usedAt));
    }

    public List<CheckoutTicketRow> findOrderTickets(UUID orderId) {
        String sql = """
                SELECT
                    oi.id AS order_item_id,
                    oi.event_seat_id,
                    es.seat_code,
                    oi.ticket_status::text AS ticket_status,
                    oi.qr_code,
                    oi.issued_at
                FROM order_items oi
                JOIN event_seats es ON es.id = oi.event_seat_id
                WHERE oi.order_id = :orderId
                ORDER BY es.seat_code
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("orderId", orderId),
                this::mapCheckoutTicketRow);
    }

    private VoucherRow mapVoucherRow(ResultSet rs, int rowNum) throws SQLException {
        Integer usageLimit = rs.getObject("usage_limit", Integer.class);
        return new VoucherRow(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("discount_type"),
                rs.getBigDecimal("discount_value"),
                rs.getBigDecimal("max_discount"),
                rs.getBigDecimal("min_order_amount"),
                rs.getBoolean("membership_required"),
                rs.getBoolean("is_active"),
                rs.getObject("start_at", OffsetDateTime.class),
                rs.getObject("end_at", OffsetDateTime.class),
                usageLimit,
                rs.getInt("used_count")
        );
    }

    private CheckoutTicketRow mapCheckoutTicketRow(ResultSet rs, int rowNum) throws SQLException {
        return new CheckoutTicketRow(
                rs.getObject("order_item_id", UUID.class),
                rs.getObject("event_seat_id", UUID.class),
                rs.getString("seat_code"),
                rs.getString("ticket_status"),
                rs.getString("qr_code"),
                rs.getObject("issued_at", OffsetDateTime.class)
        );
    }

    public record EventCheckoutRow(
            UUID id,
            String title,
            String location
    ) {
    }

    public record SeatCheckoutRow(
            UUID id,
            UUID eventId,
            String seatCode,
            BigDecimal price,
            String status
    ) {
    }

    public record VoucherRow(
            UUID id,
            String code,
            String discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscount,
            BigDecimal minOrderAmount,
            boolean membershipRequired,
            boolean active,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            Integer usageLimit,
            int usedCount
    ) {
    }

    public record CheckoutTicketRow(
            UUID orderItemId,
            UUID seatId,
            String seatCode,
            String ticketStatus,
            String qrCode,
            OffsetDateTime issuedAt
    ) {
    }
}

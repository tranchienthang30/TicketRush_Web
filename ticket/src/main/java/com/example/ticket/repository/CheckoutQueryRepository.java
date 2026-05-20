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

    public Optional<EventCheckoutRow> findBookableEvent(UUID eventId) {
        String sql = """
                SELECT
                    e.id,
                    e.title,
                    COALESCE(NULLIF(e.location_name, ''), NULLIF(e.city, ''), e.address) AS location
                FROM events e
                WHERE e.id = :eventId
                AND e.status = 'PUBLISHED'
                AND (e.sale_start_time IS NULL OR e.sale_start_time <= now())
                AND (e.sale_end_time IS NULL OR e.sale_end_time >= now())
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
                    es.status::text AS status,
                    es.locked_by,
                    es.lock_expires_at
                FROM event_seats es
                WHERE es.id IN (:seatIds)
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("seatIds", seatIds),
                this::mapSeatCheckoutRow);
    }

    public List<SeatCheckoutRow> findSeatsByIdsForUpdate(List<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT
                    es.id,
                    es.event_id,
                    es.seat_code,
                    es.price,
                    es.status::text AS status,
                    es.locked_by,
                    es.lock_expires_at
                FROM event_seats es
                WHERE es.id IN (:seatIds)
                FOR UPDATE
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("seatIds", seatIds),
                this::mapSeatCheckoutRow);
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

    public int setSeatLocks(UUID userId, UUID eventId, List<UUID> seatIds, OffsetDateTime lockExpiresAt) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0;
        }

        String sql = """
                UPDATE event_seats
                SET status = 'LOCKED',
                    locked_by = :userId,
                    lock_expires_at = :lockExpiresAt,
                    version = version + 1,
                    updated_at = now()
                WHERE event_id = :eventId
                AND id IN (:seatIds)
                AND status IN ('AVAILABLE', 'LOCKED')
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("eventId", eventId)
                .addValue("seatIds", seatIds)
                .addValue("lockExpiresAt", lockExpiresAt));
    }

    public int releaseSeatLocksForUser(UUID userId, UUID eventId, List<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0;
        }

        String sql = """
                UPDATE event_seats
                SET status = 'AVAILABLE',
                    locked_by = NULL,
                    lock_expires_at = NULL,
                    version = version + 1,
                    updated_at = now()
                WHERE event_id = :eventId
                AND id IN (:seatIds)
                AND status = 'LOCKED'
                AND locked_by = :userId
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("eventId", eventId)
                .addValue("seatIds", seatIds));
    }

    public int releaseExpiredSeatLocks(OffsetDateTime now) {
        String sql = """
                UPDATE event_seats
                SET status = 'AVAILABLE',
                    locked_by = NULL,
                    lock_expires_at = NULL,
                    version = version + 1,
                    updated_at = now()
                WHERE status = 'LOCKED'
                AND lock_expires_at IS NOT NULL
                AND lock_expires_at <= :now
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource("now", now));
    }

    public int markOrderSeatsSold(UUID orderId) {
        String sql = """
                UPDATE event_seats es
                SET status = 'SOLD',
                    locked_by = NULL,
                    lock_expires_at = NULL,
                    version = es.version + 1,
                    updated_at = now()
                FROM order_items oi
                WHERE oi.order_id = :orderId
                AND oi.event_seat_id = es.id
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource("orderId", orderId));
    }

    public int expirePendingOrders(OffsetDateTime now) {
        String sql = """
                UPDATE orders
                SET status = 'EXPIRED',
                    cancelled_at = :now
                WHERE status = 'PENDING'
                AND expires_at IS NOT NULL
                AND expires_at <= :now
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource("now", now));
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
            String status,
            OffsetDateTime paidAt,
            Long payosOrderCode
    ) {
        String sql = """
                INSERT INTO orders (
                    id, user_id, event_id, status, subtotal, discount_amount, total_amount,
                    voucher_id, expires_at, created_at, paid_at, cancelled_at, payos_order_code
                )
                VALUES (
                    :id, :userId, :eventId, CAST(:status AS order_status), :subtotal, :discountAmount, :totalAmount,
                    :voucherId, :expiresAt, now(), :paidAt, NULL, :payosOrderCode
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
                .addValue("status", status)
                .addValue("paidAt", paidAt)
                .addValue("payosOrderCode", payosOrderCode));
    }

    public void insertOrderItemPending(
            UUID id,
            UUID orderId,
            UUID eventSeatId,
            BigDecimal priceSnapshot
    ) {
        String sql = """
                INSERT INTO order_items (
                    id, order_id, event_seat_id, price_snapshot, qr_code,
                    ticket_status, issued_at, checked_in_at
                )
                VALUES (
                    :id, :orderId, :eventSeatId, :priceSnapshot, NULL,
                    'NOT_ISSUED', NULL, NULL
                )
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("orderId", orderId)
                .addValue("eventSeatId", eventSeatId)
                .addValue("priceSnapshot", priceSnapshot));
    }

    public int markOrderPaidIfPending(UUID orderId, OffsetDateTime paidAt) {
        String sql = """
                UPDATE orders
                SET status = 'SUCCESS',
                    paid_at = :paidAt
                WHERE id = :orderId
                AND status = 'PENDING'
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("paidAt", paidAt));
    }

    public int issueTicketsForOrder(UUID orderId, OffsetDateTime issuedAt) {
        String sql = """
                UPDATE order_items oi
                SET ticket_status = 'VALID',
                    qr_code = CONCAT(
                        'TR-',
                        UPPER(SUBSTRING(CAST(:orderId AS TEXT), 1, 8)),
                        '-',
                        es.seat_code,
                        '-',
                        UPPER(SUBSTRING(REPLACE(CAST(gen_random_uuid() AS TEXT), '-', ''), 1, 10))
                    ),
                    issued_at = :issuedAt
                FROM event_seats es
                WHERE oi.order_id = :orderId
                AND oi.event_seat_id = es.id
                AND oi.ticket_status = 'NOT_ISSUED'
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("issuedAt", issuedAt));
    }

    public Optional<OrderStatusRow> findOrderStatus(UUID orderId) {
        String sql = """
                SELECT id, status::text AS status
                FROM orders
                WHERE id = :orderId
                """;

        List<OrderStatusRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("orderId", orderId),
                (rs, rowNum) -> new OrderStatusRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("status")
                ));
        return rows.stream().findFirst();
    }

    public Optional<OrderStatusRow> findOrderStatusByPayOSOrderCode(Long orderCode) {
        String sql = """
                SELECT id, status::text AS status
                FROM orders
                WHERE payos_order_code = :orderCode
                """;

        List<OrderStatusRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("orderCode", orderCode),
                (rs, rowNum) -> new OrderStatusRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("status")
                ));
        return rows.stream().findFirst();
    }

    public Optional<OrderStatusRow> findOrderStatusByPayOSOrderCodeAndUserId(Long orderCode, UUID userId) {
        String sql = """
                SELECT id, status::text AS status
                FROM orders
                WHERE payos_order_code = :orderCode
                AND user_id = :userId
                """;

        List<OrderStatusRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource()
                        .addValue("orderCode", orderCode)
                        .addValue("userId", userId),
                (rs, rowNum) -> new OrderStatusRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("status")
                ));
        return rows.stream().findFirst();
    }

    public Optional<OrderEmailRow> findOrderEmailDetails(UUID orderId) {
        String sql = """
                SELECT
                    o.id AS order_id,
                    u.email,
                    u.full_name,
                    e.title AS event_title,
                    COALESCE(string_agg(es.seat_code, ', ' ORDER BY es.seat_code), '') AS seat_codes,
                    o.total_amount
                FROM orders o
                JOIN users u ON u.id = o.user_id
                JOIN events e ON e.id = o.event_id
                LEFT JOIN order_items oi ON oi.order_id = o.id
                LEFT JOIN event_seats es ON es.id = oi.event_seat_id
                WHERE o.id = :orderId
                GROUP BY o.id, u.email, u.full_name, e.title, o.total_amount
                """;

        List<OrderEmailRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("orderId", orderId),
                (rs, rowNum) -> new OrderEmailRow(
                        rs.getObject("order_id", UUID.class),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("event_title"),
                        rs.getString("seat_codes"),
                        rs.getBigDecimal("total_amount")
                ));

        return rows.stream().findFirst();
    }

    public List<OrderTicketQrRow> findOrderTicketQrDetails(UUID orderId) {
        String sql = """
                SELECT
                    es.seat_code,
                    oi.qr_code
                FROM order_items oi
                JOIN event_seats es ON es.id = oi.event_seat_id
                WHERE oi.order_id = :orderId
                ORDER BY es.seat_code
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("orderId", orderId),
                (rs, rowNum) -> new OrderTicketQrRow(
                        rs.getString("seat_code"),
                        rs.getString("qr_code")
                ));
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

    private SeatCheckoutRow mapSeatCheckoutRow(ResultSet rs, int rowNum) throws SQLException {
        return new SeatCheckoutRow(
                rs.getObject("id", UUID.class),
                rs.getObject("event_id", UUID.class),
                rs.getString("seat_code"),
                rs.getBigDecimal("price"),
                rs.getString("status"),
                rs.getObject("locked_by", UUID.class),
                rs.getObject("lock_expires_at", OffsetDateTime.class)
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
            String status,
            UUID lockedBy,
            OffsetDateTime lockExpiresAt
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

    public record OrderStatusRow(
            UUID orderId,
            String status
    ) {
    }

    public record OrderEmailRow(
            UUID orderId,
            String email,
            String fullName,
            String eventTitle,
            String seatCodes,
            BigDecimal totalAmount
    ) {
    }

    public record OrderTicketQrRow(
            String seatCode,
            String qrCode
    ) {
    }
}

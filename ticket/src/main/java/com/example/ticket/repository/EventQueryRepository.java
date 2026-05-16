package com.example.ticket.repository;

import com.example.ticket.dto.CategoryResponse;
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
public class EventQueryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EventQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CategoryResponse> findActiveCategories() {
        String sql = """
                SELECT id, name, slug, description, image_url
                FROM categories
                WHERE is_active = true
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource(), (rs, rowNum) -> new CategoryResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getString("image_url")
        ));
    }

    public List<EventRow> findPublishedEventsByCategory(long categoryId, int limit) {
        String sql = baseEventSql("""
                e.category_id = :categoryId
                ORDER BY e.start_time ASC
                LIMIT :limit
                """);

        return jdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("categoryId", categoryId)
                .addValue("limit", limit), this::mapEventRow);
    }

    public List<EventRow> findPublishedEvents(Long categoryId, String query, String city, int page, int size) {
        String where = buildEventWhere(categoryId, query, city);
        String sql = baseEventSql(where + """
                ORDER BY e.start_time ASC
                LIMIT :limit OFFSET :offset
                """);

        return jdbcTemplate.query(sql, buildEventParams(categoryId, query, city)
                .addValue("limit", size)
                .addValue("offset", page * size), this::mapEventRow);
    }

    public long countPublishedEvents(Long categoryId, String query, String city) {
        String sql = """
                SELECT COUNT(*)
                FROM events e
                WHERE e.status = 'PUBLISHED'
                AND %s
                """.formatted(buildEventWhere(categoryId, query, city));

        Long total = jdbcTemplate.queryForObject(sql, buildEventParams(categoryId, query, city), Long.class);
        return total == null ? 0 : total;
    }

    public Optional<BookingEventRow> findPublishedEventForBooking(UUID eventId) {
        String sql = """
                SELECT
                    e.id,
                    e.slug,
                    e.title,
                    e.banner_url,
                    COALESCE(NULLIF(e.location_name, ''), NULLIF(e.city, ''), e.address) AS location,
                    ch.name AS hall_name,
                    e.status::text AS status,
                    e.start_time,
                    e.sale_start_time,
                    e.sale_end_time,
                    (SELECT COUNT(*) FROM event_seats WHERE event_id = e.id AND status = 'AVAILABLE') AS available_seats,
                    (SELECT COUNT(*) FROM event_seats WHERE event_id = e.id AND status = 'SOLD') AS sold_seats
                FROM events e
                LEFT JOIN cinema_halls ch ON ch.id = e.hall_id
                WHERE e.id = :eventId
                AND e.status = 'PUBLISHED'
                AND COALESCE(e.listing_type, 'NOW_SHOWING') <> 'UPCOMING'
                AND (e.sale_start_time IS NULL OR e.sale_start_time <= now())
                AND (e.sale_end_time IS NULL OR e.sale_end_time >= now())
                """;

        List<BookingEventRow> rows = jdbcTemplate.query(sql,
                new MapSqlParameterSource("eventId", eventId),
                this::mapBookingEventRow);
        return rows.stream().findFirst();
    }

    public List<BookingSectionRow> findSectionsByEvent(UUID eventId) {
        String sql = """
                SELECT id, event_id, name, base_price, row_count, seats_per_row, display_order
                FROM event_sections
                WHERE event_id = :eventId
                ORDER BY display_order ASC, name ASC
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("eventId", eventId),
                this::mapBookingSectionRow);
    }

    public List<BookingSeatRow> findSeatsByEvent(UUID eventId) {
        String sql = """
                SELECT
                    id,
                    event_id,
                    section_id,
                    row_label,
                    seat_number,
                    seat_code,
                    price,
                    status::text AS status,
                    COALESCE(seat_type_code, 'STANDARD') AS seat_type_code,
                    layout_x,
                    layout_y,
                    is_hidden,
                    is_accessible
                FROM event_seats
                WHERE event_id = :eventId
                ORDER BY row_label ASC, seat_number ASC
                """;

        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("eventId", eventId),
                this::mapBookingSeatRow);
    }

    private String baseEventSql(String extraWhereAndOrder) {
        return """
                SELECT
                    e.id,
                    e.slug,
                    e.title,
                    e.start_time,
                    c.name AS category_name,
                    COALESCE(e.duration_minutes, GREATEST(1, ROUND(EXTRACT(EPOCH FROM (e.end_time - e.start_time)) / 60)::int)) AS duration_minutes,
                    COALESCE(e.listing_type, 'NOW_SHOWING') AS listing_type,
                    COALESCE(NULLIF(e.location_name, ''), NULLIF(e.city, ''), e.address) AS location,
                    e.city,
                    e.banner_url,
                    e.sale_start_time,
                    e.sale_end_time,
                    COALESCE((SELECT MIN(base_price) FROM event_sections WHERE event_id = e.id), 0) AS min_price,
                    (SELECT COUNT(*) FROM event_seats WHERE event_id = e.id AND status = 'AVAILABLE') AS available_seats,
                    (SELECT COUNT(*) FROM event_seats WHERE event_id = e.id AND status = 'SOLD') AS sold_seats
                FROM events e
                    LEFT JOIN categories c ON c.id = e.category_id
                WHERE e.status = 'PUBLISHED'
                AND %s
                """.formatted(extraWhereAndOrder);
    }

    private String buildEventWhere(Long categoryId, String query, String city) {
        StringBuilder where = new StringBuilder("1 = 1\n");

        if (categoryId != null) {
            where.append("AND e.category_id = :categoryId\n");
        }

        if (query != null && !query.isBlank()) {
            where.append("""
                    AND (
                        e.title ILIKE :query
                        OR e.description ILIKE :query
                        OR e.city ILIKE :query
                        OR e.location_name ILIKE :query
                    )
                    """);
        }

        if (city != null && !city.isBlank()) {
            where.append("AND e.city ILIKE :city\n");
        }

        return where.toString();
    }

    private MapSqlParameterSource buildEventParams(Long categoryId, String query, String city) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (categoryId != null) {
            params.addValue("categoryId", categoryId);
        }

        if (query != null && !query.isBlank()) {
            params.addValue("query", "%" + query.trim() + "%");
        }

        if (city != null && !city.isBlank()) {
            params.addValue("city", "%" + city.trim() + "%");
        }

        return params;
    }

    private EventRow mapEventRow(ResultSet rs, int rowNum) throws SQLException {
        return new EventRow(
                rs.getObject("id", UUID.class),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getObject("start_time", OffsetDateTime.class),
                rs.getString("location"),
                rs.getString("city"),
                rs.getString("banner_url"),
                rs.getObject("sale_start_time", OffsetDateTime.class),
                rs.getObject("sale_end_time", OffsetDateTime.class),
                rs.getBigDecimal("min_price"),
                rs.getLong("available_seats"),
                rs.getLong("sold_seats"),
                rs.getString("category_name"),
                rs.getObject("duration_minutes", Integer.class),
                rs.getString("listing_type")
        );
    }

    private BookingEventRow mapBookingEventRow(ResultSet rs, int rowNum) throws SQLException {
        return new BookingEventRow(
                rs.getObject("id", UUID.class),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("banner_url"),
                rs.getString("location"),
                rs.getString("hall_name"),
                rs.getString("status"),
                rs.getObject("start_time", OffsetDateTime.class),
                rs.getObject("sale_start_time", OffsetDateTime.class),
                rs.getObject("sale_end_time", OffsetDateTime.class),
                rs.getLong("available_seats"),
                rs.getLong("sold_seats")
        );
    }

    private BookingSectionRow mapBookingSectionRow(ResultSet rs, int rowNum) throws SQLException {
        return new BookingSectionRow(
                rs.getObject("id", UUID.class),
                rs.getObject("event_id", UUID.class),
                rs.getString("name"),
                rs.getBigDecimal("base_price"),
                rs.getInt("row_count"),
                rs.getInt("seats_per_row"),
                rs.getInt("display_order")
        );
    }

    private BookingSeatRow mapBookingSeatRow(ResultSet rs, int rowNum) throws SQLException {
        return new BookingSeatRow(
                rs.getObject("id", UUID.class),
                rs.getObject("event_id", UUID.class),
                rs.getObject("section_id", UUID.class),
                rs.getString("row_label"),
                rs.getInt("seat_number"),
                rs.getString("seat_code"),
                rs.getBigDecimal("price"),
                rs.getString("status"),
                rs.getString("seat_type_code"),
                rs.getObject("layout_x", Integer.class),
                rs.getObject("layout_y", Integer.class),
                rs.getBoolean("is_hidden"),
                rs.getBoolean("is_accessible")
        );
    }

    public record EventRow(
            UUID id,
            String slug,
            String title,
            OffsetDateTime startTime,
            String location,
            String city,
            String bannerUrl,
            OffsetDateTime saleStartTime,
            OffsetDateTime saleEndTime,
            BigDecimal minPrice,
            long availableSeats,
            long soldSeats,
            String categoryName,
            Integer durationMinutes,
            String listingType
    ) {
    }

    public record BookingEventRow(
            UUID id,
            String slug,
            String title,
            String bannerUrl,
            String location,
            String hallName,
            String status,
            OffsetDateTime startTime,
            OffsetDateTime saleStartTime,
            OffsetDateTime saleEndTime,
            long availableSeats,
            long soldSeats
    ) {
    }

    public record BookingSectionRow(
            UUID id,
            UUID eventId,
            String name,
            BigDecimal basePrice,
            int rowCount,
            int seatsPerRow,
            int displayOrder
    ) {
    }

    public record BookingSeatRow(
            UUID id,
            UUID eventId,
            UUID sectionId,
            String rowLabel,
            int seatNumber,
            String seatCode,
            BigDecimal price,
            String status,
            String seatTypeCode,
            Integer layoutX,
            Integer layoutY,
            boolean hidden,
            boolean accessible
    ) {
    }
}

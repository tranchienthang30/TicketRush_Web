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

    private String baseEventSql(String extraWhereAndOrder) {
        return """
                SELECT
                    e.id,
                    e.slug,
                    e.title,
                    e.start_time,
                    COALESCE(NULLIF(e.location_name, ''), NULLIF(e.city, ''), e.address) AS location,
                    e.city,
                    e.banner_url,
                    e.sale_start_time,
                    e.sale_end_time,
                    COALESCE((SELECT MIN(base_price) FROM event_sections WHERE event_id = e.id), 0) AS min_price,
                    (SELECT COUNT(*) FROM event_seats WHERE event_id = e.id AND status = 'AVAILABLE') AS available_seats,
                    (SELECT COUNT(*) FROM event_seats WHERE event_id = e.id AND status = 'SOLD') AS sold_seats
                FROM events e
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
                rs.getLong("sold_seats")
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
            long soldSeats
    ) {
    }
}

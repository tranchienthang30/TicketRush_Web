-- Normalize seat prices for demo/local environment:
-- STANDARD: 10,000 VND
-- VIP:      20,000 VND
-- COUPLE:   40,000 VND

UPDATE event_seats
SET price = CASE
        WHEN upper(COALESCE(seat_type_code, 'STANDARD')) = 'VIP' THEN 20000
        WHEN upper(COALESCE(seat_type_code, 'STANDARD')) IN ('COUPLE', 'SWEETBOX') THEN 40000
        ELSE 10000
    END,
    updated_at = now();

UPDATE event_sections sec
SET base_price = CASE
        WHEN upper(COALESCE(sec.seat_type_code, '')) = 'VIP' THEN 20000
        WHEN upper(COALESCE(sec.seat_type_code, '')) IN ('COUPLE', 'SWEETBOX') THEN 40000
        WHEN EXISTS (
            SELECT 1
            FROM event_seats es
            WHERE es.section_id = sec.id
              AND upper(COALESCE(es.seat_type_code, 'STANDARD')) = 'VIP'
        ) THEN 20000
        WHEN EXISTS (
            SELECT 1
            FROM event_seats es
            WHERE es.section_id = sec.id
              AND upper(COALESCE(es.seat_type_code, 'STANDARD')) IN ('COUPLE', 'SWEETBOX')
        ) THEN 40000
        ELSE 10000
    END,
    updated_at = now();

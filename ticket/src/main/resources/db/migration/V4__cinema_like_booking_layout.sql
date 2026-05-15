-- V4__cinema_like_booking_layout.sql
-- Refine demo booking layout to resemble a real cinema map (rows A-K, VIP block, couple row).

ALTER TABLE events
ADD COLUMN IF NOT EXISTS seat_provider VARCHAR(30) NOT NULL DEFAULT 'INTERNAL',
ADD COLUMN IF NOT EXISTS external_seat_chart_key VARCHAR(255),
ADD COLUMN IF NOT EXISTS payout_bank_name VARCHAR(120),
ADD COLUMN IF NOT EXISTS payout_account_name VARCHAR(120),
ADD COLUMN IF NOT EXISTS payout_account_number VARCHAR(60),
ADD COLUMN IF NOT EXISTS provider_terms_accepted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_events_seat_provider ON events(seat_provider);
CREATE INDEX IF NOT EXISTS idx_events_external_seat_chart_key ON events(external_seat_chart_key);

-- =========================================================
-- TARGET EVENT / HALL DISPLAY
-- =========================================================

UPDATE cinema_halls
SET name = 'Phòng chiếu số 8'
WHERE id = '91000000-0000-0000-0000-000000000001';

UPDATE events
SET hall_id = '91000000-0000-0000-0000-000000000001'::uuid
WHERE id = '10000000-0000-0000-0000-000000000001';

-- =========================================================
-- KEEP OLD DATA FOR HISTORY BUT HIDE FROM NEW BOOKING UI
-- =========================================================

UPDATE event_seats
SET is_hidden = true
WHERE event_id = '10000000-0000-0000-0000-000000000001'::uuid;

-- =========================================================
-- NEW SECTIONS FOR REALISTIC CINEMA MAP
-- =========================================================

INSERT INTO event_sections (
    id, event_id, name, base_price, row_count, seats_per_row, display_order,
    seat_type_code, visual_color_hex, note, created_at, updated_at
)
SELECT
    '22000000-0000-0000-0000-000000000001'::uuid,
    '10000000-0000-0000-0000-000000000001'::uuid,
    'STANDARD-HALL8',
    120000,
    10,
    14,
    11,
    'STANDARD',
    '#1E293B',
    'Standard rows around premium block',
    now(),
    now()
WHERE NOT EXISTS (
    SELECT 1
    FROM event_sections
    WHERE id = '22000000-0000-0000-0000-000000000001'::uuid
);

INSERT INTO event_sections (
    id, event_id, name, base_price, row_count, seats_per_row, display_order,
    seat_type_code, visual_color_hex, note, created_at, updated_at
)
SELECT
    '22000000-0000-0000-0000-000000000002'::uuid,
    '10000000-0000-0000-0000-000000000001'::uuid,
    'VIP-HALL8',
    170000,
    6,
    10,
    12,
    'VIP',
    '#F97316',
    'Premium center block D-I',
    now(),
    now()
WHERE NOT EXISTS (
    SELECT 1
    FROM event_sections
    WHERE id = '22000000-0000-0000-0000-000000000002'::uuid
);

INSERT INTO event_sections (
    id, event_id, name, base_price, row_count, seats_per_row, display_order,
    seat_type_code, visual_color_hex, note, created_at, updated_at
)
SELECT
    '22000000-0000-0000-0000-000000000003'::uuid,
    '10000000-0000-0000-0000-000000000001'::uuid,
    'COUPLE-HALL8',
    250000,
    1,
    12,
    13,
    'COUPLE',
    '#F43F5E',
    'Rear couple row K',
    now(),
    now()
WHERE NOT EXISTS (
    SELECT 1
    FROM event_sections
    WHERE id = '22000000-0000-0000-0000-000000000003'::uuid
);

-- =========================================================
-- NEW SEAT MAP (A-J: 14 seats, K: 12 seats)
-- =========================================================

-- A-J rows (14 seats each)
INSERT INTO event_seats (
    id, event_id, section_id, row_label, seat_number, seat_code, price,
    status, locked_by, lock_expires_at, version, seat_type_code, layout_x, layout_y,
    pair_group, is_accessible, is_hidden, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001'::uuid,
    CASE
        WHEN r.row_label IN ('D', 'E', 'F', 'G', 'H', 'I') AND s.seat_no BETWEEN 3 AND 12
            THEN '22000000-0000-0000-0000-000000000002'::uuid
        ELSE '22000000-0000-0000-0000-000000000001'::uuid
    END,
    r.row_label,
    s.seat_no,
    r.row_label || s.seat_no,
    CASE
        WHEN r.row_label IN ('D', 'E', 'F', 'G', 'H', 'I') AND s.seat_no BETWEEN 3 AND 12
            THEN 170000
        ELSE 120000
    END,
    CASE
        WHEN r.row_label = 'H' AND s.seat_no BETWEEN 7 AND 10 THEN 'SOLD'::seat_status
        ELSE 'AVAILABLE'::seat_status
    END,
    NULL,
    NULL,
    0,
    CASE
        WHEN r.row_label IN ('D', 'E', 'F', 'G', 'H', 'I') AND s.seat_no BETWEEN 3 AND 12
            THEN 'VIP'
        ELSE 'STANDARD'
    END,
    s.seat_no,
    r.row_index,
    NULL,
    false,
    false,
    now(),
    now()
FROM (
    VALUES
        (1, 'A'),
        (2, 'B'),
        (3, 'C'),
        (4, 'D'),
        (5, 'E'),
        (6, 'F'),
        (7, 'G'),
        (8, 'H'),
        (9, 'I'),
        (10, 'J')
) AS r(row_index, row_label)
CROSS JOIN generate_series(1, 14) AS s(seat_no)
WHERE NOT EXISTS (
    SELECT 1
    FROM event_seats es
    WHERE es.event_id = '10000000-0000-0000-0000-000000000001'::uuid
      AND es.seat_code = r.row_label || s.seat_no
);

-- K row (12 couple seats)
INSERT INTO event_seats (
    id, event_id, section_id, row_label, seat_number, seat_code, price,
    status, locked_by, lock_expires_at, version, seat_type_code, layout_x, layout_y,
    pair_group, is_accessible, is_hidden, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001'::uuid,
    '22000000-0000-0000-0000-000000000003'::uuid,
    'K',
    s.seat_no,
    'K' || s.seat_no,
    250000,
    CASE
        WHEN s.seat_no IN (7, 8) THEN 'SOLD'::seat_status
        ELSE 'AVAILABLE'::seat_status
    END,
    NULL,
    NULL,
    0,
    'COUPLE',
    s.seat_no,
    11,
    'K-PAIR-' || ((s.seat_no + 1) / 2),
    false,
    false,
    now(),
    now()
FROM generate_series(1, 12) AS s(seat_no)
WHERE NOT EXISTS (
    SELECT 1
    FROM event_seats es
    WHERE es.event_id = '10000000-0000-0000-0000-000000000001'::uuid
      AND es.seat_code = 'K' || s.seat_no
);

-- Explicitly unhide all new seats of the fresh map.
UPDATE event_seats
SET is_hidden = false
WHERE event_id = '10000000-0000-0000-0000-000000000001'::uuid
  AND seat_code ~ '^[A-K][0-9]{1,2}$';

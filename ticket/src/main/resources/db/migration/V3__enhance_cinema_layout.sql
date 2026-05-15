-- V3__enhance_cinema_layout.sql
-- Enrich cinema/theater layout metadata so Booking UI can render a realistic seat map
-- with explicit seat types (VIP, couple, wheelchair), colors, hall info, and rich coordinates.

-- =========================================================
-- AUTH / PROVIDER VERIFICATION EXTENSIONS
-- =========================================================

ALTER TABLE users
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS primary_organization_id UUID,
ADD COLUMN IF NOT EXISTS provider_request_status VARCHAR(30),
ADD COLUMN IF NOT EXISTS provider_requested_at TIMESTAMPTZ;

UPDATE users
SET email_verified = true
WHERE email_verified = false;

CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    business_email VARCHAR(255) NOT NULL UNIQUE,
    owner_id UUID NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_organizations_owner
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT ck_organizations_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_organizations_business_email_not_blank CHECK (length(trim(business_email)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_organizations_owner_id ON organizations(owner_id);
CREATE INDEX IF NOT EXISTS idx_organizations_business_email ON organizations(business_email);

CREATE TRIGGER trg_organizations_set_updated_at
BEFORE UPDATE ON organizations
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

ALTER TABLE users
ADD CONSTRAINT fk_users_primary_organization
FOREIGN KEY (primary_organization_id) REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_users_primary_organization_id ON users(primary_organization_id);
CREATE INDEX IF NOT EXISTS idx_users_provider_request_status ON users(provider_request_status);

ALTER TABLE events
ADD COLUMN IF NOT EXISTS organization_id UUID;

ALTER TABLE events
ADD CONSTRAINT fk_events_organization
FOREIGN KEY (organization_id) REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_events_organization_id ON events(organization_id);

-- =========================================================
-- SEAT TYPES (UI color + pricing semantics)
-- =========================================================

CREATE TABLE seat_types (
    code VARCHAR(30) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    color_hex VARCHAR(7) NOT NULL,
    price_multiplier NUMERIC(6,3) NOT NULL DEFAULT 1,
    description TEXT,
    is_couple BOOLEAN NOT NULL DEFAULT false,
    is_accessible BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_seat_types_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_seat_types_name_not_blank CHECK (length(trim(display_name)) > 0),
    CONSTRAINT ck_seat_types_color_hex CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_seat_types_price_multiplier_positive CHECK (price_multiplier > 0)
);

INSERT INTO seat_types (code, display_name, color_hex, price_multiplier, description, is_couple, is_accessible)
VALUES
    ('STANDARD', 'Standard Seat', '#94A3B8', 1.000, 'Regular cinema seat', false, false),
    ('VIP', 'VIP Seat', '#FB923C', 1.350, 'Premium sightline and spacing', false, false),
    ('COUPLE', 'Couple Seat', '#F43F5E', 1.700, 'Double seat for pairs', true, false),
    ('SWEETBOX', 'Sweetbox Seat', '#A855F7', 1.900, 'Luxury sofa-style pair seat', true, false),
    ('WHEELCHAIR', 'Accessible Seat', '#10B981', 0.950, 'Accessible seating area', false, true);

-- =========================================================
-- VENUE / HALL (realistic theater structure)
-- =========================================================

CREATE TABLE cinema_venues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address TEXT,
    timezone VARCHAR(60) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_cinema_venues_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_cinema_venues_city_not_blank CHECK (length(trim(city)) > 0),
    CONSTRAINT ck_cinema_venues_timezone_not_blank CHECK (length(trim(timezone)) > 0)
);

CREATE INDEX idx_cinema_venues_city ON cinema_venues(city);

CREATE TRIGGER trg_cinema_venues_set_updated_at
BEFORE UPDATE ON cinema_venues
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE cinema_halls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    venue_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(140) NOT NULL UNIQUE,

    screen_type VARCHAR(50) NOT NULL DEFAULT '2D',
    total_rows INT,
    seats_per_row INT,
    capacity INT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_cinema_halls_venue
        FOREIGN KEY (venue_id) REFERENCES cinema_venues(id) ON DELETE CASCADE,

    CONSTRAINT uq_cinema_halls_venue_name UNIQUE (venue_id, name),

    CONSTRAINT ck_cinema_halls_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_cinema_halls_slug_not_blank CHECK (length(trim(slug)) > 0),
    CONSTRAINT ck_cinema_halls_total_rows_positive CHECK (total_rows IS NULL OR total_rows > 0),
    CONSTRAINT ck_cinema_halls_seats_per_row_positive CHECK (seats_per_row IS NULL OR seats_per_row > 0),
    CONSTRAINT ck_cinema_halls_capacity_positive CHECK (capacity IS NULL OR capacity > 0)
);

CREATE INDEX idx_cinema_halls_venue_id ON cinema_halls(venue_id);
CREATE INDEX idx_cinema_halls_screen_type ON cinema_halls(screen_type);

CREATE TRIGGER trg_cinema_halls_set_updated_at
BEFORE UPDATE ON cinema_halls
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

ALTER TABLE events
ADD COLUMN hall_id UUID;

ALTER TABLE events
ADD CONSTRAINT fk_events_hall
FOREIGN KEY (hall_id) REFERENCES cinema_halls(id);

CREATE INDEX idx_events_hall_id ON events(hall_id);

-- =========================================================
-- EXTEND SECTION / SEAT METADATA FOR RICH UI
-- =========================================================

ALTER TABLE event_sections
ADD COLUMN seat_type_code VARCHAR(30),
ADD COLUMN visual_color_hex VARCHAR(7),
ADD COLUMN note TEXT;

ALTER TABLE event_sections
ADD CONSTRAINT fk_event_sections_seat_type
FOREIGN KEY (seat_type_code) REFERENCES seat_types(code);

ALTER TABLE event_sections
ADD CONSTRAINT ck_event_sections_visual_color_hex
CHECK (visual_color_hex IS NULL OR visual_color_hex ~ '^#[0-9A-Fa-f]{6}$');

CREATE INDEX idx_event_sections_seat_type_code ON event_sections(seat_type_code);

ALTER TABLE event_seats
ADD COLUMN seat_type_code VARCHAR(30),
ADD COLUMN layout_x INT,
ADD COLUMN layout_y INT,
ADD COLUMN pair_group VARCHAR(30),
ADD COLUMN is_accessible BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE event_seats
ADD CONSTRAINT fk_event_seats_seat_type
FOREIGN KEY (seat_type_code) REFERENCES seat_types(code);

ALTER TABLE event_seats
ADD CONSTRAINT ck_event_seats_layout_x_positive CHECK (layout_x IS NULL OR layout_x > 0),
ADD CONSTRAINT ck_event_seats_layout_y_positive CHECK (layout_y IS NULL OR layout_y > 0);

CREATE INDEX idx_event_seats_seat_type_code ON event_seats(seat_type_code);
CREATE INDEX idx_event_seats_event_layout ON event_seats(event_id, layout_y, layout_x);

-- =========================================================
-- SEED VENUES / HALLS
-- =========================================================

INSERT INTO cinema_venues (id, name, city, address, timezone)
VALUES
    ('90000000-0000-0000-0000-000000000001', 'TicketRush Hanoi Center', 'Hà Nội', '72 Tràng Tiền, Hoàn Kiếm', 'Asia/Ho_Chi_Minh'),
    ('90000000-0000-0000-0000-000000000002', 'TicketRush Saigon Hub', 'TP Hồ Chí Minh', '2 Nguyễn Huệ, Quận 1', 'Asia/Ho_Chi_Minh');

INSERT INTO cinema_halls (id, venue_id, name, slug, screen_type, total_rows, seats_per_row, capacity)
VALUES
    ('91000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', 'Hall A', 'hall-a', '2D', 14, 16, 192),
    ('91000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000001', 'Hall Premium', 'hall-premium', '2D', 12, 14, 160),
    ('91000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000002', 'Hall B', 'hall-b', '2D', 13, 15, 180);

UPDATE events
SET hall_id = CASE
    WHEN city = 'Hà Nội' THEN '91000000-0000-0000-0000-000000000001'::uuid
    WHEN city = 'TP Hồ Chí Minh' THEN '91000000-0000-0000-0000-000000000003'::uuid
    ELSE hall_id
END
WHERE hall_id IS NULL;

-- =========================================================
-- BACKFILL EXISTING SECTIONS / SEATS
-- =========================================================

UPDATE event_sections
SET seat_type_code = CASE
    WHEN lower(name) LIKE '%vip%' THEN 'VIP'
    WHEN lower(name) LIKE '%couple%' OR lower(name) LIKE '%double%' THEN 'COUPLE'
    WHEN lower(name) LIKE '%wheel%' THEN 'WHEELCHAIR'
    ELSE 'STANDARD'
END
WHERE seat_type_code IS NULL;

UPDATE event_sections es
SET visual_color_hex = st.color_hex
FROM seat_types st
WHERE es.seat_type_code = st.code
  AND es.visual_color_hex IS NULL;

UPDATE event_seats es
SET
    seat_type_code = COALESCE(es.seat_type_code, sec.seat_type_code),
    layout_x = COALESCE(es.layout_x, es.seat_number),
    layout_y = COALESCE(es.layout_y, ascii(upper(substr(es.row_label, 1, 1))) - ascii('A') + 1),
    is_accessible = COALESCE(es.is_accessible, false),
    is_hidden = COALESCE(es.is_hidden, false)
FROM event_sections sec
WHERE sec.id = es.section_id;

-- =========================================================
-- RICHER CINEMA LAYOUT DATA FOR DEMO BOOKING (EVENT 1000...0001)
-- =========================================================

INSERT INTO event_sections (
    id, event_id, name, base_price, row_count, seats_per_row, display_order,
    seat_type_code, visual_color_hex, note, created_at, updated_at
)
VALUES
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', 'STANDARD-C', 350000, 8, 14, 3, 'STANDARD', '#94A3B8', 'Main center block', now(), now()),
    ('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'COUPLE', 800000, 3, 8, 4, 'COUPLE', '#F43F5E', 'Couple seats at rear rows', now(), now()),
    ('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'WHEELCHAIR', 300000, 1, 6, 5, 'WHEELCHAIR', '#10B981', 'Accessible row near aisle', now(), now());

-- STANDARD-C rows C..J, 14 seats each
INSERT INTO event_seats (
    id, event_id, section_id, row_label, seat_number, seat_code, price,
    status, locked_by, lock_expires_at, version, seat_type_code, layout_x, layout_y,
    pair_group, is_accessible, is_hidden, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001'::uuid,
    '20000000-0000-0000-0000-000000000010'::uuid,
    r.row_label,
    s.seat_no,
    'STC-' || r.row_label || s.seat_no,
    350000,
    CASE
        WHEN s.seat_no IN (1, 14) THEN 'SOLD'::seat_status
        WHEN r.row_idx IN (3, 4) AND s.seat_no IN (7, 8) THEN 'LOCKED'::seat_status
        ELSE 'AVAILABLE'::seat_status
    END,
    CASE
        WHEN r.row_idx IN (3, 4) AND s.seat_no IN (7, 8)
            THEN '00000000-0000-0000-0000-000000000004'::uuid
        ELSE NULL
    END,
    CASE
        WHEN r.row_idx IN (3, 4) AND s.seat_no IN (7, 8)
            THEN now() + interval '30 minutes'
        ELSE NULL
    END,
    0,
    'STANDARD',
    s.seat_no,
    2 + r.row_idx,
    NULL,
    false,
    false,
    now(),
    now()
FROM (
    VALUES
        (1, 'C'),
        (2, 'D'),
        (3, 'E'),
        (4, 'F'),
        (5, 'G'),
        (6, 'H'),
        (7, 'I'),
        (8, 'J')
) AS r(row_idx, row_label)
CROSS JOIN generate_series(1, 14) AS s(seat_no);

-- COUPLE rows K..M, 8 seats each
INSERT INTO event_seats (
    id, event_id, section_id, row_label, seat_number, seat_code, price,
    status, locked_by, lock_expires_at, version, seat_type_code, layout_x, layout_y,
    pair_group, is_accessible, is_hidden, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001'::uuid,
    '20000000-0000-0000-0000-000000000011'::uuid,
    r.row_label,
    s.seat_no,
    'CP-' || r.row_label || s.seat_no,
    800000,
    CASE
        WHEN r.row_idx = 2 AND s.seat_no IN (1, 2) THEN 'SOLD'::seat_status
        ELSE 'AVAILABLE'::seat_status
    END,
    NULL,
    NULL,
    0,
    'COUPLE',
    s.seat_no,
    11 + r.row_idx,
    'PAIR-' || r.row_label || '-' || ((s.seat_no + 1) / 2),
    false,
    false,
    now(),
    now()
FROM (
    VALUES
        (1, 'K'),
        (2, 'L'),
        (3, 'M')
) AS r(row_idx, row_label)
CROSS JOIN generate_series(1, 8) AS s(seat_no);

-- WHEELCHAIR row N, 6 seats
INSERT INTO event_seats (
    id, event_id, section_id, row_label, seat_number, seat_code, price,
    status, locked_by, lock_expires_at, version, seat_type_code, layout_x, layout_y,
    pair_group, is_accessible, is_hidden, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    '10000000-0000-0000-0000-000000000001'::uuid,
    '20000000-0000-0000-0000-000000000012'::uuid,
    'N',
    s.seat_no,
    'WH-N' || s.seat_no,
    300000,
    'AVAILABLE'::seat_status,
    NULL,
    NULL,
    0,
    'WHEELCHAIR',
    s.seat_no,
    15,
    NULL,
    true,
    false,
    now(),
    now()
FROM generate_series(1, 6) AS s(seat_no);

-- Mark a few edge seats as hidden to mimic aisle/camera blind spots in real screens.
UPDATE event_seats
SET is_hidden = true
WHERE event_id = '10000000-0000-0000-0000-000000000001'::uuid
  AND seat_code IN ('STC-C1', 'STC-C14', 'STC-J1', 'STC-J14');

COMMENT ON TABLE seat_types IS 'Seat type catalog for frontend UI color legend and pricing semantics.';
COMMENT ON TABLE cinema_venues IS 'Cinema location / branch.';
COMMENT ON TABLE cinema_halls IS 'Hall/screen inside a cinema venue.';
COMMENT ON COLUMN event_sections.seat_type_code IS 'Default seat type for a section (STANDARD/VIP/COUPLE/...).';
COMMENT ON COLUMN event_sections.visual_color_hex IS 'Hex color used by UI for section legend and seat color.';
COMMENT ON COLUMN event_seats.layout_x IS 'Seat coordinate X for rendering irregular seat map layouts.';
COMMENT ON COLUMN event_seats.layout_y IS 'Seat coordinate Y for rendering irregular seat map layouts.';
COMMENT ON COLUMN event_seats.pair_group IS 'Group id for couple/sweetbox seats (2 seats per pair).';
COMMENT ON COLUMN event_seats.is_accessible IS 'Accessible seat flag for wheelchair and priority seating.';
COMMENT ON COLUMN event_seats.is_hidden IS 'Hide seat in UI while preserving seat geometry alignment.';

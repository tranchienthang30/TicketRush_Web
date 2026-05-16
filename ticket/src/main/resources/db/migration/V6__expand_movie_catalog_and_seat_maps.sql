-- V6__expand_movie_catalog_and_seat_maps.sql
-- Add a fuller movie catalogue and cinema-style seat maps.
-- Standard rooms use a large A-K map; only luxury rooms intentionally have fewer expensive seats.

INSERT INTO cinema_halls (id, venue_id, name, slug, screen_type, total_rows, seats_per_row, capacity)
VALUES
    ('91000000-0000-0000-0000-000000000004', '90000000-0000-0000-0000-000000000001', 'Grand Screen 1', 'grand-screen-1', '2D', 11, 14, 152),
    ('91000000-0000-0000-0000-000000000005', '90000000-0000-0000-0000-000000000001', 'IMAX Aurora', 'imax-aurora', 'IMAX', 11, 14, 152),
    ('91000000-0000-0000-0000-000000000006', '90000000-0000-0000-0000-000000000002', 'Saigon Screen C', 'saigon-screen-c', '2D', 11, 14, 152),
    ('91000000-0000-0000-0000-000000000007', '90000000-0000-0000-0000-000000000001', 'Luxury Suite 1', 'luxury-suite-1', 'GOLD_CLASS', 5, 8, 36),
    ('91000000-0000-0000-0000-000000000008', '90000000-0000-0000-0000-000000000002', 'Gold Class Lounge', 'gold-class-lounge', 'GOLD_CLASS', 5, 8, 36)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    screen_type = EXCLUDED.screen_type,
    total_rows = EXCLUDED.total_rows,
    seats_per_row = EXCLUDED.seats_per_row,
    capacity = EXCLUDED.capacity,
    updated_at = now();

WITH movie_data AS (
    SELECT *
    FROM (
        VALUES
            (4, '10000000-0000-0000-0000-000000000004'::uuid, 5::bigint, 'The Last Letter', 'the-last-letter', 'A quiet drama about family, memory, and one letter that changes everything.', 'https://images.unsplash.com/photo-1523207911345-32501502db22?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 25, 'PUBLISHED', '91000000-0000-0000-0000-000000000007'::uuid, 'LUXURY'),
            (5, '10000000-0000-0000-0000-000000000005'::uuid, 6::bigint, 'Love in Saigon', 'love-in-saigon', 'A warm romantic film set across neon streets, coffee shops, and rainy nights.', 'https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 35, 'PUBLISHED', '91000000-0000-0000-0000-000000000006'::uuid, 'STANDARD'),
            (10, '10000000-0000-0000-0000-000000000010'::uuid, 2::bigint, 'Starship Tomorrow', 'starship-tomorrow', 'A hopeful sci-fi journey through a wormhole beyond the solar frontier.', 'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 8, 'PUBLISHED', '91000000-0000-0000-0000-000000000005'::uuid, 'STANDARD'),
            (11, '10000000-0000-0000-0000-000000000011'::uuid, 5::bigint, 'Detective Rain', 'detective-rain', 'A rainy-night mystery following a detective through old Hanoi alleys.', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 9, 'PUBLISHED', '91000000-0000-0000-0000-000000000004'::uuid, 'STANDARD'),
            (12, '10000000-0000-0000-0000-000000000012'::uuid, 8::bigint, 'Laughing Saigon', 'laughing-saigon', 'A fast, bright comedy about three friends opening a chaotic noodle shop.', 'https://images.unsplash.com/photo-1521967906867-14ec9d64bee8?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 10, 'PUBLISHED', '91000000-0000-0000-0000-000000000006'::uuid, 'STANDARD'),
            (13, '10000000-0000-0000-0000-000000000013'::uuid, 6::bigint, 'Ocean of Memories', 'ocean-of-memories', 'A gentle seaside romance about old promises and second chances.', 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 11, 'PUBLISHED', '91000000-0000-0000-0000-000000000006'::uuid, 'STANDARD'),
            (14, '10000000-0000-0000-0000-000000000014'::uuid, 7::bigint, 'Planet Blue', 'planet-blue', 'A documentary following coral reefs, night tides, and coastal communities.', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 12, 'PUBLISHED', '91000000-0000-0000-0000-000000000004'::uuid, 'STANDARD'),
            (15, '10000000-0000-0000-0000-000000000015'::uuid, 3::bigint, 'Dragon School', 'dragon-school', 'A magical animation about a tiny dragon learning to fly before sunrise.', 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 13, 'PUBLISHED', '91000000-0000-0000-0000-000000000002'::uuid, 'STANDARD'),
            (16, '10000000-0000-0000-0000-000000000016'::uuid, 4::bigint, 'Night Corridor', 'night-corridor', 'A horror film set inside a cinema corridor that keeps getting longer.', 'https://images.unsplash.com/photo-1509248961158-e54f6934749c?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 14, 'PUBLISHED', '91000000-0000-0000-0000-000000000001'::uuid, 'STANDARD'),
            (17, '10000000-0000-0000-0000-000000000017'::uuid, 1::bigint, 'Fast Lane 2088', 'fast-lane-2088', 'A neon racing action movie with impossible cars and a citywide chase.', 'https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 16, 'PUBLISHED', '91000000-0000-0000-0000-000000000006'::uuid, 'STANDARD'),
            (18, '10000000-0000-0000-0000-000000000018'::uuid, 2::bigint, 'Parallel Earth', 'parallel-earth', 'A scientist wakes up in a second Earth where every choice has changed.', 'https://images.unsplash.com/photo-1454789548928-9efd52dc4031?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 17, 'PUBLISHED', '91000000-0000-0000-0000-000000000005'::uuid, 'STANDARD'),
            (19, '10000000-0000-0000-0000-000000000019'::uuid, 8::bigint, 'Golden Kitchen', 'golden-kitchen', 'A feel-good comedy about a family restaurant racing against opening night.', 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 18, 'PUBLISHED', '91000000-0000-0000-0000-000000000006'::uuid, 'STANDARD'),
            (20, '10000000-0000-0000-0000-000000000020'::uuid, 5::bigint, 'The Silent Bridge', 'the-silent-bridge', 'A premium drama screening with a small luxury lounge and quiet atmosphere.', 'https://images.unsplash.com/photo-1495567720989-cebdbdd97913?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 19, 'PUBLISHED', '91000000-0000-0000-0000-000000000007'::uuid, 'LUXURY'),
            (21, '10000000-0000-0000-0000-000000000021'::uuid, 7::bigint, 'Wild Mekong', 'wild-mekong', 'A sweeping documentary about rivers, markets, forests, and floating villages.', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 21, 'PUBLISHED', '91000000-0000-0000-0000-000000000003'::uuid, 'STANDARD'),
            (22, '10000000-0000-0000-0000-000000000022'::uuid, 3::bigint, 'Robot Cat Holiday', 'robot-cat-holiday', 'A cheerful animation about a robot cat who accidentally saves summer vacation.', 'https://images.unsplash.com/photo-1518709268805-4e9042af2176?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 22, 'PUBLISHED', '91000000-0000-0000-0000-000000000004'::uuid, 'STANDARD'),
            (23, '10000000-0000-0000-0000-000000000023'::uuid, 4::bigint, 'After Midnight 2', 'after-midnight-2', 'The midnight horror hit returns with a louder secret and a darker room.', 'https://images.unsplash.com/photo-1516410529446-2c777cb7366d?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 23, 'PUBLISHED', '91000000-0000-0000-0000-000000000006'::uuid, 'STANDARD'),
            (24, '10000000-0000-0000-0000-000000000024'::uuid, 6::bigint, 'Love on Platform 9', 'love-on-platform-9', 'A charming romance about missed trains, handwritten notes, and perfect timing.', 'https://images.unsplash.com/photo-1497032628192-86f99bcd76bc?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 24, 'PUBLISHED', '91000000-0000-0000-0000-000000000004'::uuid, 'STANDARD'),
            (25, '10000000-0000-0000-0000-000000000025'::uuid, 1::bigint, 'Skyfall District', 'skyfall-district', 'A city action thriller about a rescue team racing across rooftops.', 'https://images.unsplash.com/photo-1518655048521-f130df041f66?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 26, 'PUBLISHED', '91000000-0000-0000-0000-000000000003'::uuid, 'STANDARD'),
            (26, '10000000-0000-0000-0000-000000000026'::uuid, 8::bigint, 'Tiny Theatre Club', 'tiny-theatre-club', 'A limited gold-class comedy screening in a tiny premium lounge.', 'https://images.unsplash.com/photo-1505236858219-8359eb29e329?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 27, 'PUBLISHED', '91000000-0000-0000-0000-000000000008'::uuid, 'LUXURY'),
            (27, '10000000-0000-0000-0000-000000000027'::uuid, 2::bigint, 'Deep Space Rescue', 'deep-space-rescue', 'A rescue mission crosses a collapsing nebula with minutes to spare.', 'https://images.unsplash.com/photo-1462331940025-496dfbfc7564?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 28, 'PUBLISHED', '91000000-0000-0000-0000-000000000005'::uuid, 'STANDARD'),
            (28, '10000000-0000-0000-0000-000000000028'::uuid, 5::bigint, 'River of Light', 'river-of-light', 'A family drama about returning home during lantern season.', 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 29, 'PUBLISHED', '91000000-0000-0000-0000-000000000006'::uuid, 'STANDARD'),
            (29, '10000000-0000-0000-0000-000000000029'::uuid, 3::bigint, 'Penguins in Hanoi', 'penguins-in-hanoi', 'A playful family animation about penguins taking a very wrong flight.', 'https://images.unsplash.com/photo-1551986782-d0169b3f8fa7?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi Center', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 30, 'PUBLISHED', '91000000-0000-0000-0000-000000000002'::uuid, 'STANDARD'),
            (30, '10000000-0000-0000-0000-000000000030'::uuid, 7::bigint, 'The Last Reef', 'the-last-reef', 'A documentary about marine scientists protecting one of the last living reefs.', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Saigon Hub', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 31, 'PUBLISHED', '91000000-0000-0000-0000-000000000003'::uuid, 'STANDARD')
    ) AS movies(event_no, id, category_id, title, slug, description, banner_url, location_name, address, city, days_from_now, status, hall_id, room_type)
)
INSERT INTO events (
    id, provider_id, category_id, title, slug, description, banner_url,
    location_name, address, city, start_time, end_time, sale_start_time, sale_end_time,
    status, hall_id, created_at, updated_at
)
SELECT
    id,
    '00000000-0000-0000-0000-000000000002'::uuid,
    category_id,
    title,
    slug,
    description,
    banner_url,
    location_name,
    address,
    city,
    now() + (days_from_now || ' days')::interval,
    now() + (days_from_now || ' days')::interval + interval '2 hours',
    now() - interval '5 days',
    now() + ((days_from_now - 1) || ' days')::interval,
    status::event_status,
    hall_id,
    now(),
    now()
FROM movie_data
ON CONFLICT (id) DO UPDATE
SET category_id = EXCLUDED.category_id,
    title = EXCLUDED.title,
    slug = EXCLUDED.slug,
    description = EXCLUDED.description,
    banner_url = EXCLUDED.banner_url,
    location_name = EXCLUDED.location_name,
    address = EXCLUDED.address,
    city = EXCLUDED.city,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    sale_start_time = EXCLUDED.sale_start_time,
    sale_end_time = EXCLUDED.sale_end_time,
    status = EXCLUDED.status,
    hall_id = EXCLUDED.hall_id,
    updated_at = now();

UPDATE event_seats
SET is_hidden = true
WHERE event_id IN (
    '10000000-0000-0000-0000-000000000002'::uuid,
    '10000000-0000-0000-0000-000000000003'::uuid
);

WITH map_events AS (
    SELECT event_no, event_id, room_type
    FROM (
        VALUES
            (2, '10000000-0000-0000-0000-000000000002'::uuid, 'STANDARD'),
            (3, '10000000-0000-0000-0000-000000000003'::uuid, 'STANDARD'),
            (4, '10000000-0000-0000-0000-000000000004'::uuid, 'LUXURY'),
            (5, '10000000-0000-0000-0000-000000000005'::uuid, 'STANDARD'),
            (10, '10000000-0000-0000-0000-000000000010'::uuid, 'STANDARD'),
            (11, '10000000-0000-0000-0000-000000000011'::uuid, 'STANDARD'),
            (12, '10000000-0000-0000-0000-000000000012'::uuid, 'STANDARD'),
            (13, '10000000-0000-0000-0000-000000000013'::uuid, 'STANDARD'),
            (14, '10000000-0000-0000-0000-000000000014'::uuid, 'STANDARD'),
            (15, '10000000-0000-0000-0000-000000000015'::uuid, 'STANDARD'),
            (16, '10000000-0000-0000-0000-000000000016'::uuid, 'STANDARD'),
            (17, '10000000-0000-0000-0000-000000000017'::uuid, 'STANDARD'),
            (18, '10000000-0000-0000-0000-000000000018'::uuid, 'STANDARD'),
            (19, '10000000-0000-0000-0000-000000000019'::uuid, 'STANDARD'),
            (20, '10000000-0000-0000-0000-000000000020'::uuid, 'LUXURY'),
            (21, '10000000-0000-0000-0000-000000000021'::uuid, 'STANDARD'),
            (22, '10000000-0000-0000-0000-000000000022'::uuid, 'STANDARD'),
            (23, '10000000-0000-0000-0000-000000000023'::uuid, 'STANDARD'),
            (24, '10000000-0000-0000-0000-000000000024'::uuid, 'STANDARD'),
            (25, '10000000-0000-0000-0000-000000000025'::uuid, 'STANDARD'),
            (26, '10000000-0000-0000-0000-000000000026'::uuid, 'LUXURY'),
            (27, '10000000-0000-0000-0000-000000000027'::uuid, 'STANDARD'),
            (28, '10000000-0000-0000-0000-000000000028'::uuid, 'STANDARD'),
            (29, '10000000-0000-0000-0000-000000000029'::uuid, 'STANDARD'),
            (30, '10000000-0000-0000-0000-000000000030'::uuid, 'STANDARD')
    ) AS event_maps(event_no, event_id, room_type)
),
normal_sections AS (
    SELECT
        event_no,
        event_id,
        section_kind,
        section_id,
        name,
        base_price,
        row_count,
        seats_per_row,
        display_order,
        seat_type_code,
        visual_color_hex,
        note
    FROM map_events
    CROSS JOIN LATERAL (
        VALUES
            (
                'STANDARD',
                ('23000000-0000-0000-0000-' || lpad(event_no::text, 12, '0'))::uuid,
                'STANDARD-SCREEN-' || event_no,
                120000 + (event_no % 3) * 10000,
                10,
                14,
                10,
                'STANDARD',
                '#E2E8F0',
                'Large cinema map, rows A-J around the premium block'
            ),
            (
                'VIP',
                ('23100000-0000-0000-0000-' || lpad(event_no::text, 12, '0'))::uuid,
                'VIP-SCREEN-' || event_no,
                170000 + (event_no % 3) * 10000,
                6,
                10,
                11,
                'VIP',
                '#FED7AA',
                'Premium center block from D to I'
            ),
            (
                'COUPLE',
                ('23200000-0000-0000-0000-' || lpad(event_no::text, 12, '0'))::uuid,
                'COUPLE-SCREEN-' || event_no,
                250000 + (event_no % 3) * 10000,
                1,
                12,
                12,
                'COUPLE',
                '#FFE4E6',
                'Rear couple row K'
            )
    ) AS section_data(section_kind, section_id, name, base_price, row_count, seats_per_row, display_order, seat_type_code, visual_color_hex, note)
    WHERE room_type = 'STANDARD'
),
luxury_sections AS (
    SELECT
        event_no,
        event_id,
        section_kind,
        section_id,
        name,
        base_price,
        row_count,
        seats_per_row,
        display_order,
        seat_type_code,
        visual_color_hex,
        note
    FROM map_events
    CROSS JOIN LATERAL (
        VALUES
            (
                'LUXURY',
                ('23300000-0000-0000-0000-' || lpad(event_no::text, 12, '0'))::uuid,
                'LUXURY-SUITE-' || event_no,
                450000 + (event_no % 3) * 50000,
                4,
                8,
                20,
                'VIP',
                '#FDBA74',
                'Small premium room with wider seats'
            ),
            (
                'SWEETBOX',
                ('23400000-0000-0000-0000-' || lpad(event_no::text, 12, '0'))::uuid,
                'SWEETBOX-SUITE-' || event_no,
                850000 + (event_no % 3) * 50000,
                1,
                4,
                21,
                'SWEETBOX',
                '#E9D5FF',
                'Very limited sofa seats for expensive rooms'
            )
    ) AS section_data(section_kind, section_id, name, base_price, row_count, seats_per_row, display_order, seat_type_code, visual_color_hex, note)
    WHERE room_type = 'LUXURY'
),
all_sections AS (
    SELECT * FROM normal_sections
    UNION ALL
    SELECT * FROM luxury_sections
)
INSERT INTO event_sections (
    id, event_id, name, base_price, row_count, seats_per_row, display_order,
    seat_type_code, visual_color_hex, note, created_at, updated_at
)
SELECT
    section_id,
    event_id,
    name,
    base_price,
    row_count,
    seats_per_row,
    display_order,
    seat_type_code,
    visual_color_hex,
    note,
    now(),
    now()
FROM all_sections
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    base_price = EXCLUDED.base_price,
    row_count = EXCLUDED.row_count,
    seats_per_row = EXCLUDED.seats_per_row,
    display_order = EXCLUDED.display_order,
    seat_type_code = EXCLUDED.seat_type_code,
    visual_color_hex = EXCLUDED.visual_color_hex,
    note = EXCLUDED.note,
    updated_at = now();

WITH normal_events AS (
    SELECT event_no, event_id
    FROM (
        VALUES
            (2, '10000000-0000-0000-0000-000000000002'::uuid),
            (3, '10000000-0000-0000-0000-000000000003'::uuid),
            (5, '10000000-0000-0000-0000-000000000005'::uuid),
            (10, '10000000-0000-0000-0000-000000000010'::uuid),
            (11, '10000000-0000-0000-0000-000000000011'::uuid),
            (12, '10000000-0000-0000-0000-000000000012'::uuid),
            (13, '10000000-0000-0000-0000-000000000013'::uuid),
            (14, '10000000-0000-0000-0000-000000000014'::uuid),
            (15, '10000000-0000-0000-0000-000000000015'::uuid),
            (16, '10000000-0000-0000-0000-000000000016'::uuid),
            (17, '10000000-0000-0000-0000-000000000017'::uuid),
            (18, '10000000-0000-0000-0000-000000000018'::uuid),
            (19, '10000000-0000-0000-0000-000000000019'::uuid),
            (21, '10000000-0000-0000-0000-000000000021'::uuid),
            (22, '10000000-0000-0000-0000-000000000022'::uuid),
            (23, '10000000-0000-0000-0000-000000000023'::uuid),
            (24, '10000000-0000-0000-0000-000000000024'::uuid),
            (25, '10000000-0000-0000-0000-000000000025'::uuid),
            (27, '10000000-0000-0000-0000-000000000027'::uuid),
            (28, '10000000-0000-0000-0000-000000000028'::uuid),
            (29, '10000000-0000-0000-0000-000000000029'::uuid),
            (30, '10000000-0000-0000-0000-000000000030'::uuid)
    ) AS event_maps(event_no, event_id)
),
rows AS (
    SELECT *
    FROM (
        VALUES
            ('A', 1), ('B', 2), ('C', 3), ('D', 4), ('E', 5), ('F', 6),
            ('G', 7), ('H', 8), ('I', 9), ('J', 10), ('K', 11)
    ) AS row_data(row_label, row_index)
),
seat_positions AS (
    SELECT
        row_label,
        row_index,
        generate_series(1, CASE WHEN row_label = 'K' THEN 12 ELSE 14 END) AS seat_no
    FROM rows
),
normal_seats AS (
    SELECT
        ne.event_no,
        ne.event_id,
        sp.row_label,
        sp.row_index,
        sp.seat_no,
        CASE
            WHEN sp.row_label = 'K' THEN ('23200000-0000-0000-0000-' || lpad(ne.event_no::text, 12, '0'))::uuid
            WHEN sp.row_label IN ('D', 'E', 'F', 'G', 'H', 'I') AND sp.seat_no BETWEEN 3 AND 12 THEN ('23100000-0000-0000-0000-' || lpad(ne.event_no::text, 12, '0'))::uuid
            ELSE ('23000000-0000-0000-0000-' || lpad(ne.event_no::text, 12, '0'))::uuid
        END AS section_id,
        CASE
            WHEN sp.row_label = 'K' THEN 250000 + (ne.event_no % 3) * 10000
            WHEN sp.row_label IN ('D', 'E', 'F', 'G', 'H', 'I') AND sp.seat_no BETWEEN 3 AND 12 THEN 170000 + (ne.event_no % 3) * 10000
            ELSE 120000 + (ne.event_no % 3) * 10000
        END AS price,
        CASE
            WHEN sp.row_label = 'K' THEN 'COUPLE'
            WHEN sp.row_label IN ('D', 'E', 'F', 'G', 'H', 'I') AND sp.seat_no BETWEEN 3 AND 12 THEN 'VIP'
            ELSE 'STANDARD'
        END AS seat_type_code,
        CASE
            WHEN sp.row_label = 'H' AND sp.seat_no BETWEEN 7 AND 10 THEN 'SOLD'::seat_status
            WHEN sp.row_label = 'K' AND sp.seat_no IN (7, 8) THEN 'SOLD'::seat_status
            WHEN sp.row_label = 'D' AND sp.seat_no IN (3, 4) AND ne.event_no % 4 = 0 THEN 'SOLD'::seat_status
            ELSE 'AVAILABLE'::seat_status
        END AS status
    FROM normal_events ne
    CROSS JOIN seat_positions sp
)
INSERT INTO event_seats (
    id, event_id, section_id, row_label, seat_number, seat_code, price,
    status, locked_by, lock_expires_at, version, seat_type_code, layout_x, layout_y,
    pair_group, is_accessible, is_hidden, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    event_id,
    section_id,
    row_label,
    seat_no,
    row_label || seat_no,
    price,
    status,
    NULL,
    NULL,
    0,
    seat_type_code,
    seat_no,
    row_index,
    CASE WHEN row_label = 'K' THEN row_label || '-' || ceil(seat_no / 2.0)::int ELSE NULL END,
    false,
    false,
    now(),
    now()
FROM normal_seats
ON CONFLICT (event_id, seat_code) DO NOTHING;

WITH luxury_events AS (
    SELECT event_no, event_id
    FROM (
        VALUES
            (4, '10000000-0000-0000-0000-000000000004'::uuid),
            (20, '10000000-0000-0000-0000-000000000020'::uuid),
            (26, '10000000-0000-0000-0000-000000000026'::uuid)
    ) AS event_maps(event_no, event_id)
),
luxury_rows AS (
    SELECT *
    FROM (
        VALUES
            ('A', 1, 8), ('B', 2, 8), ('C', 3, 8), ('D', 4, 8), ('E', 5, 4)
    ) AS row_data(row_label, row_index, seats_in_row)
),
luxury_positions AS (
    SELECT row_label, row_index, generate_series(1, seats_in_row) AS seat_no
    FROM luxury_rows
),
luxury_seats AS (
    SELECT
        le.event_no,
        le.event_id,
        lp.row_label,
        lp.row_index,
        lp.seat_no,
        CASE
            WHEN lp.row_label = 'E' THEN ('23400000-0000-0000-0000-' || lpad(le.event_no::text, 12, '0'))::uuid
            ELSE ('23300000-0000-0000-0000-' || lpad(le.event_no::text, 12, '0'))::uuid
        END AS section_id,
        CASE
            WHEN lp.row_label = 'E' THEN 850000 + (le.event_no % 3) * 50000
            ELSE 450000 + (le.event_no % 3) * 50000
        END AS price,
        CASE WHEN lp.row_label = 'E' THEN 'SWEETBOX' ELSE 'VIP' END AS seat_type_code,
        CASE
            WHEN lp.row_label = 'B' AND lp.seat_no IN (4, 5) THEN 'SOLD'::seat_status
            ELSE 'AVAILABLE'::seat_status
        END AS status
    FROM luxury_events le
    CROSS JOIN luxury_positions lp
)
INSERT INTO event_seats (
    id, event_id, section_id, row_label, seat_number, seat_code, price,
    status, locked_by, lock_expires_at, version, seat_type_code, layout_x, layout_y,
    pair_group, is_accessible, is_hidden, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    event_id,
    section_id,
    row_label,
    seat_no,
    row_label || seat_no,
    price,
    status,
    NULL,
    NULL,
    0,
    seat_type_code,
    seat_no,
    row_index,
    CASE WHEN row_label = 'E' THEN row_label || '-' || ceil(seat_no / 2.0)::int ELSE NULL END,
    false,
    false,
    now(),
    now()
FROM luxury_seats
ON CONFLICT (event_id, seat_code) DO NOTHING;

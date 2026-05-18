-- V16__restore_multi_event_catalog.sql
-- Rebrand the demo catalogue from movie-only back to multi-event ticketing.

UPDATE categories
SET name = category_data.name,
    slug = category_data.slug,
    description = category_data.description,
    image_url = category_data.image_url,
    is_active = true
FROM (
    VALUES
        (1::bigint, 'Music', 'music', 'Live music nights, DJ sets, and acoustic sessions.', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=900&q=80'),
        (2::bigint, 'Show', 'show', 'Comedy, magic, variety, and limited stage shows.', 'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=900&q=80'),
        (3::bigint, 'Concert', 'concert', 'Large concerts, arena tours, and premium live performances.', 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=900&q=80'),
        (4::bigint, 'Cinema', 'cinema', 'Movie screenings and cinema ticket bookings.', 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=900&q=80'),
        (5::bigint, 'Sport', 'sport', 'Sports matches, tournaments, and esports finals.', 'https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=900&q=80'),
        (6::bigint, 'Festival', 'festival', 'Food, art, outdoor, and community festivals.', 'https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?auto=format&fit=crop&w=900&q=80'),
        (7::bigint, 'Theater', 'theater', 'Stage plays, musicals, and live theater.', 'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=900&q=80'),
        (8::bigint, 'Workshop', 'workshop', 'Workshops, talks, classes, and professional events.', 'https://images.unsplash.com/photo-1515169067868-5387ec356754?auto=format&fit=crop&w=900&q=80')
) AS category_data(id, name, slug, description, image_url)
WHERE categories.id = category_data.id;

UPDATE cinema_halls
SET name = hall_data.name,
    slug = hall_data.slug,
    screen_type = hall_data.venue_type,
    updated_at = now()
FROM (
    VALUES
        ('91000000-0000-0000-0000-000000000001'::uuid, 'Main Hall', 'main-hall', 'SEATED'),
        ('91000000-0000-0000-0000-000000000002'::uuid, 'Premium Hall', 'premium-hall', 'SEATED'),
        ('91000000-0000-0000-0000-000000000003'::uuid, 'Arena Floor', 'arena-floor', 'ARENA'),
        ('91000000-0000-0000-0000-000000000004'::uuid, 'Grand Stage', 'grand-stage', 'SEATED'),
        ('91000000-0000-0000-0000-000000000005'::uuid, 'Concert Arena', 'concert-arena', 'ARENA'),
        ('91000000-0000-0000-0000-000000000006'::uuid, 'Saigon Stage', 'saigon-stage', 'SEATED'),
        ('91000000-0000-0000-0000-000000000007'::uuid, 'VIP Lounge', 'vip-lounge', 'VIP'),
        ('91000000-0000-0000-0000-000000000008'::uuid, 'Gold Lounge', 'gold-lounge', 'VIP')
) AS hall_data(id, name, slug, venue_type)
WHERE cinema_halls.id = hall_data.id;

UPDATE membership_plans
SET description = CASE id
    WHEN 1 THEN 'Basic perks for casual ticket buyers'
    WHEN 2 THEN 'Popular plan with better discounts and early event access'
    WHEN 3 THEN 'Premium annual plan for frequent eventgoers'
    ELSE description
END
WHERE id IN (1, 2, 3);

UPDATE vouchers
SET code = 'EVENT15',
    description = '15% off selected event tickets'
WHERE id = '40000000-0000-0000-0000-000000000004'::uuid
   OR code = 'MOVIE15';

WITH event_data AS (
    SELECT *
    FROM (
        VALUES
            ('10000000-0000-0000-0000-000000000001'::uuid, 1::bigint, 'Indie Night Live', 'indie-night-live', 'A live indie music night with local bands and a late acoustic set.', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=1200&q=80', 'Starlight Main Hall', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 150, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000002'::uuid, 4::bigint, 'Little Moon Adventure', 'little-moon-adventure', 'A colorful family cinema screening for weekend audiences.', 'https://images.unsplash.com/photo-1524985069026-dd778a71c7b4?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 94, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000003'::uuid, 7::bigint, 'Midnight Stage Mystery', 'midnight-stage-mystery', 'A suspense theater performance inside a dark old venue.', 'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1200&q=80', 'Grand Stage Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 111, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000004'::uuid, 2::bigint, 'Illusion Night VIP', 'illusion-night-vip', 'A premium magic and variety show with limited VIP seating.', 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=1200&q=80', 'VIP Lounge Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 117, 'SPECIAL'),
            ('10000000-0000-0000-0000-000000000005'::uuid, 5::bigint, 'Saigon Basketball Cup', 'saigon-basketball-cup', 'A high-energy local basketball cup with reserved arena seating.', 'https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=1200&q=80', 'Saigon Arena', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 120, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000010'::uuid, 3::bigint, 'Starship Pop Concert', 'starship-pop-concert', 'A futuristic pop concert with arena lights and live dance performances.', 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1200&q=80', 'Concert Arena Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 132, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000011'::uuid, 8::bigint, 'Creative Design Workshop', 'creative-design-workshop', 'A practical creative workshop for design, branding, and digital products.', 'https://images.unsplash.com/photo-1515169067868-5387ec356754?auto=format&fit=crop&w=1200&q=80', 'Innovation Hub Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 180, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000012'::uuid, 2::bigint, 'Laughing Saigon Comedy Show', 'laughing-saigon-comedy-show', 'A fast, bright stand-up comedy show with Saigon performers.', 'https://images.unsplash.com/photo-1521967906867-14ec9d64bee8?auto=format&fit=crop&w=1200&q=80', 'Saigon Stage', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 98, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000013'::uuid, 6::bigint, 'Ocean Food Festival', 'ocean-food-festival', 'A seaside-inspired food and music festival with all-day tickets.', 'https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?auto=format&fit=crop&w=1200&q=80', 'Saigon Festival Yard', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 360, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000014'::uuid, 8::bigint, 'Planet Blue Talk', 'planet-blue-talk', 'A documentary-style talk and workshop about oceans and sustainability.', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80', 'Innovation Hub Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 87, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000015'::uuid, 4::bigint, 'Dragon School Cinema Day', 'dragon-school-cinema-day', 'A family-friendly cinema ticket package for animation fans.', 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 92, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000016'::uuid, 7::bigint, 'Night Corridor Play', 'night-corridor-play', 'A horror-inspired live stage play with reserved seats.', 'https://images.unsplash.com/photo-1516410529446-2c777cb7366d?auto=format&fit=crop&w=1200&q=80', 'Main Hall Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 106, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000017'::uuid, 5::bigint, 'Fast Lane Esports Final', 'fast-lane-esports-final', 'A future-racing esports final with cheering sections and VIP rows.', 'https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80', 'Saigon Arena', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 124, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000018'::uuid, 3::bigint, 'Parallel Beats Concert', 'parallel-beats-concert', 'An upcoming electronic concert with synchronized light stages.', 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80', 'Concert Arena Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 118, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000019'::uuid, 6::bigint, 'Golden Kitchen Food Fair', 'golden-kitchen-food-fair', 'A weekend food fair with workshops, tasting booths, and live shows.', 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=1200&q=80', 'Saigon Festival Yard', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 300, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000020'::uuid, 2::bigint, 'The Silent Bridge VIP Show', 'the-silent-bridge-vip-show', 'A limited VIP stage show with a small premium lounge.', 'https://images.unsplash.com/photo-1495567720989-cebdbdd97913?auto=format&fit=crop&w=1200&q=80', 'VIP Lounge Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 115, 'SPECIAL'),
            ('10000000-0000-0000-0000-000000000021'::uuid, 5::bigint, 'Wild Mekong Marathon', 'wild-mekong-marathon', 'A city marathon spectator pass and fan-zone ticket.', 'https://images.unsplash.com/photo-1552674605-db6ffd4facb5?auto=format&fit=crop&w=1200&q=80', 'Saigon Arena', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 240, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000022'::uuid, 4::bigint, 'Robot Cat Holiday Cinema', 'robot-cat-holiday-cinema', 'An upcoming family cinema screening with reserved seating.', 'https://images.unsplash.com/photo-1518709268805-4e9042af2176?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 90, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000023'::uuid, 1::bigint, 'After Midnight DJ Set', 'after-midnight-dj-set', 'A special midnight DJ set with limited seats and lounge access.', 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=1200&q=80', 'Gold Lounge Saigon', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 112, 'SPECIAL'),
            ('10000000-0000-0000-0000-000000000024'::uuid, 7::bigint, 'Love on Platform 9 Musical', 'love-on-platform-9-musical', 'A romantic live musical coming soon to the theater stage.', 'https://images.unsplash.com/photo-1497032628192-86f99bcd76bc?auto=format&fit=crop&w=1200&q=80', 'Grand Stage Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 103, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000025'::uuid, 5::bigint, 'Skyfall District Bike Race', 'skyfall-district-bike-race', 'An upcoming urban bike race with grandstand and fan-zone tickets.', 'https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=1200&q=80', 'Saigon Arena', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 119, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000026'::uuid, 2::bigint, 'Tiny Theatre Club', 'tiny-theatre-club', 'A limited gold-class comedy and variety show in a tiny premium lounge.', 'https://images.unsplash.com/photo-1505236858219-8359eb29e329?auto=format&fit=crop&w=1200&q=80', 'Gold Lounge Saigon', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 97, 'SPECIAL'),
            ('10000000-0000-0000-0000-000000000027'::uuid, 3::bigint, 'Deep Space Arena Concert', 'deep-space-arena-concert', 'A sci-fi themed arena concert opening soon.', 'https://images.unsplash.com/photo-1462331940025-496dfbfc7564?auto=format&fit=crop&w=1200&q=80', 'Concert Arena Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 126, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000028'::uuid, 6::bigint, 'River of Light Festival', 'river-of-light-festival', 'A lantern festival with food stalls, live music, and night markets.', 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80', 'Saigon Festival Yard', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 300, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000029'::uuid, 4::bigint, 'Penguins in Hanoi Cinema', 'penguins-in-hanoi-cinema', 'An upcoming animation screening for families.', 'https://images.unsplash.com/photo-1551986782-d0169b3f8fa7?auto=format&fit=crop&w=1200&q=80', 'Starlight Cinema Hanoi', '72 Trang Tien, Hoan Kiem', 'Ha Noi', 91, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000030'::uuid, 8::bigint, 'The Last Reef Masterclass', 'the-last-reef-masterclass', 'A documentary masterclass and workshop with marine researchers.', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80', 'Innovation Hub Saigon', '2 Nguyen Hue, District 1', 'Ho Chi Minh City', 120, 'UPCOMING')
    ) AS events(id, category_id, title, slug, description, banner_url, location_name, address, city, duration_minutes, listing_type)
)
UPDATE events e
SET category_id = event_data.category_id,
    title = event_data.title,
    slug = event_data.slug,
    description = event_data.description,
    banner_url = event_data.banner_url,
    location_name = event_data.location_name,
    address = event_data.address,
    city = event_data.city,
    duration_minutes = event_data.duration_minutes,
    listing_type = event_data.listing_type,
    end_time = e.start_time + (event_data.duration_minutes || ' minutes')::interval,
    updated_at = now()
FROM event_data
WHERE e.id = event_data.id;

COMMENT ON COLUMN events.duration_minutes IS 'Event duration in minutes, distinct from ticket sale window.';
COMMENT ON COLUMN events.listing_type IS 'Public event listing bucket: NOW_SHOWING, UPCOMING, or SPECIAL.';

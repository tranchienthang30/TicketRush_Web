-- V5__rebrand_seed_to_movies.sql
-- Convert the local demo seed from generic events into a cinema/movie catalogue.
-- V2 is already part of Flyway history, so this migration updates the existing rows safely.

UPDATE users
SET full_name = CASE id
    WHEN '00000000-0000-0000-0000-000000000001'::uuid THEN 'Starlight Admin'
    WHEN '00000000-0000-0000-0000-000000000002'::uuid THEN 'Cinema Manager'
    ELSE full_name
END,
updated_at = now()
WHERE id IN (
    '00000000-0000-0000-0000-000000000001'::uuid,
    '00000000-0000-0000-0000-000000000002'::uuid
);

UPDATE categories
SET name = category_data.name,
    slug = category_data.slug,
    description = category_data.description,
    image_url = category_data.image_url,
    is_active = true
FROM (
    VALUES
        (1::bigint, 'Action', 'action', 'High-energy blockbusters and action cinema.', 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=900&q=80'),
        (2::bigint, 'Sci-Fi', 'sci-fi', 'Science fiction and futuristic adventures.', 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=900&q=80'),
        (3::bigint, 'Animation', 'animation', 'Animated movies for families and fans.', 'https://images.unsplash.com/photo-1524985069026-dd778a71c7b4?auto=format&fit=crop&w=900&q=80'),
        (4::bigint, 'Horror', 'horror', 'Thrillers, horror, and late-night screenings.', 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?auto=format&fit=crop&w=900&q=80'),
        (5::bigint, 'Drama', 'drama', 'Drama films and award-season stories.', 'https://images.unsplash.com/photo-1523207911345-32501502db22?auto=format&fit=crop&w=900&q=80'),
        (6::bigint, 'Romance', 'romance', 'Romantic films and date-night picks.', 'https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=900&q=80'),
        (7::bigint, 'Documentary', 'documentary', 'Documentaries and real stories on screen.', 'https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?auto=format&fit=crop&w=900&q=80'),
        (8::bigint, 'Comedy', 'comedy', 'Comedies and feel-good cinema.', 'https://images.unsplash.com/photo-1521967906867-14ec9d64bee8?auto=format&fit=crop&w=900&q=80')
) AS category_data(id, name, slug, description, image_url)
WHERE categories.id = category_data.id;

UPDATE membership_plans
SET description = CASE id
    WHEN 1 THEN 'Basic cinema perks for casual movie nights'
    WHEN 2 THEN 'Popular plan with better discounts and early screening access'
    WHEN 3 THEN 'Premium annual plan for frequent moviegoers'
    ELSE description
END
WHERE id IN (1, 2, 3);

UPDATE vouchers
SET code = 'MOVIE15',
    description = '15% off selected movie tickets'
WHERE id = '40000000-0000-0000-0000-000000000004'::uuid;

UPDATE vouchers
SET description = CASE id
    WHEN '40000000-0000-0000-0000-000000000001'::uuid THEN '20% off for new moviegoers'
    WHEN '40000000-0000-0000-0000-000000000002'::uuid THEN '10% off for active members'
    WHEN '40000000-0000-0000-0000-000000000003'::uuid THEN 'Fixed 50,000 VND off cinema orders from 300,000 VND'
    WHEN '40000000-0000-0000-0000-000000000005'::uuid THEN '30,000 VND student movie discount'
    WHEN '40000000-0000-0000-0000-000000000006'::uuid THEN '100,000 VND off for Premium members'
    ELSE description
END
WHERE id IN (
    '40000000-0000-0000-0000-000000000001'::uuid,
    '40000000-0000-0000-0000-000000000002'::uuid,
    '40000000-0000-0000-0000-000000000003'::uuid,
    '40000000-0000-0000-0000-000000000005'::uuid,
    '40000000-0000-0000-0000-000000000006'::uuid
);

UPDATE cinema_venues
SET name = CASE id
    WHEN '90000000-0000-0000-0000-000000000001'::uuid THEN 'Starlight Cinema Hanoi Center'
    WHEN '90000000-0000-0000-0000-000000000002'::uuid THEN 'Starlight Cinema Saigon Hub'
    ELSE name
END,
city = CASE id
    WHEN '90000000-0000-0000-0000-000000000001'::uuid THEN 'Ha Noi'
    WHEN '90000000-0000-0000-0000-000000000002'::uuid THEN 'Ho Chi Minh City'
    ELSE city
END,
address = CASE id
    WHEN '90000000-0000-0000-0000-000000000001'::uuid THEN '72 Trang Tien, Hoan Kiem'
    WHEN '90000000-0000-0000-0000-000000000002'::uuid THEN '2 Nguyen Hue, District 1'
    ELSE address
END,
updated_at = now()
WHERE id IN (
    '90000000-0000-0000-0000-000000000001'::uuid,
    '90000000-0000-0000-0000-000000000002'::uuid
);

UPDATE cinema_halls
SET name = CASE id
    WHEN '91000000-0000-0000-0000-000000000001'::uuid THEN 'Screen 8'
    WHEN '91000000-0000-0000-0000-000000000002'::uuid THEN 'Premium Screen'
    WHEN '91000000-0000-0000-0000-000000000003'::uuid THEN 'Screen B'
    ELSE name
END,
slug = CASE id
    WHEN '91000000-0000-0000-0000-000000000001'::uuid THEN 'screen-8'
    WHEN '91000000-0000-0000-0000-000000000002'::uuid THEN 'premium-screen'
    WHEN '91000000-0000-0000-0000-000000000003'::uuid THEN 'screen-b'
    ELSE slug
END,
updated_at = now()
WHERE id IN (
    '91000000-0000-0000-0000-000000000001'::uuid,
    '91000000-0000-0000-0000-000000000002'::uuid,
    '91000000-0000-0000-0000-000000000003'::uuid
);

UPDATE events
SET category_id = movie_data.category_id,
    title = movie_data.title,
    slug = movie_data.slug,
    description = movie_data.description,
    banner_url = movie_data.banner_url,
    location_name = movie_data.location_name,
    address = movie_data.address,
    city = movie_data.city,
    start_time = movie_data.start_time,
    end_time = movie_data.end_time,
    sale_start_time = movie_data.sale_start_time,
    sale_end_time = movie_data.sale_end_time,
    status = movie_data.status::event_status,
    hall_id = movie_data.hall_id,
    updated_at = now()
FROM (
    VALUES
        (
            '10000000-0000-0000-0000-000000000001'::uuid,
            1::bigint,
            'Galaxy Raiders',
            'galaxy-raiders',
            'A sci-fi action blockbuster about a rebel crew defending the last star gate.',
            'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1200&q=80',
            'Starlight Cinema Hanoi Center',
            '72 Trang Tien, Hoan Kiem',
            'Ha Noi',
            now() + interval '10 days',
            now() + interval '10 days 2 hours',
            now() - interval '3 days',
            now() + interval '9 days',
            'PUBLISHED',
            '91000000-0000-0000-0000-000000000001'::uuid
        ),
        (
            '10000000-0000-0000-0000-000000000002'::uuid,
            3::bigint,
            'Little Moon Adventure',
            'little-moon-adventure',
            'A colorful animated journey for families, dreamers, and weekend matinees.',
            'https://images.unsplash.com/photo-1524985069026-dd778a71c7b4?auto=format&fit=crop&w=1200&q=80',
            'Starlight Cinema Hanoi Center',
            '72 Trang Tien, Hoan Kiem',
            'Ha Noi',
            now() + interval '15 days',
            now() + interval '15 days 2 hours',
            now() - interval '2 days',
            now() + interval '14 days',
            'PUBLISHED',
            '91000000-0000-0000-0000-000000000002'::uuid
        ),
        (
            '10000000-0000-0000-0000-000000000003'::uuid,
            4::bigint,
            'Midnight Signal',
            'midnight-signal',
            'A tense midnight thriller about a mysterious broadcast inside an empty cinema.',
            'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?auto=format&fit=crop&w=1200&q=80',
            'Starlight Cinema Hanoi Center',
            '72 Trang Tien, Hoan Kiem',
            'Ha Noi',
            now() + interval '20 days',
            now() + interval '20 days 2 hours',
            now() - interval '1 day',
            now() + interval '18 days',
            'PUBLISHED',
            '91000000-0000-0000-0000-000000000001'::uuid
        ),
        (
            '10000000-0000-0000-0000-000000000004'::uuid,
            5::bigint,
            'The Last Letter',
            'the-last-letter',
            'A quiet drama about family, memory, and one letter that changes everything.',
            'https://images.unsplash.com/photo-1523207911345-32501502db22?auto=format&fit=crop&w=1200&q=80',
            'Starlight Cinema Hanoi Center',
            '72 Trang Tien, Hoan Kiem',
            'Ha Noi',
            now() + interval '25 days',
            now() + interval '25 days 2 hours',
            now() + interval '2 days',
            now() + interval '23 days',
            'DRAFT',
            '91000000-0000-0000-0000-000000000002'::uuid
        ),
        (
            '10000000-0000-0000-0000-000000000005'::uuid,
            6::bigint,
            'Love in Saigon',
            'love-in-saigon',
            'A warm romantic film set across neon streets, coffee shops, and rainy nights.',
            'https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=1200&q=80',
            'Starlight Cinema Saigon Hub',
            '2 Nguyen Hue, District 1',
            'Ho Chi Minh City',
            now() + interval '35 days',
            now() + interval '35 days 2 hours',
            now() + interval '1 day',
            now() + interval '34 days',
            'DRAFT',
            '91000000-0000-0000-0000-000000000003'::uuid
        ),
        (
            '10000000-0000-0000-0000-000000000006'::uuid,
            7::bigint,
            'AI: The Human Cut',
            'ai-the-human-cut',
            'A documentary exploring how artificial intelligence is changing creative work.',
            'https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?auto=format&fit=crop&w=1200&q=80',
            'Starlight Cinema Saigon Hub',
            '2 Nguyen Hue, District 1',
            'Ho Chi Minh City',
            now() - interval '5 days',
            now() - interval '5 days' + interval '2 hours',
            now() - interval '20 days',
            now() - interval '6 days',
            'FINISHED',
            '91000000-0000-0000-0000-000000000003'::uuid
        )
) AS movie_data(
    id,
    category_id,
    title,
    slug,
    description,
    banner_url,
    location_name,
    address,
    city,
    start_time,
    end_time,
    sale_start_time,
    sale_end_time,
    status,
    hall_id
)
WHERE events.id = movie_data.id;

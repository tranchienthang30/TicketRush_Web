-- V2__seed_sample_data.sql
-- TicketRush - sample data for local development/demo
-- Flyway location: src/main/resources/db/migration/V2__seed_sample_data.sql
--
-- Demo accounts use password: password
-- This file assumes V1__init_schema.sql has already been migrated.
-- Notes:
-- - Flyway/PostgreSQL handles the migration transaction, so this file does not use BEGIN/COMMIT.
-- - The seeded PENDING order/LOCKED seat uses a 2-hour expiry to stay visible during demos.
-- - PAID orders keep historical expires_at values based on their created_at time.


-- =========================================================
-- USERS: 4 accounts for local demo
-- =========================================================
INSERT INTO users (
    id, email, password_hash, full_name, avatar_url, phone, gender, date_of_birth,
    role, status, provider, provider_id, created_at, updated_at
)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'admin@ticketrush.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TicketRush Admin', NULL, '0900000001', 'OTHER', '1995-01-01', 'ADMIN', 'ACTIVE', 'LOCAL', NULL, now(), now()),
    ('00000000-0000-0000-0000-000000000002', 'organizer@ticketrush.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Event Organizer', NULL, '0900000002', 'MALE', '1997-04-12', 'ORGANIZER', 'ACTIVE', 'LOCAL', NULL, now(), now()),
    ('00000000-0000-0000-0000-000000000003', 'customer1@ticketrush.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Nguyen An', NULL, '0900000003', 'FEMALE', '2001-08-20', 'CUSTOMER', 'ACTIVE', 'LOCAL', NULL, now(), now()),
    ('00000000-0000-0000-0000-000000000004', 'customer2@ticketrush.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Tran Binh', NULL, '0900000004', 'MALE', '1999-11-05', 'CUSTOMER', 'ACTIVE', 'LOCAL', NULL, now(), now())
ON CONFLICT DO NOTHING;

-- =========================================================
-- CATEGORIES
-- =========================================================
INSERT INTO categories (id, name, slug, description, image_url, is_active, created_at)
VALUES
    (1, 'Music', 'music', 'Sự kiện âm nhạc', NULL, true, now()),
    (2, 'Concert', 'concert', 'Liveshow và concert', NULL, true, now()),
    (3, 'Workshop', 'workshop', 'Workshop, seminar và lớp học', NULL, true, now()),
    (4, 'Sport', 'sport', 'Sự kiện thể thao', NULL, true, now()),
    (5, 'Theater', 'theater', 'Sân khấu và kịch', NULL, true, now()),
    (6, 'Festival', 'festival', 'Lễ hội và sự kiện ngoài trời', NULL, true, now()),
    (7, 'Technology', 'technology', 'Công nghệ và đổi mới sáng tạo', NULL, true, now()),
    (8, 'Art', 'art', 'Triển lãm và nghệ thuật', NULL, true, now())
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('categories', 'id'), COALESCE((SELECT MAX(id) FROM categories), 1), true);

-- =========================================================
-- MEMBERSHIP PLANS
-- =========================================================
INSERT INTO membership_plans (id, name, description, price, duration_days, discount_percent, is_active, created_at)
VALUES
    (1, 'Silver', 'Gói cơ bản cho người dùng mới', 99000, 30, 5, true, now()),
    (2, 'Gold', 'Gói phổ biến với ưu đãi tốt hơn', 199000, 90, 10, true, now()),
    (3, 'Premium', 'Gói cao cấp cho người đặt vé thường xuyên', 499000, 365, 15, true, now())
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('membership_plans', 'id'), COALESCE((SELECT MAX(id) FROM membership_plans), 1), true);

-- =========================================================
-- USER MEMBERSHIPS
-- =========================================================
INSERT INTO user_memberships (id, user_id, plan_id, start_at, end_at, status, created_at)
VALUES
    ('70000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', 2, now() - interval '5 days', now() + interval '85 days', 'ACTIVE', now()),
    ('70000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000004', 3, now() - interval '10 days', now() + interval '355 days', 'ACTIVE', now()),
    ('70000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', 1, now() - interval '40 days', now() - interval '10 days', 'EXPIRED', now())
ON CONFLICT DO NOTHING;

-- =========================================================
-- VOUCHERS
-- =========================================================
INSERT INTO vouchers (
    id, code, description, discount_type, discount_value, max_discount, min_order_amount,
    membership_required, start_at, end_at, usage_limit, used_count, is_active, created_at
)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'WELCOME20', 'Giảm 20% cho người dùng mới', 'PERCENT', 20, 200000, 300000, false, now() - interval '30 days', now() + interval '180 days', 100, 1, true, now()),
    ('40000000-0000-0000-0000-000000000002', 'MEMBER10', 'Giảm 10% cho thành viên còn hạn', 'PERCENT', 10, 150000, 200000, true, now() - interval '10 days', now() + interval '120 days', 200, 0, true, now()),
    ('40000000-0000-0000-0000-000000000003', 'GOLD50', 'Giảm cố định 50.000 cho đơn từ 300.000', 'FIXED', 50000, NULL, 300000, true, now() - interval '10 days', now() + interval '90 days', 150, 0, true, now()),
    ('40000000-0000-0000-0000-000000000004', 'MUSIC15', 'Ưu đãi 15% cho sự kiện âm nhạc', 'PERCENT', 15, 120000, 250000, false, now() - interval '5 days', now() + interval '60 days', 80, 0, true, now()),
    ('40000000-0000-0000-0000-000000000005', 'STUDENT30', 'Giảm 30.000 cho sinh viên', 'FIXED', 30000, NULL, 150000, false, now() - interval '5 days', now() + interval '45 days', 120, 0, true, now()),
    ('40000000-0000-0000-0000-000000000006', 'PREMIUM100', 'Giảm 100.000 cho Premium', 'FIXED', 100000, NULL, 800000, true, now() - interval '5 days', now() + interval '365 days', 50, 0, true, now())
ON CONFLICT DO NOTHING;

-- =========================================================
-- EVENTS
-- =========================================================
INSERT INTO events (
    id, organizer_id, category_id, title, slug, description, banner_url,
    location_name, address, city, start_time, end_time, sale_start_time, sale_end_time,
    status, created_at, updated_at
)
VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 1, 'Summer Music Night', 'summer-music-night', 'Đêm nhạc mùa hè với nhiều nghệ sĩ trẻ.', NULL, 'Hanoi Opera House', '1 Tràng Tiền, Hoàn Kiếm', 'Hà Nội', now() + interval '10 days', now() + interval '10 days 3 hours', now() - interval '3 days', now() + interval '9 days', 'PUBLISHED', now(), now()),
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 3, 'Vue Spring Boot Workshop', 'vue-spring-boot-workshop', 'Workshop thực hành xây dựng web app với VueJS và Spring Boot.', NULL, 'VNU Technology Hub', 'Xuân Thủy, Cầu Giấy', 'Hà Nội', now() + interval '15 days', now() + interval '15 days 4 hours', now() - interval '2 days', now() + interval '14 days', 'PUBLISHED', now(), now()),
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', 4, 'Community Football Cup', 'community-football-cup', 'Giải bóng đá cộng đồng cuối tuần.', NULL, 'My Dinh Stadium', 'Lê Đức Thọ, Nam Từ Liêm', 'Hà Nội', now() + interval '20 days', now() + interval '20 days 5 hours', now() - interval '1 day', now() + interval '18 days', 'PUBLISHED', now(), now()),
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000002', 5, 'Indie Theater Night', 'indie-theater-night', 'Đêm diễn sân khấu độc lập.', NULL, 'Youth Theater', '11 Ngô Thì Nhậm', 'Hà Nội', now() + interval '25 days', now() + interval '25 days 2 hours', now() + interval '2 days', now() + interval '23 days', 'DRAFT', now(), now()),
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000002', 6, 'Food and Art Festival', 'food-and-art-festival', 'Lễ hội ẩm thực và nghệ thuật ngoài trời.', NULL, 'Crescent Lake', 'Phú Mỹ Hưng', 'TP Hồ Chí Minh', now() + interval '35 days', now() + interval '35 days 8 hours', now() + interval '1 day', now() + interval '34 days', 'PUBLISHED', now(), now()),
    ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000002', 7, 'AI Product Talk', 'ai-product-talk', 'Talkshow về sản phẩm AI và trải nghiệm người dùng.', NULL, 'Innovation Center', 'Quận 1', 'TP Hồ Chí Minh', now() - interval '5 days', (now() - interval '5 days') + interval '2 hours',now() - interval '20 days',now() - interval '6 days', 'FINISHED', now(), now())
ON CONFLICT DO NOTHING;

-- =========================================================
-- EVENT SECTIONS
-- =========================================================
INSERT INTO event_sections (id, event_id, name, base_price, row_count, seats_per_row, display_order, created_at, updated_at)
VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'VIP', 1000000, 2, 5, 1, now(), now()),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'A', 500000, 2, 5, 2, now(), now()),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'STANDARD', 300000, 3, 5, 1, now(), now()),
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'VIP', 700000, 1, 5, 2, now(), now()),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', 'A', 250000, 1, 10, 1, now(), now())
ON CONFLICT DO NOTHING;

-- =========================================================
-- EVENT SEATS: 50 seats total for demo seat map and booking
-- =========================================================
INSERT INTO event_seats (
    id, event_id, section_id, row_label, seat_number, seat_code, price,
    status, locked_by, lock_expires_at, version, created_at, updated_at
)
VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'A', 1, 'VIP-A1', 1000000, 'SOLD', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'A', 2, 'VIP-A2', 1000000, 'SOLD', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'A', 3, 'VIP-A3', 1000000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'A', 4, 'VIP-A4', 1000000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'A', 5, 'VIP-A5', 1000000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'B', 1, 'VIP-B1', 1000000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'B', 2, 'VIP-B2', 1000000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'B', 3, 'VIP-B3', 1000000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'B', 4, 'VIP-B4', 1000000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'B', 5, 'VIP-B5', 1000000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'A', 1, 'A-A1', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'A', 2, 'A-A2', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'A', 3, 'A-A3', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'A', 4, 'A-A4', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'A', 5, 'A-A5', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'B', 1, 'A-B1', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'B', 2, 'A-B2', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000018', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'B', 3, 'A-B3', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000019', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'B', 4, 'A-B4', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000020', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'B', 5, 'A-B5', 500000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'A', 1, 'STD-A1', 300000, 'LOCKED', '00000000-0000-0000-0000-000000000004', now() + interval '2 hours', 0, now(), now()),
    ('30000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'A', 2, 'STD-A2', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000023', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'A', 3, 'STD-A3', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000024', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'A', 4, 'STD-A4', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000025', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'A', 5, 'STD-A5', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000026', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'B', 1, 'STD-B1', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000027', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'B', 2, 'STD-B2', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000028', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'B', 3, 'STD-B3', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000029', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'B', 4, 'STD-B4', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000030', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'B', 5, 'STD-B5', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000031', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'C', 1, 'STD-C1', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000032', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'C', 2, 'STD-C2', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000033', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'C', 3, 'STD-C3', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000034', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'C', 4, 'STD-C4', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000035', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003', 'C', 5, 'STD-C5', 300000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000036', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', 'A', 1, 'VIP-A1', 700000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000037', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', 'A', 2, 'VIP-A2', 700000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000038', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', 'A', 3, 'VIP-A3', 700000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000039', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', 'A', 4, 'VIP-A4', 700000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000040', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004', 'A', 5, 'VIP-A5', 700000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000041', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 1, 'A-A1', 250000, 'SOLD', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000042', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 2, 'A-A2', 250000, 'SOLD', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000043', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 3, 'A-A3', 250000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000044', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 4, 'A-A4', 250000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000045', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 5, 'A-A5', 250000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000046', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 6, 'A-A6', 250000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000047', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 7, 'A-A7', 250000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000048', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 8, 'A-A8', 250000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000049', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 9, 'A-A9', 250000, 'AVAILABLE', NULL, NULL, 0, now(), now()),
    ('30000000-0000-0000-0000-000000000050', '10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005', 'A', 10, 'A-A10', 250000, 'AVAILABLE', NULL, NULL, 0, now(), now())
ON CONFLICT DO NOTHING;

-- =========================================================
-- ORDERS
-- =========================================================
INSERT INTO orders (
    id, user_id, event_id, status, subtotal, discount_amount, total_amount,
    voucher_id, expires_at, created_at, paid_at, cancelled_at
)
VALUES
    ('50000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'PAID', 2000000, 200000, 1800000, '40000000-0000-0000-0000-000000000001', now() - interval '1 day' + interval '10 minutes', now() - interval '1 day', now() - interval '1 day' + interval '5 minutes', NULL),
    ('50000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'PENDING', 300000, 0, 300000, NULL, now() + interval '2 hours', now(), NULL, NULL),
    ('50000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000003', 'PAID', 500000, 0, 500000, NULL, now() - interval '2 days' + interval '10 minutes', now() - interval '2 days', now() - interval '2 days' + interval '6 minutes', NULL),
    ('50000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'EXPIRED', 500000, 0, 500000, NULL, now() - interval '1 hour', now() - interval '2 hours', NULL, NULL)
ON CONFLICT DO NOTHING;

-- =========================================================
-- ORDER ITEMS + TICKET QR SAMPLE
-- =========================================================
INSERT INTO order_items (
    id, order_id, event_seat_id, price_snapshot, qr_code, ticket_status, issued_at, checked_in_at
)
VALUES
    ('60000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 1000000, 'TICKET-TR-000001', 'VALID', now() - interval '1 day', NULL),
    ('60000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', 1000000, 'TICKET-TR-000002', 'VALID', now() - interval '1 day', NULL),
    ('60000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000021', 300000, NULL, 'NOT_ISSUED', NULL, NULL),
    ('60000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000041', 250000, 'TICKET-TR-000003', 'VALID', now() - interval '2 days', NULL),
    ('60000000-0000-0000-0000-000000000005', '50000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000042', 250000, 'TICKET-TR-000004', 'USED', now() - interval '2 days', now() - interval '1 day'),
    ('60000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000012', 500000, NULL, 'NOT_ISSUED', NULL, NULL)
ON CONFLICT DO NOTHING;

-- =========================================================
-- VOUCHER REDEMPTIONS
-- =========================================================
INSERT INTO voucher_redemptions (id, voucher_id, user_id, order_id, discount_amount, used_at)
VALUES
    ('80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000001', 200000, now() - interval '1 day')
ON CONFLICT DO NOTHING;


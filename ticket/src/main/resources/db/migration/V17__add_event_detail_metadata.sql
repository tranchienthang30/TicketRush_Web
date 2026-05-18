ALTER TABLE events
    ADD COLUMN IF NOT EXISTS genre VARCHAR(120),
    ADD COLUMN IF NOT EXISTS country VARCHAR(120),
    ADD COLUMN IF NOT EXISTS author_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS director_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cast_members TEXT,
    ADD COLUMN IF NOT EXISTS performer_names TEXT,
    ADD COLUMN IF NOT EXISTS singer_names TEXT;

WITH metadata AS (
    SELECT *
    FROM (
        VALUES
            ('indie-night-live', 'Indie / Acoustic', 'Vietnam', 'Starlight Music Collective', NULL, NULL, 'The Velvet Days, Paper Lanterns, Mellow Park', 'Minh An, Ha My'),
            ('little-moon-adventure', 'Family Animation', 'Japan', 'Sora Tanaka', 'Yuki Mori', 'Aoi Haru, Kenji Sato, Mika Ito', NULL, NULL),
            ('midnight-stage-mystery', 'Mystery Theater', 'Vietnam', 'Le Minh Chau', 'Tran Quang Huy', 'Ngoc Lan, Duc Anh, Mai Phuong', 'Grand Stage Ensemble', NULL),
            ('illusion-night-vip', 'Magic / Variety', 'United States', 'Avery Cole', 'Morgan Bell', 'Lina Fox, David Stone', 'The Mirage Crew', NULL),
            ('saigon-basketball-cup', 'Basketball', 'Vietnam', 'Saigon Sports Association', NULL, 'Saigon Tigers, Hanoi Falcons, Da Nang Waves', 'Arena MC Team', NULL),
            ('starship-pop-concert', 'Pop Concert', 'South Korea', 'Nova Entertainment', 'Kim Jaehoon', 'Starship Dance Crew', 'Luna Ray, Orbit Five', 'Luna Ray'),
            ('creative-design-workshop', 'Design / Branding', 'Vietnam', 'Mai Linh Studio', NULL, 'Mai Linh, Pham Khoa, Anh Tue', 'Innovation Hub Mentors', NULL),
            ('laughing-saigon-comedy-show', 'Stand-up Comedy', 'Vietnam', 'Saigon Laugh Lab', 'Bao Nguyen', 'Minh Beo, Trang Moon, Tony Tran', 'Saigon Laugh Lab', NULL),
            ('ocean-food-festival', 'Food / Music Festival', 'Vietnam', 'Blue Coast Events', NULL, 'Chef Linh, Chef Bao, DJ Saltwave', 'Blue Coast Band, DJ Saltwave', 'Ha Vi'),
            ('planet-blue-talk', 'Sustainability Talk', 'Vietnam', 'Ocean Future Lab', NULL, 'Dr. Lan Anh, Nguyen Quoc Bao', 'Ocean Future Lab', NULL),
            ('dragon-school-cinema-day', 'Family Fantasy', 'United Kingdom', 'Eleanor Brooks', 'Samuel Finch', 'Lily Hart, Noah Wells, Grace Cole', NULL, NULL),
            ('night-corridor-play', 'Horror Theater', 'Vietnam', 'Pham Nhat An', 'Vu Hoang Nam', 'Thuy Chi, Minh Quan, Bao Tram', 'Main Hall Theatre Group', NULL),
            ('fast-lane-esports-final', 'Esports Racing', 'Singapore', 'Velocity League', NULL, 'Team Nitro, Apex Drift, Redline VN', 'Velocity League Hosts', NULL),
            ('parallel-beats-concert', 'Electronic Concert', 'Germany', 'Pulse Bureau', 'Jonas Keller', NULL, 'DJ Parallel, Neon Frame, AVA-9', 'AVA-9'),
            ('golden-kitchen-food-fair', 'Food Fair', 'Vietnam', 'Golden Kitchen Club', NULL, 'Chef My, Chef Tung, Chef Rika', 'Kitchen Stage Crew', NULL),
            ('the-silent-bridge-vip-show', 'Premium Stage Show', 'France', 'Camille Moreau', 'Luc Bernard', 'Elise Martin, Theo Dubois', 'Noir Bridge Company', NULL),
            ('wild-mekong-marathon', 'Marathon / Fan Zone', 'Vietnam', 'Mekong Run Club', NULL, 'Elite runners, community teams', 'Mekong Run Hosts', NULL),
            ('robot-cat-holiday-cinema', 'Family Animation', 'Japan', 'Hiro Arai', 'Mina Kobayashi', 'Riku Yamada, Emi Sato, Hana Mori', NULL, NULL),
            ('after-midnight-dj-set', 'DJ / Nightlife', 'Vietnam', 'Gold Lounge Sessions', 'Khoa Pham', NULL, 'DJ Midnight, DJ Lumen, MC Vy', 'Vy Nguyen'),
            ('love-on-platform-9-musical', 'Romantic Musical', 'United Kingdom', 'Oliver Hayes', 'Nora Bennett', 'Emma Reed, Lucas Gray, Sophie King', 'Platform 9 Orchestra', 'Emma Reed'),
            ('skyfall-district-bike-race', 'Cycling Race', 'Vietnam', 'District Cycling Club', NULL, 'Skyfall Riders, Saigon Sprint, Urban Wheels', 'Race Day Hosts', NULL),
            ('tiny-theatre-club', 'Comedy / Variety', 'Vietnam', 'Tiny Theatre Club', 'Minh Tran', 'Anh Dao, Huy Lam, Gia Bao', 'Tiny Theatre Ensemble', NULL),
            ('deep-space-arena-concert', 'Sci-fi Concert', 'United States', 'Orion Stageworks', 'Maya Reed', NULL, 'Galaxy Nine, The Orbitals', 'Astra Vale'),
            ('river-of-light-festival', 'Lantern Festival', 'Vietnam', 'Riverlight Collective', NULL, 'Lantern artists, food vendors, folk dancers', 'Riverlight Ensemble', 'Mai Hoa'),
            ('penguins-in-hanoi-cinema', 'Animation Adventure', 'Canada', 'Mason Lee', 'Clara Snow', 'Finn White, Nora Vale, Leo North', NULL, NULL),
            ('the-last-reef-masterclass', 'Marine Masterclass', 'Australia', 'Reef Research Group', NULL, 'Dr. Eva Stone, Prof. Liam West', 'Marine Research Panel', NULL)
    ) AS data(slug, genre, country, author_name, director_name, cast_members, performer_names, singer_names)
)
UPDATE events e
SET genre = metadata.genre,
    country = metadata.country,
    author_name = metadata.author_name,
    director_name = metadata.director_name,
    cast_members = metadata.cast_members,
    performer_names = metadata.performer_names,
    singer_names = metadata.singer_names,
    updated_at = now()
FROM metadata
WHERE e.slug = metadata.slug;

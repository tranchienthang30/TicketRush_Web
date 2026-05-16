-- Add movie metadata used by the public movies page.

ALTER TABLE events
ADD COLUMN IF NOT EXISTS duration_minutes INT,
ADD COLUMN IF NOT EXISTS listing_type VARCHAR(30) NOT NULL DEFAULT 'NOW_SHOWING';

ALTER TABLE events
DROP CONSTRAINT IF EXISTS ck_events_duration_minutes_positive;

ALTER TABLE events
ADD CONSTRAINT ck_events_duration_minutes_positive
CHECK (duration_minutes IS NULL OR duration_minutes > 0);

ALTER TABLE events
DROP CONSTRAINT IF EXISTS ck_events_listing_type;

ALTER TABLE events
ADD CONSTRAINT ck_events_listing_type
CHECK (listing_type IN ('NOW_SHOWING', 'UPCOMING', 'SPECIAL'));

CREATE INDEX IF NOT EXISTS idx_events_listing_type
ON events(listing_type);

CREATE INDEX IF NOT EXISTS idx_events_status_listing_start_time
ON events(status, listing_type, start_time);

UPDATE events
SET duration_minutes = GREATEST(1, ROUND(EXTRACT(EPOCH FROM (end_time - start_time)) / 60)::int)
WHERE duration_minutes IS NULL
  AND start_time IS NOT NULL
  AND end_time IS NOT NULL;

WITH movie_metadata AS (
    SELECT *
    FROM (
        VALUES
            ('10000000-0000-0000-0000-000000000001'::uuid, 128, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000002'::uuid, 94, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000003'::uuid, 111, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000004'::uuid, 117, 'SPECIAL'),
            ('10000000-0000-0000-0000-000000000005'::uuid, 105, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000006'::uuid, 96, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000010'::uuid, 132, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000011'::uuid, 109, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000012'::uuid, 98, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000013'::uuid, 104, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000014'::uuid, 87, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000015'::uuid, 92, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000016'::uuid, 106, 'NOW_SHOWING'),
            ('10000000-0000-0000-0000-000000000017'::uuid, 124, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000018'::uuid, 118, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000019'::uuid, 101, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000020'::uuid, 115, 'SPECIAL'),
            ('10000000-0000-0000-0000-000000000021'::uuid, 89, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000022'::uuid, 90, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000023'::uuid, 112, 'SPECIAL'),
            ('10000000-0000-0000-0000-000000000024'::uuid, 103, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000025'::uuid, 119, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000026'::uuid, 97, 'SPECIAL'),
            ('10000000-0000-0000-0000-000000000027'::uuid, 126, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000028'::uuid, 108, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000029'::uuid, 91, 'UPCOMING'),
            ('10000000-0000-0000-0000-000000000030'::uuid, 86, 'UPCOMING')
    ) AS metadata(id, duration_minutes, listing_type)
)
UPDATE events e
SET duration_minutes = movie_metadata.duration_minutes,
    listing_type = movie_metadata.listing_type,
    end_time = e.start_time + (movie_metadata.duration_minutes || ' minutes')::interval,
    updated_at = now()
FROM movie_metadata
WHERE e.id = movie_metadata.id;

UPDATE events
SET listing_type = 'NOW_SHOWING'
WHERE listing_type IS NULL;

COMMENT ON COLUMN events.duration_minutes IS 'Movie runtime in minutes, distinct from ticket sale window.';
COMMENT ON COLUMN events.listing_type IS 'Public movie listing bucket: NOW_SHOWING, UPCOMING, or SPECIAL.';

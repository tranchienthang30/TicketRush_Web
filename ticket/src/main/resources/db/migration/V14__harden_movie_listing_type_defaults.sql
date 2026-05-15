-- Keep listing type defaults explicit for databases upgraded from older local schemas.

UPDATE events
SET listing_type = 'NOW_SHOWING'
WHERE listing_type IS NULL;

ALTER TABLE events
ALTER COLUMN listing_type SET DEFAULT 'NOW_SHOWING',
ALTER COLUMN listing_type SET NOT NULL;

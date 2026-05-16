-- Backfill schema drift for local databases that applied an older V4.
ALTER TABLE events
ADD COLUMN IF NOT EXISTS external_seat_chart_key VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_events_external_seat_chart_key
ON events(external_seat_chart_key);

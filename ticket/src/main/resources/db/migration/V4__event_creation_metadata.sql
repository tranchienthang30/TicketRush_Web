-- V4__event_creation_metadata.sql
-- Adds event creation metadata for external seat chart tools and organizer payout details.

ALTER TABLE events
ADD COLUMN IF NOT EXISTS seat_provider VARCHAR(30) NOT NULL DEFAULT 'INTERNAL';

ALTER TABLE events
ADD COLUMN IF NOT EXISTS external_seat_chart_key VARCHAR(255);

ALTER TABLE events
ADD COLUMN IF NOT EXISTS payout_bank_name VARCHAR(120);

ALTER TABLE events
ADD COLUMN IF NOT EXISTS payout_account_name VARCHAR(160);

ALTER TABLE events
ADD COLUMN IF NOT EXISTS payout_account_number VARCHAR(80);

ALTER TABLE events
ADD COLUMN IF NOT EXISTS organizer_terms_accepted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_events_seat_provider ON events(seat_provider);
CREATE INDEX IF NOT EXISTS idx_events_external_seat_chart_key ON events(external_seat_chart_key);

COMMENT ON COLUMN events.seat_provider IS 'Seat map provider. INTERNAL for local generated seats, SEATS_IO for seats.io chart integration.';
COMMENT ON COLUMN events.external_seat_chart_key IS 'External chart key from a seat map provider such as seats.io.';
COMMENT ON COLUMN events.organizer_terms_accepted_at IS 'Timestamp when organizer accepted event publishing terms.';

-- Align pre-merge development schemas with the provider role naming.
-- Fresh databases already create provider_id in V1; older local schemas may still have organizer_id.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'events'
          AND column_name = 'organizer_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'events'
          AND column_name = 'provider_id'
    ) THEN
        ALTER TABLE events RENAME COLUMN organizer_id TO provider_id;
    END IF;
END$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_events_organizer'
    ) AND NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_events_provider'
    ) THEN
        ALTER TABLE events RENAME CONSTRAINT fk_events_organizer TO fk_events_provider;
    END IF;
END$$;

ALTER INDEX IF EXISTS idx_events_organizer_id RENAME TO idx_events_provider_id;
ALTER INDEX IF EXISTS idx_events_organizer_status RENAME TO idx_events_provider_status;

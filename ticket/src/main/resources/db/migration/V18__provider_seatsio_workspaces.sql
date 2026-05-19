CREATE TABLE IF NOT EXISTS provider_seat_workspaces (
    provider_id UUID PRIMARY KEY,
    seatsio_workspace_id BIGINT,
    workspace_name VARCHAR(255) NOT NULL,
    workspace_key VARCHAR(255) NOT NULL UNIQUE,
    workspace_secret_key VARCHAR(255) NOT NULL,
    is_test BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_provider_seat_workspaces_provider
        FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE events
ADD COLUMN IF NOT EXISTS external_seat_workspace_key VARCHAR(255),
ADD COLUMN IF NOT EXISTS external_seat_event_key VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_events_external_seat_workspace_key
ON events(external_seat_workspace_key);

CREATE INDEX IF NOT EXISTS idx_events_external_seat_event_key
ON events(external_seat_event_key);

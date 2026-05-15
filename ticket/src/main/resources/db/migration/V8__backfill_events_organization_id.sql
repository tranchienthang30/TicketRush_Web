-- Backfill schema drift for local databases that were created from older V3/V4 files.

ALTER TABLE users
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS primary_organization_id UUID,
ADD COLUMN IF NOT EXISTS provider_request_status VARCHAR(30),
ADD COLUMN IF NOT EXISTS provider_requested_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    business_email VARCHAR(255) NOT NULL UNIQUE,
    owner_id UUID NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_organizations_owner_id ON organizations(owner_id);
CREATE INDEX IF NOT EXISTS idx_organizations_business_email ON organizations(business_email);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_organizations_owner'
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT fk_organizations_owner
            FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_organizations_set_updated_at'
    ) THEN
        CREATE TRIGGER trg_organizations_set_updated_at
        BEFORE UPDATE ON organizations
        FOR EACH ROW
        EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_users_primary_organization'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_primary_organization
            FOREIGN KEY (primary_organization_id) REFERENCES organizations(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_primary_organization_id ON users(primary_organization_id);
CREATE INDEX IF NOT EXISTS idx_users_provider_request_status ON users(provider_request_status);

ALTER TABLE events
ADD COLUMN IF NOT EXISTS organization_id UUID,
ADD COLUMN IF NOT EXISTS seat_provider VARCHAR(30) NOT NULL DEFAULT 'INTERNAL',
ADD COLUMN IF NOT EXISTS payout_bank_name VARCHAR(120),
ADD COLUMN IF NOT EXISTS payout_account_name VARCHAR(120),
ADD COLUMN IF NOT EXISTS payout_account_number VARCHAR(60),
ADD COLUMN IF NOT EXISTS provider_terms_accepted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_events_organization_id ON events(organization_id);
CREATE INDEX IF NOT EXISTS idx_events_seat_provider ON events(seat_provider);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_events_organization'
    ) THEN
        ALTER TABLE events
            ADD CONSTRAINT fk_events_organization
            FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL;
    END IF;
END $$;

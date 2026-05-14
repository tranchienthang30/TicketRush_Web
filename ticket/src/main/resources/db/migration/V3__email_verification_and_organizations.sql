-- V3__email_verification_and_organizations.sql
-- Adds email verification and organization ownership with minimal impact to existing data.

ALTER TABLE users
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT false;

-- Existing local/dev accounts predate this verification workflow.
-- Keep them usable and mark them verified.
UPDATE users
SET email_verified = true
WHERE email_verified = false;

CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(255) NOT NULL,
    business_email VARCHAR(255) NOT NULL UNIQUE,
    owner_id UUID NOT NULL,

    verified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_organizations_owner
        FOREIGN KEY (owner_id) REFERENCES users(id),

    CONSTRAINT ck_organizations_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_organizations_business_email_not_blank CHECK (length(trim(business_email)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_organizations_owner_id ON organizations(owner_id);
CREATE INDEX IF NOT EXISTS idx_organizations_business_email ON organizations(business_email);

CREATE TRIGGER trg_organizations_set_updated_at
BEFORE UPDATE ON organizations
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

ALTER TABLE users
ADD COLUMN IF NOT EXISTS primary_organization_id UUID;

ALTER TABLE users
ADD CONSTRAINT fk_users_primary_organization
FOREIGN KEY (primary_organization_id) REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_users_primary_organization_id ON users(primary_organization_id);

ALTER TABLE events
ADD COLUMN IF NOT EXISTS organization_id UUID;

ALTER TABLE events
ADD CONSTRAINT fk_events_organization
FOREIGN KEY (organization_id) REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_events_organization_id ON events(organization_id);

COMMENT ON TABLE organizations IS 'Verified organization profile that owns organizer-created events.';
COMMENT ON COLUMN users.email_verified IS 'True when the account email has been verified or trusted through OAuth.';
COMMENT ON COLUMN users.primary_organization_id IS 'Primary organization the user manages as organizer.';
COMMENT ON COLUMN events.organization_id IS 'Organization that owns the event.';

ALTER TABLE users
ADD COLUMN IF NOT EXISTS provider_reviewed_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS provider_reviewed_by UUID,
ADD COLUMN IF NOT EXISTS provider_rejection_reason TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_users_provider_reviewed_by'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT fk_users_provider_reviewed_by
        FOREIGN KEY (provider_reviewed_by) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_provider_reviewed_by ON users(provider_reviewed_by);

UPDATE users
SET provider_request_status = 'APPROVED',
    provider_reviewed_at = COALESCE(provider_reviewed_at, updated_at)
WHERE role = 'PROVIDER'
  AND provider_request_status IS NULL;

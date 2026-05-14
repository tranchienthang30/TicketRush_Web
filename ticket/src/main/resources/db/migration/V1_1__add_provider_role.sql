-- Adds PROVIDER as the system role used by movie/cinema providers.
-- Kept before V2 so seed data can safely use this enum value after commit.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum
        WHERE enumtypid = 'user_role'::regtype
          AND enumlabel = 'PROVIDER'
    ) THEN
        ALTER TYPE user_role ADD VALUE 'PROVIDER';
    END IF;
END$$;

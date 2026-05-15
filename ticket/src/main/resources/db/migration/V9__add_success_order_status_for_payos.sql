-- Support asynchronous payment finalization where orders move from PENDING to SUCCESS.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'order_status'
          AND e.enumlabel = 'SUCCESS'
    ) THEN
        ALTER TYPE order_status ADD VALUE 'SUCCESS';
    END IF;
END $$;

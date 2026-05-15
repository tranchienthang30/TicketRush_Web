ALTER TABLE orders
DROP CONSTRAINT IF EXISTS ck_orders_paid_at_required;

ALTER TABLE orders
ADD CONSTRAINT ck_orders_paid_at_required CHECK (
    status NOT IN ('PAID', 'SUCCESS') OR paid_at IS NOT NULL
);

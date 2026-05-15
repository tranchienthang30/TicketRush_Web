ALTER TABLE orders
ADD COLUMN IF NOT EXISTS payos_order_code BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_payos_order_code
ON orders(payos_order_code)
WHERE payos_order_code IS NOT NULL;

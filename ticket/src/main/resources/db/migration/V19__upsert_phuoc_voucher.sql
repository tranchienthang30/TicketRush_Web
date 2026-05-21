-- Ensure PHUOC voucher is available and effectively non-expiring across environments.
INSERT INTO vouchers (
    id,
    code,
    description,
    discount_type,
    discount_value,
    max_discount,
    min_order_amount,
    membership_required,
    start_at,
    end_at,
    usage_limit,
    used_count,
    is_active,
    created_at
)
VALUES (
    '40000000-0000-0000-0000-000000000099'::uuid,
    'PHUOC',
    'Voucher PHUOC khong gioi han thoi gian',
    'PERCENT',
    20,
    200000,
    0,
    false,
    now() - interval '1 day',
    '9999-12-31 23:59:59+00'::timestamptz,
    NULL,
    0,
    true,
    now()
)
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    discount_type = EXCLUDED.discount_type,
    discount_value = EXCLUDED.discount_value,
    max_discount = EXCLUDED.max_discount,
    min_order_amount = EXCLUDED.min_order_amount,
    membership_required = EXCLUDED.membership_required,
    start_at = LEAST(vouchers.start_at, now() - interval '1 day'),
    end_at = '9999-12-31 23:59:59+00'::timestamptz,
    usage_limit = NULL,
    is_active = true;

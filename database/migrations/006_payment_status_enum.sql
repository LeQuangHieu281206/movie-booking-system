-- Migration 006: payments.status -> PostgreSQL enum payment_status_enum.
--
-- Bổ sung CANCELLED/EXPIRED để đóng các payment PENDING khi booking bị huỷ hoặc
-- hết hạn, tránh các bản ghi "mồ côi" không bao giờ kết thúc.
--
-- Script idempotent: chạy lại nhiều lần không lỗi.
-- Chạy trên Supabase PostgreSQL:
--   psql "$SUPABASE_DB_URL" -f database/migrations/006_payment_status_enum.sql

BEGIN;

-- 1. Tạo type nếu chưa có, và bổ sung giá trị còn thiếu.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status_enum') THEN
        CREATE TYPE public.payment_status_enum AS ENUM
            ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum e JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'payment_status_enum' AND e.enumlabel = 'CANCELLED'
    ) THEN
        ALTER TYPE public.payment_status_enum ADD VALUE 'CANCELLED';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum e JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'payment_status_enum' AND e.enumlabel = 'EXPIRED'
    ) THEN
        ALTER TYPE public.payment_status_enum ADD VALUE 'EXPIRED';
    END IF;
END $$;

-- 2. Bỏ CHECK cũ (enum đã thay thế), bỏ partial index rồi chuyển cột sang enum.
ALTER TABLE public.payments
    DROP CONSTRAINT IF EXISTS payments_status_check;

-- Partial index dùng predicate status = 'PENDING'; phải bỏ trước khi đổi kiểu
-- vì PostgreSQL không cho predicate chứa cast không IMMUTABLE.
DROP INDEX IF EXISTS public.uk_payments_one_pending_per_booking;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'payments'
          AND column_name = 'status' AND udt_name <> 'payment_status_enum'
    ) THEN
        -- Cột đang có DEFAULT 'PENDING'::varchar, phải bỏ trước khi đổi kiểu.
        ALTER TABLE public.payments ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE public.payments
            ALTER COLUMN status TYPE public.payment_status_enum
            USING status::text::public.payment_status_enum;
        ALTER TABLE public.payments ALTER COLUMN status SET DEFAULT 'PENDING';
    END IF;
END $$;

-- Mỗi booking tối đa một payment PENDING (dùng cho upsert ON CONFLICT).
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_one_pending_per_booking
    ON public.payments (booking_id)
    WHERE status = 'PENDING';

COMMIT;

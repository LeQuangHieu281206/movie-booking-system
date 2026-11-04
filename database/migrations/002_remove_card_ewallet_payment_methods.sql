-- Migration 002: chỉ còn CASH và BANK_TRANSFER (bỏ CARD, E_WALLET).
-- Chạy trên Supabase PostgreSQL:
--   psql "$SUPABASE_DB_URL" -f database/migrations/002_remove_card_ewallet_payment_methods.sql
-- Script idempotent: chạy lại nhiều lần không lỗi.

BEGIN;

-- ============================================================
-- 1. payment_method_enum: chỉ còn CASH, BANK_TRANSFER
--    Không xoá được giá trị khỏi enum -> tạo lại type.
--    Hiện không cột nào dùng type này, nên drop/create an toàn.
-- ============================================================
DO $$
DECLARE
    v_used integer;
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE n.nspname = 'public' AND t.typname = 'payment_method_enum'
    ) THEN
        SELECT count(*) INTO v_used
        FROM pg_attribute a
        JOIN pg_type t ON t.oid = a.atttypid
        WHERE t.typname = 'payment_method_enum'
          AND a.attnum > 0
          AND NOT a.attisdropped;

        IF v_used = 0 THEN
            DROP TYPE public.payment_method_enum;
            CREATE TYPE public.payment_method_enum AS ENUM ('CASH', 'BANK_TRANSFER');
        ELSE
            RAISE NOTICE 'payment_method_enum vẫn được % cột dùng - bỏ qua drop/create', v_used;
        END IF;
    ELSE
        CREATE TYPE public.payment_method_enum AS ENUM ('CASH', 'BANK_TRANSFER');
    END IF;
END $$;

-- ============================================================
-- 2. bookings.payment_method: chặn giá trị đã bỏ ở tầng DB
--    Cột là varchar (Booking entity map EnumType.STRING), nên thêm CHECK.
-- ============================================================
ALTER TABLE public.bookings
    DROP CONSTRAINT IF EXISTS bookings_payment_method_check;

ALTER TABLE public.bookings
    ADD CONSTRAINT bookings_payment_method_check
    CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'BANK_TRANSFER'));

COMMIT;

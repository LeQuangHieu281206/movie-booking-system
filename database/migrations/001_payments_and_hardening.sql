-- Migration 001: bảng payments + hardening schema.
-- Chạy trên Supabase PostgreSQL:
--   psql "$SUPABASE_DB_URL" -f database/migrations/001_payments_and_hardening.sql
-- Script idempotent: chạy lại nhiều lần không lỗi.

BEGIN;

-- ============================================================
-- 1. payments: lưu vết giao dịch SePay, chống trùng webhook
-- ============================================================
CREATE TABLE IF NOT EXISTS public.payments (
    payment_id      bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    booking_id      bigint NOT NULL,
    amount          numeric(12, 0) NOT NULL,
    currency        varchar(8) NOT NULL DEFAULT 'VND',
    status          varchar(16) NOT NULL DEFAULT 'PENDING',
    provider        varchar(32) NOT NULL DEFAULT 'SEPAY',
    provider_txn_id text,
    reference_code  text,
    qr_url          text,
    raw_payload     jsonb,
    failure_reason  text,
    paid_at         timestamp with time zone,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT payments_pkey PRIMARY KEY (payment_id),
    CONSTRAINT payments_amount_check CHECK (amount >= 0),
    CONSTRAINT payments_status_check CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    -- Unique cho phép nhiều NULL, nên các payment PENDING chưa có mã giao dịch vẫn hợp lệ.
    CONSTRAINT uk_payments_provider_txn UNIQUE (provider, provider_txn_id)
);

COMMENT ON TABLE public.payments IS 'Lịch sử giao dịch thanh toán do payment-service ghi nhận';
COMMENT ON COLUMN public.payments.provider_txn_id IS 'ID giao dịch phía cổng thanh toán, dùng để chống xử lý trùng webhook';
COMMENT ON COLUMN public.payments.reference_code IS 'Nội dung đối soát, ví dụ DH19';
COMMENT ON COLUMN public.payments.raw_payload IS 'Payload webhook gốc để đối soát khi cần';

CREATE INDEX IF NOT EXISTS idx_payments_booking ON public.payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON public.payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_reference ON public.payments (reference_code);

-- ============================================================
-- 2. booking_details: showtime_seat_id -> seat_id
--    Dữ liệu cũ có 2 kiểu: showtime_seat_id (trước refactor) và seat_id vật lý
--    (sau refactor). Chỉ chuyển các dòng còn map được qua showtime_seats.
-- ============================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'booking_details'
          AND column_name = 'showtime_seat_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'booking_details'
          AND column_name = 'seat_id'
    ) THEN
        UPDATE public.booking_details bd
        SET showtime_seat_id = ss.seat_id
        FROM public.bookings b, public.showtime_seats ss
        WHERE bd.booking_id = b.booking_id
          AND ss.showtime_seat_id = bd.showtime_seat_id
          AND ss.showtime_id = b.showtime_id;

        ALTER TABLE public.booking_details
            RENAME COLUMN showtime_seat_id TO seat_id;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_booking_details_booking ON public.booking_details (booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_details_seat ON public.booking_details (seat_id);

-- ============================================================
-- 3. Index còn thiếu cho các truy vấn nóng
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_bookings_status_time ON public.bookings (status, booking_time);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON public.bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_showtime_id ON public.bookings (showtime_id);
CREATE INDEX IF NOT EXISTS idx_showtimes_movie_start ON public.showtimes (movie_id, start_time);
CREATE INDEX IF NOT EXISTS idx_showtimes_room_start ON public.showtimes (room_id, start_time);
CREATE INDEX IF NOT EXISTS idx_seats_room ON public.seats (room_id);
CREATE INDEX IF NOT EXISTS idx_rooms_cinema ON public.rooms (cinema_id);
CREATE INDEX IF NOT EXISTS idx_movie_genres_genre ON public.movie_genres (genre_id);

-- ============================================================
-- 4. booking_time bắt buộc để scheduler hết hạn booking luôn hoạt động
-- ============================================================
UPDATE public.bookings SET booking_time = created_at WHERE booking_time IS NULL;
ALTER TABLE public.bookings ALTER COLUMN booking_time SET DEFAULT now();
ALTER TABLE public.bookings ALTER COLUMN booking_time SET NOT NULL;

-- ============================================================
-- 5. Ràng buộc toàn vẹn dữ liệu
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_seats_room_pos') THEN
        ALTER TABLE public.seats
            ADD CONSTRAINT uk_seats_room_pos UNIQUE (room_id, seat_row, seat_number);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_rooms_cinema_name') THEN
        ALTER TABLE public.rooms
            ADD CONSTRAINT uk_rooms_cinema_name UNIQUE (cinema_id, name);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_showtime_time') THEN
        ALTER TABLE public.showtimes
            ADD CONSTRAINT ck_showtime_time CHECK (end_time > start_time);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_hold_coherence') THEN
        ALTER TABLE public.showtime_seats
            ADD CONSTRAINT ck_hold_coherence CHECK (
                status = 'AVAILABLE'
                OR (status = 'HOLDING' AND hold_until IS NOT NULL)
                OR (status = 'BOOKED' AND hold_until IS NULL)
            );
    END IF;
END $$;

-- ============================================================
-- 6. Email không phân biệt hoa thường
-- ============================================================
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_lower ON public.users (lower(email));

COMMIT;

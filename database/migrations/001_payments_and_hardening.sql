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

-- Mỗi booking chỉ có tối đa một payment PENDING. Nếu có dữ liệu trùng từ trước,
-- giữ bản mới nhất và đánh dấu các bản cũ là FAILED để tạo được unique index.
UPDATE public.payments p
SET status = 'FAILED',
    failure_reason = 'Payment PENDING trùng, đã thay bằng bản mới nhất',
    updated_at = now()
WHERE p.status = 'PENDING'
  AND EXISTS (
      SELECT 1 FROM public.payments p2
      WHERE p2.booking_id = p.booking_id
        AND p2.status = 'PENDING'
        AND p2.payment_id > p.payment_id
  );

CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_one_pending_per_booking
    ON public.payments (booking_id) WHERE status = 'PENDING';

-- payments.booking_id -> bookings.booking_id
-- Dự án đang dùng chung một Supabase DB nên vẫn giữ FK để bảo đảm toàn vẹn.
-- Khi tách DB theo service, FK này sẽ được thay bằng kiểm tra qua BookingClient.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'payments_booking_id_fkey') THEN
        ALTER TABLE public.payments
            ADD CONSTRAINT payments_booking_id_fkey
            FOREIGN KEY (booking_id) REFERENCES public.bookings (booking_id);
    END IF;
END $$;

-- ============================================================
-- 2. booking_details: giữ seat_id và thêm showtime_seat_id
--    - seat_id          : ghế vật lý, dùng cho release/audit nhanh
--    - showtime_seat_id : dòng tồn kho ghế-theo-suất, join ra cả seats và showtimes
--    Hỗ trợ cả schema gốc (chỉ có showtime_seat_id) và schema sau refactor (chỉ có seat_id).
-- ============================================================

-- 2.1 Bổ sung seat_id nếu schema gốc chỉ có showtime_seat_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'booking_details'
          AND column_name = 'seat_id'
    ) THEN
        ALTER TABLE public.booking_details ADD COLUMN seat_id bigint;
        UPDATE public.booking_details bd
        SET seat_id = ss.seat_id
        FROM public.showtime_seats ss
        WHERE ss.showtime_seat_id = bd.showtime_seat_id;
    END IF;
END $$;

-- 2.2 Bổ sung showtime_seat_id nếu schema sau refactor chỉ có seat_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'booking_details'
          AND column_name = 'showtime_seat_id'
    ) THEN
        ALTER TABLE public.booking_details ADD COLUMN showtime_seat_id bigint;
    END IF;
END $$;

-- 2.3 Backfill showtime_seat_id từ bookings.showtime_id + booking_details.seat_id
UPDATE public.booking_details bd
SET showtime_seat_id = ss.showtime_seat_id
FROM public.bookings b, public.showtime_seats ss
WHERE bd.booking_id = b.booking_id
  AND ss.showtime_id = b.showtime_id
  AND ss.seat_id = bd.seat_id
  AND bd.showtime_seat_id IS NULL;

-- 2.4 Backfill seat_id từ showtime_seat_id cho dữ liệu cũ
UPDATE public.booking_details bd
SET seat_id = ss.seat_id
FROM public.showtime_seats ss
WHERE ss.showtime_seat_id = bd.showtime_seat_id
  AND bd.seat_id IS NULL;

-- 2.5 Không thể tiếp tục nếu còn dòng không map được
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM public.booking_details
        WHERE seat_id IS NULL OR showtime_seat_id IS NULL
    ) THEN
        RAISE EXCEPTION 'booking_details có dòng không map được sang seats/showtime_seats';
    END IF;
END $$;

ALTER TABLE public.booking_details ALTER COLUMN seat_id SET NOT NULL;
ALTER TABLE public.booking_details ALTER COLUMN showtime_seat_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_booking_details_booking ON public.booking_details (booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_details_seat ON public.booking_details (seat_id);
CREATE INDEX IF NOT EXISTS idx_booking_details_showtime_seat ON public.booking_details (showtime_seat_id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'booking_details_seat_id_fkey') THEN
        ALTER TABLE public.booking_details
            ADD CONSTRAINT booking_details_seat_id_fkey
            FOREIGN KEY (seat_id) REFERENCES public.seats (seat_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'booking_details_showtime_seat_id_fkey') THEN
        ALTER TABLE public.booking_details
            ADD CONSTRAINT booking_details_showtime_seat_id_fkey
            FOREIGN KEY (showtime_seat_id) REFERENCES public.showtime_seats (showtime_seat_id);
    END IF;
END $$;

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

-- showtime_seats.seat_id -> seats.seat_id
-- Đây là mắt nối còn thiếu giữa tồn kho ghế-theo-suất và ghế vật lý.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'showtime_seats_seat_id_fkey') THEN
        ALTER TABLE public.showtime_seats
            ADD CONSTRAINT showtime_seats_seat_id_fkey
            FOREIGN KEY (seat_id) REFERENCES public.seats (seat_id);
    END IF;
END $$;

-- Đổi tên FK tự sinh của showtime_id cho dễ đọc trên Supabase.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkdcy3vgv0vf44n12fq5vouan3b') THEN
        ALTER TABLE public.showtime_seats
            RENAME CONSTRAINT fkdcy3vgv0vf44n12fq5vouan3b TO showtime_seats_showtime_id_fkey;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_showtime_seats_seat ON public.showtime_seats (seat_id);

-- Bảo đảm cặp (showtime_seat_id, seat_id) trong booking_details trỏ đúng
-- cùng một dòng tồn kho ghế, tránh trường hợp hai FK riêng lẻ đều hợp lệ
-- nhưng lệch nhau.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_showtime_seats_id_seat') THEN
        ALTER TABLE public.showtime_seats
            ADD CONSTRAINT uk_showtime_seats_id_seat UNIQUE (showtime_seat_id, seat_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'booking_details_showtime_seat_pair_fkey') THEN
        ALTER TABLE public.booking_details
            ADD CONSTRAINT booking_details_showtime_seat_pair_fkey
            FOREIGN KEY (showtime_seat_id, seat_id)
            REFERENCES public.showtime_seats (showtime_seat_id, seat_id);
    END IF;
END $$;

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

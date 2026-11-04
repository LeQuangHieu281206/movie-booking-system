-- Migration 002: chuyển các cột trạng thái sang PostgreSQL enum, các cột chuỗi tự do sang text.
-- Chạy trên Supabase PostgreSQL:
--   psql "$SUPABASE_DB_URL" -f database/migrations/002_enums_and_text.sql
-- Script idempotent: chạy lại nhiều lần không lỗi.

BEGIN;

-- ============================================================
-- 1. Enum types
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role_enum') THEN
        CREATE TYPE public.user_role_enum AS ENUM ('CUSTOMER', 'ADMIN');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'movie_status_enum') THEN
        CREATE TYPE public.movie_status_enum AS ENUM ('UPCOMING', 'SHOWING', 'ENDED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'age_restriction_enum') THEN
        CREATE TYPE public.age_restriction_enum AS ENUM ('P', 'K', 'T13', 'T16', 'T18');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'room_type_enum') THEN
        CREATE TYPE public.room_type_enum AS ENUM ('2D', '3D', 'IMAX', '4DX');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'seat_type_enum') THEN
        CREATE TYPE public.seat_type_enum AS ENUM ('NORMAL', 'VIP', 'COUPLE');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'showtime_status_enum') THEN
        CREATE TYPE public.showtime_status_enum AS ENUM ('SCHEDULED', 'ONGOING', 'ENDED', 'CANCELLED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'seat_status_enum') THEN
        CREATE TYPE public.seat_status_enum AS ENUM ('AVAILABLE', 'BOOKED', 'HOLDING');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_status_enum') THEN
        CREATE TYPE public.booking_status_enum AS ENUM ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method_enum') THEN
        CREATE TYPE public.payment_method_enum AS ENUM ('CASH', 'BANK_TRANSFER', 'CARD', 'E_WALLET');
    END IF;
END $$;

-- ============================================================
-- 2. Cột trạng thái -> enum
-- ============================================================
ALTER TABLE public.users
    ALTER COLUMN role TYPE public.user_role_enum USING role::text::public.user_role_enum;

ALTER TABLE public.movies
    ALTER COLUMN status TYPE public.movie_status_enum USING status::text::public.movie_status_enum,
    ALTER COLUMN age_restriction TYPE public.age_restriction_enum USING age_restriction::text::public.age_restriction_enum;

ALTER TABLE public.rooms
    ALTER COLUMN room_type TYPE public.room_type_enum USING room_type::text::public.room_type_enum;

ALTER TABLE public.seats
    ALTER COLUMN seat_type TYPE public.seat_type_enum USING seat_type::text::public.seat_type_enum;

ALTER TABLE public.showtimes
    ALTER COLUMN status TYPE public.showtime_status_enum USING status::text::public.showtime_status_enum;

ALTER TABLE public.showtime_seats
    ALTER COLUMN status TYPE public.seat_status_enum USING status::text::public.seat_status_enum;

ALTER TABLE public.bookings
    ALTER COLUMN status TYPE public.booking_status_enum USING status::text::public.booking_status_enum,
    ALTER COLUMN payment_method TYPE public.payment_method_enum USING payment_method::text::public.payment_method_enum;

-- ============================================================
-- 3. Cột chuỗi tự do -> text
-- ============================================================
ALTER TABLE public.cinemas
    ALTER COLUMN name TYPE text,
    ALTER COLUMN address TYPE text,
    ALTER COLUMN city TYPE text,
    ALTER COLUMN phone_number TYPE varchar(12);

ALTER TABLE public.genres
    ALTER COLUMN name TYPE text;

ALTER TABLE public.movies
    ALTER COLUMN title TYPE text,
    ALTER COLUMN language TYPE text,
    ALTER COLUMN poster_url TYPE text,
    ALTER COLUMN trailer_url TYPE text,
    ALTER COLUMN director TYPE text;

ALTER TABLE public.rooms
    ALTER COLUMN name TYPE text;

ALTER TABLE public.seats
    ALTER COLUMN seat_row TYPE text;

ALTER TABLE public.showtime_seats
    ALTER COLUMN hold_by TYPE text;

ALTER TABLE public.users
    ALTER COLUMN phone_number TYPE varchar(12);

ALTER TABLE public.payments
    ALTER COLUMN currency TYPE text,
    ALTER COLUMN provider TYPE text;

COMMIT;

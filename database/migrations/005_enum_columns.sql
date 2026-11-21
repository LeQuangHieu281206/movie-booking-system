-- Migration 005: chuyển các cột trạng thái từ varchar sang PostgreSQL enum.
--
-- Vì sao cần: migration 002_enums_and_text.sql đã tạo 9 enum type nhưng các
-- cột trên database đang chạy vẫn là varchar (002 chưa được áp hoặc DB đã được
-- tạo lại). Entity JPA khai báo enum native nên Hibernate sinh CAST tới tên
-- type, gặp cột varchar sẽ lỗi 42704 / "operator does not exist".
--
-- Script idempotent: cột nào đã đúng enum thì bỏ qua.
-- Chạy trên Supabase PostgreSQL:
--   psql "$SUPABASE_DB_URL" -f database/migrations/005_enum_columns.sql

BEGIN;

-- 1. Bảo đảm đủ enum type (cùng nội dung với 002).
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
        CREATE TYPE public.payment_method_enum AS ENUM ('CASH', 'BANK_TRANSFER');
    END IF;
END $$;

-- 2. Chuyển từng cột, chỉ khi cột chưa dùng đúng enum type.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users'
          AND column_name = 'role' AND udt_name <> 'user_role_enum'
    ) THEN
        ALTER TABLE public.users
            ALTER COLUMN role TYPE public.user_role_enum
            USING role::text::public.user_role_enum;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'movies'
          AND column_name = 'status' AND udt_name <> 'movie_status_enum'
    ) THEN
        ALTER TABLE public.movies
            ALTER COLUMN status TYPE public.movie_status_enum
            USING status::text::public.movie_status_enum;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'movies'
          AND column_name = 'age_restriction' AND udt_name <> 'age_restriction_enum'
    ) THEN
        ALTER TABLE public.movies
            ALTER COLUMN age_restriction TYPE public.age_restriction_enum
            USING age_restriction::text::public.age_restriction_enum;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'rooms'
          AND column_name = 'room_type' AND udt_name <> 'room_type_enum'
    ) THEN
        ALTER TABLE public.rooms
            ALTER COLUMN room_type TYPE public.room_type_enum
            USING room_type::text::public.room_type_enum;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'seats'
          AND column_name = 'seat_type' AND udt_name <> 'seat_type_enum'
    ) THEN
        ALTER TABLE public.seats
            ALTER COLUMN seat_type TYPE public.seat_type_enum
            USING seat_type::text::public.seat_type_enum;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'showtimes'
          AND column_name = 'status' AND udt_name <> 'showtime_status_enum'
    ) THEN
        ALTER TABLE public.showtimes
            ALTER COLUMN status TYPE public.showtime_status_enum
            USING status::text::public.showtime_status_enum;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'showtime_seats'
          AND column_name = 'status' AND udt_name <> 'seat_status_enum'
    ) THEN
        ALTER TABLE public.showtime_seats
            ALTER COLUMN status TYPE public.seat_status_enum
            USING status::text::public.seat_status_enum;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'bookings'
          AND column_name = 'status' AND udt_name <> 'booking_status_enum'
    ) THEN
        ALTER TABLE public.bookings
            ALTER COLUMN status TYPE public.booking_status_enum
            USING status::text::public.booking_status_enum;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'bookings'
          AND column_name = 'payment_method' AND udt_name <> 'payment_method_enum'
    ) THEN
        ALTER TABLE public.bookings
            ALTER COLUMN payment_method TYPE public.payment_method_enum
            USING payment_method::text::public.payment_method_enum;
    END IF;
END $$;

COMMIT;

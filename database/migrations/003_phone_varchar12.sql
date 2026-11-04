-- Migration 003: chuẩn hoá độ dài cột số điện thoại.
-- Số di động Việt Nam 10 số, số bàn tối đa 11 số; varchar(12) giữ được cả hai.
-- Chạy trên Supabase PostgreSQL:
--   psql "$SUPABASE_DB_URL" -f database/migrations/003_phone_varchar12.sql
-- Script idempotent: chạy lại nhiều lần không lỗi.

BEGIN;

ALTER TABLE public.users
    ALTER COLUMN phone_number TYPE varchar(12) USING phone_number::varchar(12);

ALTER TABLE public.cinemas
    ALTER COLUMN phone_number TYPE varchar(12) USING phone_number::varchar(12);

COMMIT;

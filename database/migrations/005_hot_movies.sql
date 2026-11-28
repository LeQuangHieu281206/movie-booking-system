-- Migration 005: admin chọn top 5 phim hot hiển thị ở trang chủ.
-- Chạy trên Supabase PostgreSQL (TRƯỚC khi khởi động lại movie-service, vì ddl-auto=validate):
--   psql "$SUPABASE_DB_URL" -f database/migrations/005_hot_movies.sql
-- Script idempotent: chạy lại nhiều lần không lỗi.

BEGIN;

-- Thứ hạng hot 1..5; NULL = phim không nằm trong top.
ALTER TABLE public.movies
    ADD COLUMN IF NOT EXISTS hot_rank smallint;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_movies_hot_rank') THEN
        ALTER TABLE public.movies
            ADD CONSTRAINT ck_movies_hot_rank CHECK (hot_rank BETWEEN 1 AND 5);
    END IF;
    -- DEFERRABLE để đổi chỗ thứ hạng trong cùng một transaction không bị lỗi trùng tạm thời.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_movies_hot_rank') THEN
        ALTER TABLE public.movies
            ADD CONSTRAINT uk_movies_hot_rank UNIQUE (hot_rank) DEFERRABLE INITIALLY DEFERRED;
    END IF;
END $$;

COMMIT;

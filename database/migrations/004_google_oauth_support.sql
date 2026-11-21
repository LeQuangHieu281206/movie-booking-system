-- Migration 004: hỗ trợ đăng nhập bằng Google OAuth2 (ID Token) trên bảng users.
-- Chạy trên Supabase PostgreSQL:
--   psql "$SUPABASE_DB_URL" -f database/migrations/004_google_oauth_support.sql
-- Script idempotent: chạy lại nhiều lần không lỗi.

BEGIN;

-- 1. Cho phép password_hash nhận giá trị NULL (tài khoản đăng nhập qua Google không có mật khẩu nội bộ)
ALTER TABLE public.users
    ALTER COLUMN password_hash DROP NOT NULL;

-- 2. Thêm cột auth_provider (mặc định 'LOCAL', chỉ nhận 'LOCAL' hoặc 'GOOGLE')
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS auth_provider varchar(20) NOT NULL DEFAULT 'LOCAL';

UPDATE public.users
SET auth_provider = 'LOCAL'
WHERE auth_provider IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_users_auth_provider') THEN
        ALTER TABLE public.users
            ADD CONSTRAINT ck_users_auth_provider
            CHECK (auth_provider IN ('LOCAL', 'GOOGLE'));
    END IF;
END $$;

-- 3. Thêm cột google_id (VARCHAR, UNIQUE, NULLABLE)
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS google_id varchar(255);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_google_id') THEN
        ALTER TABLE public.users
            ADD CONSTRAINT uk_users_google_id
            UNIQUE (google_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_google_id ON public.users (google_id);

COMMIT;

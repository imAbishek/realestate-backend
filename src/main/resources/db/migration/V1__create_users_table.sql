-- ============================================================
-- V1__create_users_table.sql
-- Flyway migration — runs automatically on app startup
-- Location: src/main/resources/db/migration/
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TYPE user_role AS ENUM ('BUYER', 'SELLER', 'AGENT', 'ADMIN');

CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name              VARCHAR(150)  NOT NULL,
    email             VARCHAR(255)  NOT NULL UNIQUE,
    phone             VARCHAR(15)   UNIQUE,
    password_hash     TEXT          NOT NULL,
    role              user_role     NOT NULL DEFAULT 'BUYER',
    profile_photo_url TEXT,
    is_verified       BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    otp_code          VARCHAR(10),
    otp_expires_at    TIMESTAMP,
    last_login_at     TIMESTAMP,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role  ON users(role);

-- Auto-update updated_at on every row change
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

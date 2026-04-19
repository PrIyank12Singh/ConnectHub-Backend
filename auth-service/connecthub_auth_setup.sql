-- =============================================================
-- ConnectHub Auth Service — MySQL Database Setup Script
-- Run this ONCE before starting the application in prod/dev mode
-- =============================================================

-- 1. Create database
CREATE DATABASE IF NOT EXISTS connecthub_auth
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE connecthub_auth;

-- 2. Create users table
CREATE TABLE IF NOT EXISTS users (
    user_id     CHAR(36)        NOT NULL,
    username    VARCHAR(50)     NOT NULL,
    email       VARCHAR(120)    NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    full_name   VARCHAR(120)    NOT NULL,
    avatar_url  VARCHAR(500)    DEFAULT NULL,
    bio         VARCHAR(400)    DEFAULT NULL,

    -- Enums stored as VARCHAR — must match Java enum names exactly
    status      VARCHAR(20)     NOT NULL DEFAULT 'ONLINE',   -- ONLINE | AWAY | DND | INVISIBLE
    provider    VARCHAR(20)     NOT NULL DEFAULT 'LOCAL',    -- LOCAL | GOOGLE | GITHUB
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER',     -- USER | ROOM_ADMIN | PLATFORM_ADMIN

    is_active   TINYINT(1)      NOT NULL DEFAULT 1,
    last_seen_at DATETIME       DEFAULT NULL,
    created_at  DATETIME        NOT NULL,
    updated_at  DATETIME        NOT NULL,

    PRIMARY KEY (user_id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Indexes for common queries
CREATE INDEX idx_status   ON users (status);
CREATE INDEX idx_username ON users (username);   -- already unique but explicit for range scans
CREATE INDEX idx_is_active ON users (is_active);

-- 4. (Optional) seed a platform admin account
--    Password below = "Admin@1234" bcrypt-hashed. Change it immediately after first login.
-- INSERT INTO users (user_id, username, email, password_hash, full_name,
--                    status, provider, role, is_active, created_at, updated_at)
-- VALUES (
--     UUID(),
--     'admin',
--     'admin@connecthub.local',
--     '$2a$10$7EqJtq98hPqEX7fNZaFWoOf6VjQAkqyxhFmVK1oWQqKZxX1k.O5Gy',
--     'Platform Admin',
--     'ONLINE', 'LOCAL', 'PLATFORM_ADMIN', 1,
--     NOW(), NOW()
-- );

-- =============================================================
-- Verify
-- =============================================================
SHOW TABLES;
DESCRIBE users;

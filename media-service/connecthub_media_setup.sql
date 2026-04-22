-- =============================================================
-- ConnectHub Media Service — MySQL Database Setup Script
-- Run this ONCE before starting media-service
-- =============================================================

CREATE DATABASE IF NOT EXISTS connecthub_media
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE connecthub_media;

CREATE TABLE IF NOT EXISTS media_files (
    media_id        CHAR(36)        NOT NULL,
    uploader_id     CHAR(36)        NOT NULL,
    room_id         CHAR(36)        DEFAULT NULL,
    message_id      CHAR(36)        DEFAULT NULL,
    filename        VARCHAR(255)    NOT NULL,
    original_name   VARCHAR(255)    NOT NULL,
    url             VARCHAR(1000)   NOT NULL,
    thumbnail_url   VARCHAR(1000)   DEFAULT NULL,
    mime_type       VARCHAR(100)    NOT NULL,
    size_kb         BIGINT          NOT NULL,
    media_type      VARCHAR(10)     NOT NULL,   -- IMAGE | FILE | VIDEO | AUDIO
    width           INT             DEFAULT NULL,
    height          INT             DEFAULT NULL,
    uploaded_at     DATETIME        NOT NULL,

    PRIMARY KEY (media_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_media_uploader    ON media_files (uploader_id);
CREATE INDEX idx_media_room        ON media_files (room_id);
CREATE INDEX idx_media_message     ON media_files (message_id);
CREATE INDEX idx_media_type        ON media_files (media_type);
CREATE INDEX idx_media_uploaded_at ON media_files (uploaded_at DESC);

-- Verify
SHOW TABLES;
DESCRIBE media_files;

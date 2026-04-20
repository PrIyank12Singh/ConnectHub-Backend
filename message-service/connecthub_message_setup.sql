-- =============================================================
-- ConnectHub Message Service — MySQL Database Setup Script
-- Run this ONCE before starting message-service
-- =============================================================

CREATE DATABASE IF NOT EXISTS connecthub_message
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE connecthub_message;

-- Messages table
CREATE TABLE IF NOT EXISTS messages (
    message_id          CHAR(36)        NOT NULL,
    room_id             CHAR(36)        NOT NULL,
    sender_id           CHAR(36)        NOT NULL,
    content             TEXT            DEFAULT NULL,
    type                VARCHAR(10)     NOT NULL DEFAULT 'TEXT',   -- TEXT | IMAGE | FILE | REACTION | SYSTEM
    media_url           VARCHAR(1000)   DEFAULT NULL,
    reply_to_message_id CHAR(36)        DEFAULT NULL,
    is_edited           TINYINT(1)      NOT NULL DEFAULT 0,
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0,
    delivery_status     VARCHAR(10)     NOT NULL DEFAULT 'SENT',   -- SENT | DELIVERED | READ
    sent_at             DATETIME        NOT NULL,
    edited_at           DATETIME        DEFAULT NULL,

    PRIMARY KEY (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_messages_room_id      ON messages (room_id);
CREATE INDEX idx_messages_sender_id    ON messages (sender_id);
CREATE INDEX idx_messages_sent_at      ON messages (sent_at DESC);
CREATE INDEX idx_messages_room_sent    ON messages (room_id, sent_at DESC);
CREATE INDEX idx_messages_is_deleted   ON messages (is_deleted);
CREATE FULLTEXT INDEX idx_messages_content ON messages (content);

-- Verify
SHOW TABLES;
DESCRIBE messages;

-- ================================================================
-- ConnectHub — notification-service  |  Database Setup
-- Run this once before starting the service.
-- ================================================================

-- 1. Create the database
CREATE DATABASE IF NOT EXISTS connecthub_notification
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE connecthub_notification;

-- 2. Create table
CREATE TABLE IF NOT EXISTS notifications (
    notification_id  BIGINT          NOT NULL AUTO_INCREMENT,
    recipient_id     VARCHAR(36)     NOT NULL,
    actor_id         VARCHAR(36)     NULL,
    type             ENUM('NEW_MESSAGE','MENTION','ROOM_INVITE','SYSTEM') NOT NULL,
    title            VARCHAR(200)    NOT NULL,
    message          VARCHAR(500)    NOT NULL,
    room_id          VARCHAR(36)     NULL,
    message_id       VARCHAR(36)     NULL,
    is_read          TINYINT(1)      NOT NULL DEFAULT 0,
    created_at       DATETIME        NOT NULL,
    PRIMARY KEY (notification_id),
    INDEX idx_notif_recipient        (recipient_id),
    INDEX idx_notif_recipient_read   (recipient_id, is_read),
    INDEX idx_notif_type             (type),
    INDEX idx_notif_room             (room_id),
    INDEX idx_notif_created          (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- Verification queries — run AFTER Postman tests
-- ================================================================

-- Q1: All notifications newest first
SELECT * FROM notifications ORDER BY created_at DESC;

-- Q2: Unread count per user
SELECT recipient_id, COUNT(*) AS unread_count
FROM notifications
WHERE is_read = 0
GROUP BY recipient_id;

-- Q3: Notifications by type
SELECT type, COUNT(*) AS count
FROM notifications
GROUP BY type;

-- Q4: All unread for a specific user (replace the UUID)
SELECT * FROM notifications
WHERE recipient_id = 'YOUR-USER-UUID-HERE'
  AND is_read = 0
ORDER BY created_at DESC;

-- Q5: Confirm mark-all-read worked
SELECT recipient_id, is_read, COUNT(*) AS count
FROM notifications
GROUP BY recipient_id, is_read;

-- ================================================================
-- ConnectHub — presence-service  |  Database Setup
-- Run this once before starting the service.
-- ================================================================

-- 1. Create the database
CREATE DATABASE IF NOT EXISTS connecthub_presence
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE connecthub_presence;

-- 2. Create table (Hibernate auto-creates this too via ddl-auto=update,
--    but running it manually gives you full control)
CREATE TABLE IF NOT EXISTS user_presence (
    presence_id   BIGINT          NOT NULL AUTO_INCREMENT,
    user_id       VARCHAR(36)     NOT NULL,           -- UUID from auth-service
    status        ENUM('ONLINE','AWAY','DND','INVISIBLE') NOT NULL DEFAULT 'ONLINE',
    custom_message VARCHAR(160)   NULL,
    device_type   VARCHAR(20)     NOT NULL DEFAULT 'WEB',
    ip_address    VARCHAR(45)     NULL,
    connected_at  DATETIME        NOT NULL,
    last_ping_at  DATETIME        NOT NULL,
    session_id    VARCHAR(100)    NOT NULL UNIQUE,    -- one row per WS session
    created_at    DATETIME        NOT NULL,
    updated_at    DATETIME        NOT NULL,
    PRIMARY KEY (presence_id),
    INDEX idx_presence_user_id  (user_id),
    INDEX idx_presence_session  (session_id),
    INDEX idx_presence_status   (status),
    INDEX idx_presence_ping     (last_ping_at)       -- used by stale-cleanup query
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- Verification queries — run AFTER starting the service and
-- making a few API calls to confirm data is persisted correctly.
-- ================================================================

-- Q1: All active sessions
SELECT * FROM user_presence ORDER BY last_ping_at DESC;

-- Q2: Count of distinct online users
SELECT COUNT(DISTINCT user_id) AS online_users
FROM user_presence
WHERE status = 'ONLINE';

-- Q3: Sessions that would be cleaned up (stale > 60 s)
SELECT session_id, user_id, last_ping_at,
       TIMESTAMPDIFF(SECOND, last_ping_at, NOW()) AS seconds_since_ping
FROM user_presence
WHERE last_ping_at < DATE_SUB(NOW(), INTERVAL 60 SECOND);

-- Q4: All sessions per user (multi-device check)
SELECT user_id, COUNT(*) AS session_count, GROUP_CONCAT(device_type) AS devices
FROM user_presence
GROUP BY user_id;

-- Q5: Status distribution
SELECT status, COUNT(*) AS count
FROM user_presence
GROUP BY status;

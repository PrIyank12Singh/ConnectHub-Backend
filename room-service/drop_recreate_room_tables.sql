-- =============================================================
-- Step 1: Drop existing tables (clears wrong structure)
-- =============================================================
USE connecthub_room;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS room_members;
DROP TABLE IF EXISTS rooms;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- Step 2: Recreate with correct CHAR(36) UUID columns
-- =============================================================

CREATE TABLE rooms (
    room_id         CHAR(36)        NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(500)    DEFAULT NULL,
    type            VARCHAR(10)     NOT NULL,
    created_by_id   CHAR(36)        NOT NULL,
    avatar_url      VARCHAR(500)    DEFAULT NULL,
    is_private      TINYINT(1)      NOT NULL DEFAULT 0,
    max_members     INT             NOT NULL DEFAULT 100,
    last_message_at DATETIME        DEFAULT NULL,
    created_at      DATETIME        NOT NULL,
    updated_at      DATETIME        NOT NULL,

    PRIMARY KEY (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE room_members (
    member_id       BIGINT          NOT NULL AUTO_INCREMENT,
    room_id         CHAR(36)        NOT NULL,
    user_id         CHAR(36)        NOT NULL,
    role            VARCHAR(10)     NOT NULL DEFAULT 'MEMBER',
    joined_at       DATETIME        NOT NULL,
    last_read_at    DATETIME        DEFAULT NULL,
    is_muted        TINYINT(1)      NOT NULL DEFAULT 0,

    PRIMARY KEY (member_id),
    UNIQUE KEY uq_room_user (room_id, user_id),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_rooms_created_by  ON rooms (created_by_id);
CREATE INDEX idx_rooms_type        ON rooms (type);
CREATE INDEX idx_rooms_last_msg    ON rooms (last_message_at);
CREATE INDEX idx_members_user_id   ON room_members (user_id);
CREATE INDEX idx_members_room_id   ON room_members (room_id);

-- Verify
SHOW TABLES;
DESCRIBE rooms;
DESCRIBE room_members;

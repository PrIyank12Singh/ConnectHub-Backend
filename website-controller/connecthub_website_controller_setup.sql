-- ============================================================
-- ConnectHub — connecthub-web (MVC Layer)
-- SQL VERIFICATION QUERIES
-- ============================================================
-- connecthub-web is a stateless aggregator layer.
-- It has NO own database — it delegates to downstream services.
-- Verify behaviour by querying the downstream service databases.
--
-- Databases:
--   connecthub_auth           (auth-service         — port 8081)
--   connecthub_room           (room-service         — port 8082)
--   connecthub_message        (message-service      — port 8083)
--   connecthub_media          (media-service        — port 8084)
--   connecthub_presence       (presence-service     — port 8085)
--   connecthub_notification   (notification-service — port 8086)
-- ============================================================


-- ============================================================
-- STEP 1: After POST /web/auth/register
-- Expected: 1 row in connecthub_auth.users
-- ============================================================
USE connecthub_auth;

SELECT user_id, username, email, full_name, status, is_active, created_at
FROM users
WHERE email = 'alice@example.com';

-- Expected: is_active = 1, status = 'ONLINE' or 'INVISIBLE'


-- ============================================================
-- STEP 2: After POST /web/auth/login
-- Expected: last_seen_at updated when WS connects later
-- ============================================================
USE connecthub_auth;

SELECT user_id, username, last_seen_at
FROM users
WHERE email = 'alice@example.com';


-- ============================================================
-- STEP 3: After POST /web/rooms (create room)
-- Expected: 1 room + 1 room_member (creator as ADMIN)
-- ============================================================
USE connecthub_room;

-- 3a: Verify room created
SELECT room_id, name, type, is_private, created_by_id, created_at
FROM rooms
ORDER BY created_at DESC
LIMIT 5;

-- 3b: Verify creator added as ADMIN
SELECT rm.room_id, rm.user_id, rm.role, rm.joined_at, rm.is_muted
FROM room_members rm
JOIN rooms r ON r.room_id = rm.room_id
WHERE r.name = 'dev-team';

-- Expected: role = 'ADMIN', is_muted = 0


-- ============================================================
-- STEP 4: After POST /web/rooms/{roomId}/members (add member)
-- Expected: 1 new room_member row with role MEMBER
-- ============================================================
USE connecthub_room;

SELECT user_id, role, joined_at, is_muted
FROM room_members
WHERE room_id = 'YOUR_ROOM_UUID_HERE';

-- Expected: 2 rows (creator ADMIN + new user MEMBER)


-- ============================================================
-- STEP 5: After PUT /web/rooms/{roomId}/members/{userId}/mute?mute=true
-- Expected: is_muted = 1 for that user
-- ============================================================
USE connecthub_room;

SELECT user_id, role, is_muted
FROM room_members
WHERE room_id = 'YOUR_ROOM_UUID_HERE'
  AND user_id = 'TARGET_USER_UUID';

-- Expected: is_muted = 1


-- ============================================================
-- STEP 6: After POST /web/messages (send message)
-- Expected: 1 row in connecthub_message.messages with status SENT
-- ============================================================
USE connecthub_message;

SELECT message_id, room_id, sender_id, content, type,
       is_edited, is_deleted, delivery_status, sent_at
FROM messages
WHERE room_id = 'YOUR_ROOM_UUID_HERE'
ORDER BY sent_at DESC
LIMIT 10;

-- Expected: delivery_status = 'SENT', is_deleted = 0, is_edited = 0


-- ============================================================
-- STEP 7: After PUT /web/messages/{messageId} (edit)
-- Expected: is_edited = 1, content updated, edited_at set
-- ============================================================
USE connecthub_message;

SELECT message_id, content, is_edited, edited_at
FROM messages
WHERE message_id = 'YOUR_MESSAGE_UUID_HERE';

-- Expected: is_edited = 1, edited_at is NOT NULL


-- ============================================================
-- STEP 8: After DELETE /web/messages/{messageId} (soft delete)
-- Expected: is_deleted = 1 (row still exists — soft delete)
-- ============================================================
USE connecthub_message;

SELECT message_id, content, is_deleted
FROM messages
WHERE message_id = 'YOUR_MESSAGE_UUID_HERE';

-- Expected: is_deleted = 1 (NOT removed from DB)


-- ============================================================
-- STEP 9: After PUT /web/messages/{messageId}/status?status=READ
-- Expected: delivery_status = 'READ'
-- ============================================================
USE connecthub_message;

SELECT message_id, delivery_status
FROM messages
WHERE message_id = 'YOUR_MESSAGE_UUID_HERE';


-- ============================================================
-- STEP 10: After POST /web/presence/online/{userId}
-- Expected: 1 row in connecthub_presence.user_presence with status ONLINE
-- ============================================================
USE connecthub_presence;

SELECT presence_id, user_id, status, device_type, connected_at, last_ping_at, session_id
FROM user_presence
WHERE user_id = 'YOUR_USER_UUID_HERE';

-- Expected: status = 'ONLINE', last_ping_at recent


-- ============================================================
-- STEP 11: After PUT /web/presence/status/{userId}?status=AWAY
-- Expected: status = 'AWAY'
-- ============================================================
USE connecthub_presence;

SELECT user_id, status
FROM user_presence
WHERE user_id = 'YOUR_USER_UUID_HERE';


-- ============================================================
-- STEP 12: After POST /web/presence/offline/{userId}
-- Expected: row removed OR status set to OFFLINE depending on impl
-- ============================================================
USE connecthub_presence;

SELECT user_id, status
FROM user_presence
WHERE user_id = 'YOUR_USER_UUID_HERE';

-- If presence-service deletes on offline: 0 rows
-- If it keeps: status = 'OFFLINE' or 'INVISIBLE'


-- ============================================================
-- STEP 13: After GET /web/dashboard/{userId}
-- No DB change — but verify aggregation is correct by comparing:
-- ============================================================

-- a) Profile matches users table
USE connecthub_auth;
SELECT user_id, username, email, status
FROM users
WHERE user_id = 'YOUR_USER_UUID_HERE';

-- b) Rooms list matches room_members
USE connecthub_room;
SELECT r.room_id, r.name, r.type, r.last_message_at
FROM rooms r
JOIN room_members rm ON r.room_id = rm.room_id
WHERE rm.user_id = 'YOUR_USER_UUID_HERE'
ORDER BY r.last_message_at DESC;


-- ============================================================
-- STEP 14: Verify unread count logic
-- Uses subquery — no manual datetime pasting needed
-- ============================================================
USE connecthub_message;

SELECT COUNT(*) AS unread_count
FROM messages
WHERE room_id = 'YOUR_ROOM_UUID_HERE'
  AND sent_at > (
      SELECT last_read_at
      FROM connecthub_room.room_members
      WHERE room_id = 'YOUR_ROOM_UUID_HERE'
        AND user_id  = 'YOUR_USER_UUID_HERE'
  )
  AND is_deleted = 0;

-- This count should match what /web/rooms/{roomId}/unread/{userId} returns


-- ============================================================
-- STEP 15: After POST /web/admin/broadcast
-- Expected: rows in connecthub_notification.notifications for all users
-- FIX: database is connecthub_notification (not connecthub_notif)
-- ============================================================
USE connecthub_notification;

SELECT notification_id, recipient_id, actor_id, type, title, message, is_read, created_at
FROM notifications
WHERE type = 'SYSTEM'
ORDER BY created_at DESC
LIMIT 10;


-- ============================================================
-- USEFUL CROSS-DATABASE HEALTH CHECK
-- Run this to confirm all service DBs are reachable
-- FIX: corrected connecthub_notification name
-- ============================================================
SELECT 'connecthub_auth'         AS db, COUNT(*) AS users    FROM connecthub_auth.users;
SELECT 'connecthub_room'         AS db, COUNT(*) AS rooms    FROM connecthub_room.rooms;
SELECT 'connecthub_message'      AS db, COUNT(*) AS messages FROM connecthub_message.messages;
SELECT 'connecthub_media'        AS db, COUNT(*) AS files    FROM connecthub_media.media_files;
SELECT 'connecthub_presence'     AS db, COUNT(*) AS sessions FROM connecthub_presence.user_presence;
SELECT 'connecthub_notification' AS db, COUNT(*) AS notifs   FROM connecthub_notification.notifications;

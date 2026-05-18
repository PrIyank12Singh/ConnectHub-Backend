# ConnectHub — Presence / Status Service

## What this service does
Tracks every user's live online state using one MySQL row per WebSocket session.  
Exposes REST endpoints called by the WebSocket handler, the Angular frontend, and the Admin dashboard.

---

## Port
| Service          | Port |
|-----------------|------|
| api-gateway      | 8080 |
| auth-service     | 8081 |
| room-service     | 8082 |
| message-service  | 8083 |
| media-service    | 8084 |
| **presence-service** | **8085** |

---

## 1 — MySQL Setup

```sql
-- Run schema.sql (included in this zip)
CREATE DATABASE IF NOT EXISTS connecthub_presence
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Hibernate will auto-create the `user_presence` table on first boot via `ddl-auto=update`.  
If you want manual control, run the full `schema.sql` file first.

---

## 2 — Configuration (application.properties)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/connecthub_presence?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

auth.jwt.secret=connecthub-dev-secret-key-change-me-please-32bytes
server.port=8085
```

The JWT secret **must match** the value in auth-service and api-gateway.

---

## 3 — Build & Run

```bash
cd presence-service
mvn clean install -DskipTests
mvn spring-boot:run
```

Expected startup log:
```
Started PresenceServiceApplication on port 8085
```

---

## 4 — API Gateway Update

Replace `api-gateway/src/main/java/.../config/GatewayConfig.java` with the
updated file provided in the `api-gateway-updated/` folder.

It adds two new routes:

| Route ID             | Path pattern                                              | Auth    | Target |
|---------------------|-----------------------------------------------------------|---------|--------|
| presence-internal   | /presence/online, /presence/offline/**, /presence/session/**, /presence/cleanup | None (server-to-server) | :8085 |
| presence-protected  | /presence/**                                              | JWT     | :8085 |

Restart the gateway after replacing the file.

---

## 5 — Postman Testing

1. Import `ConnectHub_Presence_Service.postman_collection.json` into Postman.
2. The collection uses variables: `GATEWAY`, `token`, `userId`, `sessionId`.
3. **Run request 0 first** — it calls `/auth/login` and auto-saves the token + userId.
4. Then run requests **1 → 16 in order**.

### Quick sanity test sequence:
```
0. Login          → saves token + userId
1. Set Online     → creates presence row, saves sessionId
2. Get Presence   → confirms row exists, status = ONLINE
3. Update → AWAY  → confirms status change
5. Update → ONLINE
6. Bulk Presence  → confirms known user is ONLINE, unknown is INVISIBLE
8. Online Count   → should be >= 1
9. Ping Session   → updates lastPingAt
10. Is Online     → returns { online: true }
15. Set Offline   → deletes all rows for user
16. Get Presence  → 404 (no session)
```

---

## 6 — MySQL Verification

After running Postman tests:

```sql
USE connecthub_presence;

-- See all rows
SELECT * FROM user_presence ORDER BY last_ping_at DESC;

-- Count online
SELECT COUNT(DISTINCT user_id) AS online_users FROM user_presence WHERE status = 'ONLINE';

-- Check for stale rows (> 60s without ping)
SELECT session_id, user_id, TIMESTAMPDIFF(SECOND, last_ping_at, NOW()) AS stale_seconds
FROM user_presence
WHERE last_ping_at < DATE_SUB(NOW(), INTERVAL 60 SECOND);
```

---

## 7 — Frontend Integration

Copy these files into your Angular project:

| File | Destination |
|------|-------------|
| `presence.service.ts` | `src/app/core/services/` |
| `presence-indicator.component.ts` | `src/app/shared/components/` |

### Wire into chat-layout (call on WebSocket connect):

```typescript
// In chat-layout.component.ts
constructor(private presenceService: PresenceService, private authService: AuthService) {}

ngOnInit(): void {
  const user = this.authService.getCurrentUser();
  if (user) {
    this.presenceService.goOnline(user.userId).subscribe();
  }

  // Mark offline on browser close
  window.addEventListener('beforeunload', () => {
    if (user) this.presenceService.disconnectSession().subscribe();
  });
}
```

### Show online dot in room member list:

```html
<app-presence-indicator [userId]="member.userId" [showLabel]="true" />
```

### Status picker:

```typescript
this.presenceService.updateStatus(userId, 'AWAY', 'In a meeting').subscribe();
```

---

## 8 — STOMP / WebSocket Integration Notes

The WebSocket handler (when you build it) should call these endpoints:

| WebSocket event                   | REST call to presence-service              |
|----------------------------------|--------------------------------------------|
| `afterConnectionEstablished()`   | `POST /presence/online`                    |
| `afterConnectionClosed()`        | `DELETE /presence/session/{sessionId}`     |
| User sends PRESENCE_UPDATE frame | `PUT /presence/status`                     |
| Scheduled heartbeat (30s)        | `PUT /presence/ping/{sessionId}`           |

---

## 9 — Endpoints Summary

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /presence/online | No (internal) | Create / refresh session |
| DELETE | /presence/offline/{userId} | No (internal) | Remove all sessions |
| DELETE | /presence/session/{sessionId} | No (internal) | Remove one session |
| PUT | /presence/status | JWT | Update status + message |
| GET | /presence/{userId} | JWT | Get latest presence |
| POST | /presence/bulk | JWT | Bulk presence for member list |
| GET | /presence/users/online | JWT | All online users |
| GET | /presence/count/online | JWT | Online user count |
| PUT | /presence/ping/{sessionId} | JWT | Keep-alive ping |
| GET | /presence/check/{userId} | JWT | Boolean online check |
| DELETE | /presence/cleanup | No (internal) | Manual stale cleanup |

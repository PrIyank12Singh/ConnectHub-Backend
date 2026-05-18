# ConnectHub — Notification Service

## Port
| Service               | Port |
|----------------------|------|
| api-gateway           | 8080 |
| auth-service          | 8081 |
| room-service          | 8082 |
| message-service       | 8083 |
| media-service         | 8084 |
| presence-service      | 8085 |
| **notification-service** | **8086** |

---

## 1 — MySQL Setup

```sql
CREATE DATABASE IF NOT EXISTS connecthub_notification
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Hibernate auto-creates the `notifications` table on first boot.

---

## 2 — Configuration (application.properties)

```properties
spring.datasource.password=P_22@LovePUBG
server.port=8086

# Email — false in dev (logs to console), true in prod
notification.email.enabled=false

# FCM — false in dev (logs to console), true in prod
notification.fcm.enabled=false
```

---

## 3 — Build & Run

```bash
cd notification-service
mvn clean install -DskipTests
mvn spring-boot:run
```

Expected startup log:
```
Started NotificationServiceApplication on port 8086
```

---

## 4 — API Gateway Update

Replace only `GatewayConfig.java` in your api-gateway with the
updated `GatewayConfig.java` provided in this zip.

It adds two new routes:

| Route ID                  | Path                                                                 | Auth | Target |
|--------------------------|----------------------------------------------------------------------|------|--------|
| notification-internal    | /notifications, /notifications/bulk, /notifications/email, /notifications/push/** | None | :8086 |
| notification-protected   | /notifications/**                                                    | JWT  | :8086 |

---

## 5 — Postman Testing (run 0 → 16 in order)

1. Import `ConnectHub_Notification_Service.postman_collection.json`
2. Run **request 0** first → saves token + userId automatically
3. Then run in sequence:

```
0.  Login              → saves token + userId
1.  Send NEW_MESSAGE   → saves notificationId
2.  Send MENTION
3.  Send ROOM_INVITE
4.  Send SYSTEM
5.  Get By Recipient   → should return 4+ items
6.  Get Unread Count   → should be >= 4
7.  Send Bulk          → 2 recipients
8.  Send Email         → logs to console (email.enabled=false)
9.  Send Push          → logs to console (fcm.enabled=false)
10. Mark Single Read   → uses saved notificationId
11. Get Unread Count   → decreased by 1
12. Mark All Read
13. Get Unread Count   → 0
14. Delete Notification
15. Get All (admin)
16. Missing Fields     → expect 400
```

---

## 6 — MySQL Verification

```sql
USE connecthub_notification;

-- All notifications
SELECT * FROM notifications ORDER BY created_at DESC;

-- Unread count per user
SELECT recipient_id, COUNT(*) AS unread
FROM notifications WHERE is_read = 0
GROUP BY recipient_id;

-- By type
SELECT type, COUNT(*) FROM notifications GROUP BY type;
```

---

## 7 — Frontend Integration

**File destinations in your Angular project:**

| File | Destination |
|------|-------------|
| `notification.service.ts` | `src/app/core/services/` |
| `notification-bell.component.ts` | `src/app/shared/components/` |

**Add the bell to your navbar:**

```html
<!-- In your navbar/layout component template -->
<app-notification-bell [userId]="currentUser.userId" />
```

**Import in your navbar component:**

```typescript
import { NotificationBellComponent } from '../../shared/components/notification-bell.component';

@Component({
  imports: [NotificationBellComponent],
  ...
})
```

**Refresh badge after receiving a STOMP message event:**

```typescript
// In your WebSocket message handler
this.notificationService.refreshUnreadCount(this.currentUserId);
```

---

## 8 — Endpoints Summary

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /notifications | No (internal) | Send single notification |
| POST | /notifications/bulk | No (internal) | Send to multiple users |
| POST | /notifications/email | No (internal) | Send email |
| POST | /notifications/push/{recipientId} | No (internal) | Send FCM push |
| GET | /notifications/recipient/{id} | JWT | Get all for user |
| GET | /notifications/recipient/{id}/unread-count | JWT | Unread badge count |
| PUT | /notifications/{id}/read | JWT | Mark one as read |
| PUT | /notifications/recipient/{id}/read-all | JWT | Mark all as read |
| DELETE | /notifications/{id} | JWT | Delete one |
| GET | /notifications/all | JWT | All notifications (admin) |

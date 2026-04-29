# ConnectHub — WebSocket Handler (Service 7)

## What this service does
Real-time messaging core — the Java equivalent of Socket.io.
Manages STOMP WebSocket sessions, routes all real-time frames, and
coordinates with message-service, presence-service, and notification-service.

---

## Port
| Service               | Port |
|----------------------|------|
| api-gateway           | 8080 |
| auth-service          | 8081 |
| room-service          | 8082 |
| message-service       | 8083 |
| media-service         | 8084 |
| presence-service      | 8085 |
| notification-service  | 8086 |
| **websocket-handler** | **8087** |

---

## 1 — No Database Needed
This service has NO database — it is purely in-memory (ConcurrentHashMap for sessions)
plus REST calls to other services. No schema.sql required.

---

## 2 — Build & Run

```bash
cd websocket-handler
mvn clean install -DskipTests
mvn spring-boot:run
# Expected: Started WebSocketHandlerApplication on port 8087
```

---

## 3 — API Gateway Update
Replace `GatewayConfig.java` with the updated file in `docs/GatewayConfig.java`.
Adds two new routes:
- `/ws/**`        → public (SockJS handshake, JWT in STOMP CONNECT)
- `/ws-stats/**`  → JWT protected (admin dashboard)

---

## 4 — STOMP Subscription Topics (PDF Section 4.7)

| Topic | Description |
|-------|-------------|
| `/topic/room/{roomId}` | All events for a room |
| `/topic/user/{userId}` | Personal alerts and pong responses |
| `/topic/presence`      | Global online/offline broadcasts |

---

## 5 — Inbound STOMP Endpoints

| Destination | Frame Type | Description |
|-------------|-----------|-------------|
| `/app/chat.send`   | CHAT_MESSAGE     | Send a message |
| `/app/chat.typing` | TYPING_INDICATOR | Typing indicator |
| `/app/chat.read`   | READ_RECEIPT     | Mark messages read |
| `/app/chat.react`  | REACTION         | Emoji reaction |
| `/app/chat.edit`   | MESSAGE_EDIT     | Edit a message |
| `/app/chat.delete` | MESSAGE_DELETE   | Delete a message |
| `/app/chat.status` | PRESENCE_UPDATE  | Change status |
| `/app/chat.join`   | ROOM_JOIN        | Join a room |
| `/app/chat.leave`  | ROOM_LEAVE       | Leave a room |
| `/app/chat.ping`   | PING             | Keep-alive |

---

## 6 — REST Endpoint

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /ws-stats/connections | JWT | Active WebSocket connection count |

---

## 7 — Frontend Integration

**File destination:**

| File | Path |
|------|------|
| `websocket.service.ts` | `src/app/core/services/websocket.service.ts` |

**Install required npm packages:**
```bash
npm install @stomp/stompjs sockjs-client
npm install --save-dev @types/sockjs-client
```

**Wire into chat-page.component.ts:**

```typescript
// On login / component init
this.wsService.connect(this.authService.getToken(), this.user.userId);

// When user selects a room
this.wsService.subscribeToRoom(roomId);

// Listen for real-time events
this.wsService.roomEvent$.subscribe(event => {
  if (!event) return;
  switch (event.type) {
    case 'CHAT_MESSAGE':
      this.messages.push(event);
      break;
    case 'TYPING_INDICATOR':
      this.showTyping(event.senderId, event.isTyping);
      break;
    case 'READ_RECEIPT':
      this.updateReadStatus(event.upToMessageId);
      break;
    case 'MESSAGE_EDIT':
      this.updateMessage(event.messageId, event.newContent);
      break;
    case 'MESSAGE_DELETE':
      this.markDeleted(event.messageId);
      break;
    case 'REACTION':
      this.addReaction(event.messageId, event.emoji);
      break;
  }
});

// Send a message via STOMP instead of REST
this.wsService.sendMessage(roomId, content);

// Typing indicator
this.wsService.sendTyping(roomId, true);

// On logout
this.wsService.disconnect();
```

---

## 8 — Full Service Startup Order

```
1. auth-service          (8081)
2. room-service          (8082)
3. message-service       (8083)
4. media-service         (8084)
5. presence-service      (8085)
6. notification-service  (8086)
7. websocket-handler     (8087)  ← this service
8. api-gateway           (8080)  ← always last
```

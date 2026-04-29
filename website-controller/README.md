# connecthub-web — 8th Microservice (MVC Layer)

## What is this?
connecthub-web is the **Website Controller / MVC Layer** for ConnectHub.  
It sits between the Angular frontend and the backend microservices, acting as an **aggregation & routing layer**.

```
Angular (4200)
     │
     ▼
api-gateway (8080)  ← /web/** routed here
     │
     ▼
connecthub-web (8088)  ← This service
     │
     ├── auth-service     (8081)
     ├── room-service     (8082)
     ├── message-service  (8083)
     ├── media-service    (8084)
     ├── presence-service (8085)
     ├── notification-service (8086)
     └── websocket-handler (8087)
```

## Package Structure
```
com.ConnectHub.connecthub_web
├── ConnectHubWebApplication.java       ← @SpringBootApplication
├── config/
│   ├── SecurityConfig.java             ← CORS + JWT security
│   ├── JwtAuthFilter.java              ← Validates Bearer token or X-User-Id header from gateway
│   ├── JwtUtil.java                    ← Same JWT secret as auth-service
│   ├── RestTemplateConfig.java         ← RestTemplate bean
│   └── ServiceProperties.java          ← Downstream URL config
└── controller/
    ├── ChatController.java             ← /web/** user-facing endpoints + /web/dashboard aggregation
    ├── RoomManagerController.java      ← /web/room-manager/** room-admin endpoints
    └── AdminController.java            ← /web/admin/** platform admin endpoints (ROLE_ADMIN only)
```

## Running the Service
```bash
# From connecthub-web directory
mvn spring-boot:run

# Or build and run jar
mvn clean package -DskipTests
java -jar target/connecthub-web-0.0.1-SNAPSHOT.jar
```

## Environment Variables (optional overrides)
| Variable              | Default                       |
|-----------------------|-------------------------------|
| AUTH_JWT_SECRET       | connecthub-dev-secret-key...  |
| AUTH_SERVICE_URL      | http://localhost:8081         |
| ROOM_SERVICE_URL      | http://localhost:8082         |
| MESSAGE_SERVICE_URL   | http://localhost:8083         |
| MEDIA_SERVICE_URL     | http://localhost:8084         |
| PRESENCE_SERVICE_URL  | http://localhost:8085         |
| NOTIFICATION_SERVICE_URL | http://localhost:8086      |
| CORS_ORIGINS          | http://localhost:4200         |

## Key Endpoints

### ChatController `/web/**`
| Method | Path | Description |
|--------|------|-------------|
| POST | /web/auth/register | Register (public) |
| POST | /web/auth/login | Login (public) |
| GET | /web/dashboard/{userId} | **Aggregated** profile+rooms+presence+notifs |
| POST | /web/rooms | Create room |
| GET | /web/rooms/user/{userId} | Get user's rooms |
| POST | /web/messages | Send message |
| GET | /web/messages/room/{roomId} | Paginated history |
| GET | /web/presence/{userId} | Get presence |
| GET | /web/notifications/{userId} | Get notifications |

### RoomManagerController `/web/room-manager/**`
| Method | Path | Description |
|--------|------|-------------|
| PUT | /rooms/{roomId} | Update room settings |
| DELETE | /messages/{messageId} | Moderate: delete any message |
| DELETE | /rooms/{roomId}/history | Clear all room messages |
| POST | /rooms/{roomId}/pin/{messageId} | Pin message |
| DELETE | /rooms/{roomId}/pin/{messageId} | Unpin message |

### AdminController `/web/admin/**`  _(ROLE_ADMIN)_
| Method | Path | Description |
|--------|------|-------------|
| GET | /web/admin/dashboard | Aggregated admin dashboard |
| GET | /web/admin/users | All users |
| PUT | /web/admin/users/{id}/suspend | Suspend user |
| GET | /web/admin/analytics | Platform analytics |
| POST | /web/admin/broadcast | Send platform-wide notification |

## API Gateway Integration
Replace **api-gateway/src/main/resources/application.properties** with the updated version in
`api-gateway-update/src/main/resources/application.properties`.

The new route added:
```properties
spring.cloud.gateway.routes[0].id=connecthub-web
spring.cloud.gateway.routes[0].uri=http://localhost:8088
spring.cloud.gateway.routes[0].predicates[0]=Path=/web/**
```

## Frontend Integration
Copy `web.service.ts` to your Angular project:
```
src/app/core/services/web.service.ts
```
Inject and use `WebService` in any component for MVC-layer calls.  
The `getDashboard(userId)` call is especially useful on app load — one HTTP request replaces 4.

## Postman Testing
1. Import `ConnectHub_Web_Postman_Collection.json`
2. Run **Login** first — token is auto-saved to collection variable `{{token}}`
3. Run requests in order 1→2→3→4...

## SQL Verification
Run `connecthub_web_sql_verification.sql` in MySQL Workbench.  
Replace placeholder UUIDs with actual values from Postman responses.

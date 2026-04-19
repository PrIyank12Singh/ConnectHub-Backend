# Auth/User-Service — ConnectHub Microservice

## Overview
The Auth/User-Service is the identity and profile management microservice for ConnectHub, implementing JWT-based authentication, user profile management, and status tracking.

## Architecture

### Layers
- **Model (Entity)**: `User.java` — User domain object with JPA persistence
- **Repository**: `UserRepository.java` — Spring Data JPA interface for data access
- **Service Interface**: `AuthService.java` — Business contract
- **Service Implementation**: `AuthServiceImpl.java` — Business logic (bcrypt hashing, JWT generation, profile updates)
- **REST Resource**: `AuthResource.java` — HTTP endpoints at `/auth/**`
- **Security**: `JwtUtil.java` — JWT token generation/validation
- **Config**: `PasswordConfig.java`, `SecurityConfig.java` — Spring Security setup

### Data Model

**User Entity**
```
- userId (UUID, primary key)
- username (String, unique)
- email (String, unique)
- passwordHash (bcrypt-encoded)
- fullName (String)
- avatarUrl (String)
- bio (String)
- status (Enum: ONLINE, AWAY, DND, INVISIBLE)
- provider (Enum: LOCAL, GOOGLE, GITHUB)
- role (Enum: USER, ROOM_ADMIN, PLATFORM_ADMIN)
- isActive (Boolean)
- lastSeenAt (LocalDateTime)
- createdAt (LocalDateTime, immutable)
- updatedAt (LocalDateTime)
```

## REST Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/auth/register` | POST | Register new user |
| `/auth/login` | POST | Login and receive JWT token |
| `/auth/logout` | POST | Logout and record last-seen |
| `/auth/validate` | POST | Validate JWT token |
| `/auth/refresh` | POST | Refresh JWT token |
| `/auth/profile/{userId}` | GET | Retrieve user profile |
| `/auth/profile/{userId}` | PUT | Update profile (name, avatar, bio) |
| `/auth/password/{userId}` | PUT | Change password |
| `/auth/search` | GET | Search users by username |
| `/auth/status/{userId}` | PUT | Update user status |

## Request/Response DTOs

- **RegisterRequest**: `username`, `email`, `password`, `fullName`
- **LoginRequest**: `email`, `password`
- **AuthResponse**: `userId`, `username`, `email`, `role`, `accessToken`, `tokenType`, `issuedAt`
- **UserResponse**: Full user details (profile view)
- **UpdateProfileRequest**: `fullName`, `avatarUrl`, `bio`
- **ChangePasswordRequest**: `currentPassword`, `newPassword`

## Configuration

**application.properties (Development)**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/connecthub_auth
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update

auth.jwt.secret=<32+ byte secret key>
auth.jwt.access-expiration-minutes=60
```

**Environment Variables**
- `AUTH_DB_URL` — Database JDBC URL
- `AUTH_DB_USERNAME` — Database username
- `AUTH_DB_PASSWORD` — Database password
- `AUTH_JWT_SECRET` — JWT signing key (min 32 bytes for HS256)
- `AUTH_JWT_ACCESS_EXPIRATION_MINUTES` — Token expiration (default 60)

## Dependencies

- Spring Boot 4.0.5
- Spring Data JPA + Hibernate ORM
- Spring Security (password encoding)
- JJWT 0.12.6 (JWT library)
- MySQL Connector/J
- Lombok (code generation)
- H2 Database (for testing)

## Building & Running

**Build (with tests)**
```bash
cd auth-service
mvn clean test
```

**Build (skip tests)**
```bash
cd auth-service
mvn clean package -DskipTests
```

**Run locally**
```bash
mvn spring-boot:run
```

**Docker**
```bash
docker build -t connecthub/auth-service:latest .
docker run -p 8080:8080 -e AUTH_JWT_SECRET=<secret> connecthub/auth-service:latest
```

## Database Setup

Create MySQL database and tables:
```sql
CREATE DATABASE connecthub_auth;
USE connecthub_auth;

CREATE TABLE users (
    user_id CHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    avatar_url VARCHAR(500),
    bio VARCHAR(400),
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_status ON users(status);
```

## Security Notes

- **JWT Tokens**: Issued on login, passed as `Authorization: Bearer <token>` header
- **Token Validation**: On every WebSocket upgrade (see ChatWebSocketHandler)
- **Password Hashing**: BCrypt with Spring Security's PasswordEncoder
- **CORS**: Should be configured at API Gateway level
- **HTTPS**: Use in production

## Integration Points

- **WebSocket Handler** (`ChatWebSocketHandler`): Calls `recordLastSeen()` on disconnect
- **Presence Service**: Calls `setOnline()` / `setOffline()` on connection events
- **Notification Service**: Sends profile update events to subscribed clients
- **Room Service**: User search used when adding members to rooms

## Testing

Default test suite:
- `AuthServiceApplicationTests.contextLoads()` — Verifies Spring context startup

Recommended additional tests:
- User registration with duplicate email/username
- Login with invalid credentials
- JWT token validation and refresh
- Password change verification
- Profile update isolation

## Status

✅ **Build**: SUCCESS
- Compilation: 18 source files compiled
- Package: JAR created at `target/auth-service-0.0.1-SNAPSHOT.jar`
- Dependencies: Resolved and downloaded

---

**Version**: 1.0  
**Last Updated**: 2026-04-18  
**Author**: ConnectHub Platform Team

package com.connecthub.presence_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores the live presence record for each connected WebSocket session.
 * One user can have multiple sessions (desktop + mobile) — each session
 * gets its own row keyed by sessionId.
 */
@Getter
@Setter
@Entity
@Table(
    name = "user_presence",
    indexes = {
        @Index(name = "idx_presence_user_id",  columnList = "user_id"),
        @Index(name = "idx_presence_session",  columnList = "session_id"),
        @Index(name = "idx_presence_status",   columnList = "status")
    }
)
public class UserPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "presence_id")
    private Long presenceId;

    /** UUID string — matches auth-service User.userId */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /** Optional free-text status message, e.g. "In a meeting" */
    @Column(name = "custom_message", length = 160)
    private String customMessage;

    /** WEB, MOBILE, DESKTOP */
    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    /** Updated every 30 s by the client ping; used to detect stale sessions */
    @Column(name = "last_ping_at", nullable = false)
    private LocalDateTime lastPingAt;

    /** STOMP WebSocket session ID — unique per connection */
    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    private String sessionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.connectedAt = now;
        this.lastPingAt = now;
        if (this.status == null) {
            this.status = UserStatus.ONLINE;
        }
        if (this.deviceType == null) {
            this.deviceType = "WEB";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}



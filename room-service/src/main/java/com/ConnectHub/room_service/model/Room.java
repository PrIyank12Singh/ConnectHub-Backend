package com.ConnectHub.room_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "room_id", nullable = false, updatable = false, length = 36)
    private UUID roomId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoomType type;

    @Column(nullable = false)
    private UUID createdById;

    @Column(length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    private Boolean isPrivate;

    @Column(nullable = false)
    private Integer maxMembers;

    private LocalDateTime lastMessageAt;

    // ─── GAP 10 FIX ───────────────────────────────────────────────────────────
    /**
     * UUID of the message currently pinned to the top of this room.
     * Null when no message is pinned.
     * Set/cleared via POST|DELETE /rooms/{roomId}/pin/{messageId}
     */
    @Column(length = 36)
    private String pinnedMessageId;
    // ─────────────────────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isPrivate == null) {
            this.isPrivate = this.type == RoomType.DM;
        }
        if (this.maxMembers == null) {
            this.maxMembers = this.type == RoomType.DM ? 2 : 100;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

package com.connecthub.notification_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notif_recipient",       columnList = "recipient_id"),
        @Index(name = "idx_notif_recipient_read",  columnList = "recipient_id, is_read"),
        @Index(name = "idx_notif_type",            columnList = "type"),
        @Index(name = "idx_notif_room",            columnList = "room_id"),
        @Index(name = "idx_notif_created",         columnList = "created_at")
    }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    /** userId of the user who will receive this notification */
    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    /** userId of the user who triggered the notification (null for SYSTEM) */
    @Column(name = "actor_id", length = 36)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    /** Room context (nullable for SYSTEM notifications) */
    @Column(name = "room_id", length = 36)
    private String roomId;

    /** Message context (nullable) */
    @Column(name = "message_id", length = 36)
    private String messageId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
}



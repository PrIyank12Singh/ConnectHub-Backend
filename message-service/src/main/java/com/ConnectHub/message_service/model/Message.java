package com.ConnectHub.message_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "message_id", nullable = false, updatable = false, length = 36)
    private UUID messageId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 36)
    private UUID roomId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 36)
    private UUID senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageType type;

    @Column(length = 1000)
    private String mediaUrl;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID replyToMessageId;

    @Column(nullable = false)
    private Boolean isEdited;

    @Column(nullable = false)
    private Boolean isDeleted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeliveryStatus deliveryStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private LocalDateTime editedAt;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
        if (this.type == null) {
            this.type = MessageType.TEXT;
        }
        if (this.isEdited == null) {
            this.isEdited = false;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.deliveryStatus == null) {
            this.deliveryStatus = DeliveryStatus.SENT;
        }
    }
}

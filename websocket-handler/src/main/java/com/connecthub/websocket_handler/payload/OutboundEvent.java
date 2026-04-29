package com.connecthub.websocket_handler.payload;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound frame broadcast to /topic/room/{roomId} or /topic/user/{userId}.
 * All fields are populated depending on the event type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundEvent {

    private MessageType type;

    // ── Common ────────────────────────────────────────────────────
    private String roomId;
    private String userId;
    private LocalDateTime timestamp;

    // ── CHAT_MESSAGE ──────────────────────────────────────────────
    private String messageId;
    private String senderId;
    private String content;
    private String messageType;   // TEXT | IMAGE | FILE
    private String mediaUrl;
    private String replyToId;
    private String deliveryStatus;

    // ── TYPING_INDICATOR ─────────────────────────────────────────
    private Boolean isTyping;

    // ── READ_RECEIPT ─────────────────────────────────────────────
    private String readerId;
    private String upToMessageId;

    // ── REACTION ─────────────────────────────────────────────────
    private String emoji;

    // ── PRESENCE_UPDATE ──────────────────────────────────────────
    private String status;
    private String customMessage;

    // ── MESSAGE_EDIT ─────────────────────────────────────────────
    private String newContent;
    private Boolean isEdited;

    // ── MESSAGE_DELETE ────────────────────────────────────────────
    private Boolean isDeleted;

    // ── ROOM_JOIN / ROOM_LEAVE ────────────────────────────────────
    private String username;
}



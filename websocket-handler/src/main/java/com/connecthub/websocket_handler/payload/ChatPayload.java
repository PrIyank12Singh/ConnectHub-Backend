package com.ConnectHub.websocket_handler.payload;

import lombok.Data;

/**
 * Universal inbound STOMP frame — all fields are optional depending on type.
 * The handler reads the 'type' field first then processes the relevant fields.
 *
 * Examples from PDF Section 4.7:
 *
 * CHAT_MESSAGE:
 *   { "type":"CHAT_MESSAGE", "senderId":"uuid", "roomId":"uuid",
 *     "content":"Hello!", "messageType":"TEXT", "replyToId":null }
 *
 * TYPING_INDICATOR:
 *   { "type":"TYPING_INDICATOR", "senderId":"uuid", "roomId":"uuid", "isTyping":true }
 *
 * READ_RECEIPT:
 *   { "type":"READ_RECEIPT", "readerId":"uuid", "roomId":"uuid", "upToMessageId":"uuid" }
 *
 * REACTION:
 *   { "type":"REACTION", "senderId":"uuid", "messageId":"uuid", "emoji":"👍" }
 *
 * PRESENCE_UPDATE:
 *   { "type":"PRESENCE_UPDATE", "userId":"uuid", "status":"AWAY", "customMessage":"In a meeting" }
 *
 * MESSAGE_EDIT:
 *   { "type":"MESSAGE_EDIT", "editorId":"uuid", "messageId":"uuid", "newContent":"edited text" }
 *
 * MESSAGE_DELETE:
 *   { "type":"MESSAGE_DELETE", "deleterId":"uuid", "messageId":"uuid" }
 */
@Data
public class ChatPayload {

    private MessageType type;

    // ── CHAT_MESSAGE fields ──────────────────────────────────────
    private String senderId;
    private String roomId;
    private String content;
    private String messageType;   // TEXT | IMAGE | FILE
    private String mediaUrl;
    private String replyToId;

    // ── TYPING_INDICATOR fields ──────────────────────────────────
    private Boolean isTyping;

    // ── READ_RECEIPT fields ──────────────────────────────────────
    private String readerId;
    private String upToMessageId;

    // ── REACTION fields ─────────────────────────────────────────
    private String messageId;
    private String emoji;

    // ── PRESENCE_UPDATE fields ───────────────────────────────────
    private String userId;
    private String status;
    private String customMessage;

    // ── MESSAGE_EDIT fields ──────────────────────────────────────
    private String editorId;
    private String newContent;

    // ── MESSAGE_DELETE fields ────────────────────────────────────
    private String deleterId;

    // ── ROOM_JOIN / ROOM_LEAVE fields ────────────────────────────
    // uses userId + roomId (already declared above)

    // ── PING fields ──────────────────────────────────────────────
    private String sessionId;
}

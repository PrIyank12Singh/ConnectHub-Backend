package com.connecthub.websocket_handler.handler;

import com.connecthub.websocket_handler.client.MessageServiceClient;
import com.connecthub.websocket_handler.client.NotificationServiceClient;
import com.connecthub.websocket_handler.client.PresenceServiceClient;
import com.connecthub.websocket_handler.payload.ChatPayload;
import com.connecthub.websocket_handler.payload.MessageType;
import com.connecthub.websocket_handler.payload.OutboundEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.task.TaskExecutor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * ChatWebSocketHandler — the real-time messaging core of ConnectHub.
 * Java equivalent of Socket.io's event emitter (PDF Section 4.7).
 *
 * Session tracking:
 *   sessions     : sessionId → userId
 *   userSessions : userId → Set of sessionIds (multi-device support)
 *
 * Routes inbound /app/** STOMP frames:
 *   /app/chat.send   → handleChatMessage()
 *   /app/chat.typing → handleTypingIndicator()
 *   /app/chat.read   → handleReadReceipt()
 *   /app/chat.react  → handleReaction()
 *   /app/chat.edit   → handleMessageEdit()
 *   /app/chat.delete → handleMessageDelete()
 *   /app/chat.status → handlePresenceUpdate()
 *   /app/chat.join   → handleRoomJoin()
 *   /app/chat.leave  → handleRoomLeave()
 *   /app/chat.ping   → handlePing()
 *
 * Broadcasts to:
 *   /topic/room/{roomId}   — all events scoped to a room
 *   /topic/user/{userId}   — personal notifications
 *   /topic/presence        — global presence updates
 */
@Controller
public class ChatWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    // ─── Session Registry (ConcurrentHashMap — thread-safe) ──────────────────
    /** sessionId → userId */
    private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();
    /** userId → Set of sessionIds (supports multi-device) */
    private final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageServiceClient messageClient;
    private final PresenceServiceClient presenceClient;
    private final NotificationServiceClient notificationClient;
    private final ObjectMapper objectMapper;
    private final TaskExecutor broadcastExecutor;

    public ChatWebSocketHandler(
            SimpMessagingTemplate messagingTemplate,
            MessageServiceClient messageClient,
            PresenceServiceClient presenceClient,
            NotificationServiceClient notificationClient,
            TaskExecutor broadcastExecutor) {
        this.messagingTemplate = messagingTemplate;
        this.messageClient = messageClient;
        this.presenceClient = presenceClient;
        this.notificationClient = notificationClient;
        this.broadcastExecutor = broadcastExecutor;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    // =========================================================================
    // ── afterConnectionEstablished (called by WebSocketConfig interceptor) ───
    // =========================================================================

    /**
     * Called when a WebSocket session is established.
     * Registered via WebSocketConfig's ChannelInterceptor on STOMP CONNECT.
     * The userId was already validated and stored in session attributes.
     */
    public void onSessionConnected(String sessionId, String userId) {
        sessions.put(sessionId, userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // ── Call presence-service: set user ONLINE ──
        presenceClient.setOnline(userId, sessionId, "WEB");

        // ── Broadcast PRESENCE_UPDATE to all rooms ──
        broadcastPresence(userId, "ONLINE", null);

        log.info("WS CONNECT → user: {} session: {}", userId, sessionId);
    }

    // =========================================================================
    // ── afterConnectionClosed ────────────────────────────────────────────────
    // =========================================================================

    /**
     * Called when a WebSocket session is closed.
     */
    public void onSessionDisconnected(String sessionId) {
        String userId = sessions.remove(sessionId);
        if (userId != null) {
            Set<String> userSess = userSessions.get(userId);
            if (userSess != null) {
                userSess.remove(sessionId);
                if (userSess.isEmpty()) {
                    userSessions.remove(userId);
                    // ── Only broadcast offline if no other sessions remain ──
                    broadcastPresence(userId, "OFFLINE", null);
                }
            }
            // ── Call presence-service: remove this session ──
            presenceClient.setOfflineBySession(sessionId);
            log.info("WS DISCONNECT → user: {} session: {}", userId, sessionId);
        }
    }

    // =========================================================================
    // ── /app/chat.send → CHAT_MESSAGE ────────────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.send")
    public void handleChatMessage(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        String userId    = getUserIdFromSession(sessionId);
        if (userId == null) return;

        // 1. Persist via message-service
        // Sanitize inbound content to prevent XSS
        if (payload.getContent() != null) {
            payload.setContent(Jsoup.clean(payload.getContent(), Safelist.basic()));
        }
        if (payload.getMediaUrl() != null) {
            payload.setMediaUrl(Jsoup.clean(payload.getMediaUrl(), Safelist.basic()));
        }

        Map<String, Object> saved = messageClient.sendMessage(
                payload.getRoomId(),
                userId,
                payload.getContent(),
                payload.getMessageType() != null ? payload.getMessageType() : "TEXT",
                payload.getMediaUrl(),
                payload.getReplyToId());

        if (saved == null) {
            log.error("Failed to persist message from user {}", userId);
            return;
        }

        // 2. Build outbound event
        OutboundEvent event = OutboundEvent.builder()
                .type(MessageType.CHAT_MESSAGE)
                .messageId(String.valueOf(saved.get("messageId")))
                .roomId(payload.getRoomId())
                .senderId(userId)
                .content(payload.getContent())
                .messageType(payload.getMessageType() != null ? payload.getMessageType() : "TEXT")
                .mediaUrl(payload.getMediaUrl())
                .replyToId(payload.getReplyToId())
                .deliveryStatus("SENT")
                .timestamp(LocalDateTime.now())
                .build();

        // 3. Broadcast to /topic/room/{roomId}
        broadcastToRoom(payload.getRoomId(), event);

        // 4. Check mentions (@username in content) and notify
        if (payload.getContent() != null && payload.getContent().contains("@")) {
            // Simplified: in production resolve username → userId from auth-service
            log.info("[WS] Mention detected in message from user {}", userId);
        }

        log.info("[WS] CHAT_MESSAGE from user {} in room {}", userId, payload.getRoomId());
    }

    // =========================================================================
    // ── /app/chat.typing → TYPING_INDICATOR ──────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = getUserIdFromSession(headerAccessor.getSessionId());
        if (userId == null) return;

        // Typing events are NOT persisted — broadcast only to connected subscribers
        OutboundEvent event = OutboundEvent.builder()
                .type(MessageType.TYPING_INDICATOR)
                .roomId(payload.getRoomId())
                .senderId(userId)
                .isTyping(payload.getIsTyping() != null ? payload.getIsTyping() : true)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(payload.getRoomId(), event);
    }

    // =========================================================================
    // ── /app/chat.read → READ_RECEIPT ────────────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.read")
    public void handleReadReceipt(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = getUserIdFromSession(headerAccessor.getSessionId());
        if (userId == null) return;

        // Update delivery status to READ in message-service
        if (payload.getUpToMessageId() != null) {
            messageClient.updateDeliveryStatus(payload.getUpToMessageId(), "READ");
        }

        OutboundEvent event = OutboundEvent.builder()
                .type(MessageType.READ_RECEIPT)
                .roomId(payload.getRoomId())
                .readerId(userId)
                .upToMessageId(payload.getUpToMessageId())
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(payload.getRoomId(), event);
        log.debug("[WS] READ_RECEIPT from user {} up to msg {}", userId, payload.getUpToMessageId());
    }

    // =========================================================================
    // ── /app/chat.react → REACTION ───────────────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.react")
    public void handleReaction(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = getUserIdFromSession(headerAccessor.getSessionId());
        if (userId == null) return;

        OutboundEvent event = OutboundEvent.builder()
                .type(MessageType.REACTION)
                .roomId(payload.getRoomId())
                .senderId(userId)
                .messageId(payload.getMessageId())
                .emoji(payload.getEmoji())
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(payload.getRoomId(), event);
        log.debug("[WS] REACTION {} from user {} on message {}", payload.getEmoji(), userId, payload.getMessageId());
    }

    // =========================================================================
    // ── /app/chat.edit → MESSAGE_EDIT ────────────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.edit")
    public void handleMessageEdit(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = getUserIdFromSession(headerAccessor.getSessionId());
        if (userId == null) return;

        // Persist edit via message-service
        messageClient.editMessage(payload.getMessageId(), payload.getNewContent());

        OutboundEvent event = OutboundEvent.builder()
                .type(MessageType.MESSAGE_EDIT)
                .roomId(payload.getRoomId())
                .messageId(payload.getMessageId())
                .newContent(payload.getNewContent())
                .isEdited(true)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(payload.getRoomId(), event);
        log.info("[WS] MESSAGE_EDIT by user {} on message {}", userId, payload.getMessageId());
    }

    // =========================================================================
    // ── /app/chat.delete → MESSAGE_DELETE ────────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.delete")
    public void handleMessageDelete(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = getUserIdFromSession(headerAccessor.getSessionId());
        if (userId == null) return;

        // Soft-delete via message-service
        messageClient.deleteMessage(payload.getMessageId());

        OutboundEvent event = OutboundEvent.builder()
                .type(MessageType.MESSAGE_DELETE)
                .roomId(payload.getRoomId())
                .messageId(payload.getMessageId())
                .isDeleted(true)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(payload.getRoomId(), event);
        log.info("[WS] MESSAGE_DELETE by user {} on message {}", userId, payload.getMessageId());
    }

    // =========================================================================
    // ── /app/chat.status → PRESENCE_UPDATE ───────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.status")
    public void handlePresenceUpdate(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = getUserIdFromSession(headerAccessor.getSessionId());
        if (userId == null) return;

        // Update via presence-service
        presenceClient.updateStatus(userId, payload.getStatus(), payload.getCustomMessage());

        // Broadcast to /topic/presence so all clients update their indicators
        broadcastPresence(userId, payload.getStatus(), payload.getCustomMessage());

        log.info("[WS] PRESENCE_UPDATE user {} → {}", userId, payload.getStatus());
    }

    // =========================================================================
    // ── /app/chat.join → ROOM_JOIN ────────────────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.join")
    public void handleRoomJoin(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = getUserIdFromSession(headerAccessor.getSessionId());
        if (userId == null) return;

        OutboundEvent event = OutboundEvent.builder()
                .type(MessageType.ROOM_JOIN)
                .roomId(payload.getRoomId())
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(payload.getRoomId(), event);
        log.info("[WS] ROOM_JOIN user {} joined room {}", userId, payload.getRoomId());
    }

    // =========================================================================
    // ── /app/chat.leave → ROOM_LEAVE ─────────────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.leave")
    public void handleRoomLeave(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = getUserIdFromSession(headerAccessor.getSessionId());
        if (userId == null) return;

        OutboundEvent event = OutboundEvent.builder()
                .type(MessageType.ROOM_LEAVE)
                .roomId(payload.getRoomId())
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToRoom(payload.getRoomId(), event);
        log.info("[WS] ROOM_LEAVE user {} left room {}", userId, payload.getRoomId());
    }

    // =========================================================================
    // ── /app/chat.ping → PING (keep-alive) ───────────────────────────────────
    // =========================================================================

    @MessageMapping("/chat.ping")
    public void handlePing(
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            presenceClient.ping(sessionId);
        }
        // Send pong back to this user's personal queue
        String userId = getUserIdFromSession(sessionId);
        if (userId != null) {
            sendToUser(userId, OutboundEvent.builder()
                    .type(MessageType.PING)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    // =========================================================================
    // ── Broadcast helpers ────────────────────────────────────────────────────
    // =========================================================================

    /** Broadcast to all subscribers of /topic/room/{roomId} */
    public void broadcastToRoom(String roomId, OutboundEvent event) {
        broadcastExecutor.execute(() -> messagingTemplate.convertAndSend("/topic/room/" + roomId, event));
    }

    /** Send to a specific user's personal queue /topic/user/{userId} */
    public void sendToUser(String userId, OutboundEvent event) {
        broadcastExecutor.execute(() -> messagingTemplate.convertAndSend("/topic/user/" + userId, event));
    }

    /** Broadcast presence update to /topic/presence */
    public void broadcastPresence(String userId, String status, String customMessage) {
        OutboundEvent event = OutboundEvent.builder()
                .type(MessageType.PRESENCE_UPDATE)
                .userId(userId)
                .status(status)
                .customMessage(customMessage)
                .timestamp(LocalDateTime.now())
                .build();
        broadcastExecutor.execute(() -> messagingTemplate.convertAndSend("/topic/presence", event));
    }

    // =========================================================================
    // ── Session helpers ──────────────────────────────────────────────────────
    // =========================================================================

    private String getUserIdFromSession(String sessionId) {
        if (sessionId == null) return null;
        String userId = sessions.get(sessionId);
        if (userId == null) {
            log.warn("[WS] No userId found for session {} — frame ignored", sessionId);
        }
        return userId;
    }

    /** Returns true if the user has at least one active WebSocket session */
    public boolean isUserConnected(String userId) {
        Set<String> userSess = userSessions.get(userId);
        return userSess != null && !userSess.isEmpty();
    }

    /** Active connection count — used by admin dashboard */
    public int getActiveConnectionCount() {
        return sessions.size();
    }
}



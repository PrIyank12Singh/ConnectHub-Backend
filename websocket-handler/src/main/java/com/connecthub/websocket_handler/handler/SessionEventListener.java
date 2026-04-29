package com.connecthub.websocket_handler.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens to Spring's STOMP session lifecycle events.
 * Bridges Spring's event system → ChatWebSocketHandler.
 *
 * afterConnectionEstablished → onSessionConnected()
 * afterConnectionClosed      → onSessionDisconnected()
 */
@Component
public class SessionEventListener {

    private static final Logger log = LoggerFactory.getLogger(SessionEventListener.class);

    private final ChatWebSocketHandler chatHandler;

    public SessionEventListener(ChatWebSocketHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    // ─── STOMP CONNECT ────────────────────────────────────────────────────────

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // userId was stored in session attributes by WebSocketConfig JWT interceptor
        Object userIdObj = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get("userId")
                : null;

        if (sessionId != null && userIdObj != null) {
            String userId = userIdObj.toString();
            chatHandler.onSessionConnected(sessionId, userId);
            log.info("[SessionEvent] CONNECTED → sessionId: {} userId: {}", sessionId, userId);
        } else {
            log.warn("[SessionEvent] CONNECTED but no userId in session attributes — sessionId: {}", sessionId);
        }
    }

    // ─── STOMP DISCONNECT ─────────────────────────────────────────────────────

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId != null) {
            chatHandler.onSessionDisconnected(sessionId);
            log.info("[SessionEvent] DISCONNECTED → sessionId: {}", sessionId);
        }
    }
}



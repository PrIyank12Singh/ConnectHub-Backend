package com.connecthub.websocket_handler.client;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Calls presence-service (port 8085) on WebSocket connect/disconnect/ping.
 */
@Component
public class PresenceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(PresenceServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${presence.service.url:http://localhost:8085}")
    private String presenceServiceUrl;

    public PresenceServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    /** Called on afterConnectionEstablished */
    public void setOnline(String userId, String sessionId, String deviceType) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("sessionId", sessionId);
            payload.put("deviceType", deviceType != null ? deviceType : "WEB");
            restTemplate.postForObject(presenceServiceUrl + "/presence/online", payload, Map.class);
            log.info("[PresenceClient] User {} set ONLINE (session: {})", userId, sessionId);
        } catch (Exception ex) {
            log.warn("[PresenceClient] setOnline failed for user {}: {}", userId, ex.getMessage());
        }
    }

    /** Called on afterConnectionClosed */
    public void setOfflineBySession(String sessionId) {
        try {
            restTemplate.delete(presenceServiceUrl + "/presence/session/" + sessionId);
            log.info("[PresenceClient] Session {} disconnected", sessionId);
        } catch (Exception ex) {
            log.warn("[PresenceClient] setOfflineBySession failed for session {}: {}",
                    sessionId, ex.getMessage());
        }
    }

    /** Called when user sends PRESENCE_UPDATE STOMP frame */
    public void updateStatus(String userId, String status, String customMessage) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("status", status);
            if (customMessage != null) payload.put("customMessage", customMessage);
            restTemplate.put(presenceServiceUrl + "/presence/status", payload);
        } catch (Exception ex) {
            log.warn("[PresenceClient] updateStatus failed for user {}: {}", userId, ex.getMessage());
        }
    }

    /** Called on client PING frame — keeps session alive */
    public void ping(String sessionId) {
        try {
            restTemplate.put(presenceServiceUrl + "/presence/ping/" + sessionId, null);
        } catch (Exception ex) {
            log.warn("[PresenceClient] ping failed for session {}: {}", sessionId, ex.getMessage());
        }
    }
}



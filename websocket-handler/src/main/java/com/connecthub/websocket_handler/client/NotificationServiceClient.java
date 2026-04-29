package com.ConnectHub.websocket_handler.client;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Calls notification-service (port 8086) to dispatch alerts for offline users.
 */
@Component
public class NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${notification.service.url:http://localhost:8086}")
    private String notificationServiceUrl;

    public NotificationServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    /** Send a NEW_MESSAGE notification to a recipient */
    public void sendNewMessageNotification(String recipientId, String actorId,
            String roomId, String messageId, String preview) {
        sendNotification(recipientId, actorId, "NEW_MESSAGE",
                "New message", preview, roomId, messageId);
    }

    /** Send a MENTION notification */
    public void sendMentionNotification(String recipientId, String actorId,
            String roomId, String messageId, String preview) {
        sendNotification(recipientId, actorId, "MENTION",
                "You were mentioned", preview, roomId, messageId);
    }

    /** Send a ROOM_INVITE notification */
    public void sendRoomInviteNotification(String recipientId, String actorId,
            String roomId, String roomName) {
        sendNotification(recipientId, actorId, "ROOM_INVITE",
                "Room Invitation", "You have been invited to join " + roomName, roomId, null);
    }

    // ─── Private ─────────────────────────────────────────────────────────────

    private void sendNotification(String recipientId, String actorId, String type,
            String title, String message, String roomId, String messageId) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("recipientId", recipientId);
            payload.put("actorId", actorId);
            payload.put("type", type);
            payload.put("title", title);
            payload.put("message", message != null ? truncate(message, 200) : "");
            if (roomId != null)    payload.put("roomId", roomId);
            if (messageId != null) payload.put("messageId", messageId);

            restTemplate.postForObject(
                    notificationServiceUrl + "/notifications", payload, Map.class);
            log.info("[NotifClient] {} notification sent to user {}", type, recipientId);
        } catch (Exception ex) {
            log.warn("[NotifClient] sendNotification failed for user {}: {}", recipientId, ex.getMessage());
        }
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}

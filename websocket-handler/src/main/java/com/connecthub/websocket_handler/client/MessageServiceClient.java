package com.ConnectHub.websocket_handler.client;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Calls message-service (port 8083) to persist and update messages.
 */
@Component
public class MessageServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${message.service.url:http://localhost:8083}")
    private String messageServiceUrl;

    public MessageServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    /** Persist a new chat message — returns the saved message with its messageId */
    public Map<String, Object> sendMessage(String roomId, String senderId,
            String content, String type, String mediaUrl, String replyToId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("roomId", roomId);
            payload.put("senderId", senderId);
            payload.put("content", content != null ? content : "");
            payload.put("type", type != null ? type : "TEXT");
            if (mediaUrl != null)  payload.put("mediaUrl", mediaUrl);
            if (replyToId != null) payload.put("replyToMessageId", replyToId);

            @SuppressWarnings("unchecked")
            Map<String, Object> saved = restTemplate.postForObject(
                    messageServiceUrl + "/messages", payload, Map.class);
            return saved;
        } catch (Exception ex) {
            log.error("[MessageClient] sendMessage failed: {}", ex.getMessage());
            return null;
        }
    }

    /** Edit an existing message */
    public void editMessage(String messageId, String newContent) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("content", newContent);
            restTemplate.put(messageServiceUrl + "/messages/" + messageId, payload);
        } catch (Exception ex) {
            log.error("[MessageClient] editMessage failed: {}", ex.getMessage());
        }
    }

    /** Soft-delete a message */
    public void deleteMessage(String messageId) {
        try {
            restTemplate.delete(messageServiceUrl + "/messages/" + messageId);
        } catch (Exception ex) {
            log.error("[MessageClient] deleteMessage failed: {}", ex.getMessage());
        }
    }

    /** Update delivery status — SENT → DELIVERED → READ */
    public void updateDeliveryStatus(String messageId, String status) {
        try {
            restTemplate.put(messageServiceUrl + "/messages/" + messageId
                    + "/status?status=" + status, null);
        } catch (Exception ex) {
            log.error("[MessageClient] updateDeliveryStatus failed: {}", ex.getMessage());
        }
    }
}

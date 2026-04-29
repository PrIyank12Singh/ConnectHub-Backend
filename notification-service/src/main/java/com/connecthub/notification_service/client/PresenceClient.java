package com.connecthub.notification_service.client;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client used by NotificationServiceImpl to query the presence-service
 * before deciding whether to send an email for a missed DM.
 *
 * Flow:
 *   1. Message arrives for a recipient
 *   2. NotificationServiceImpl calls isOnline(userId)
 *      → if true  : skip email (user is connected)
 *      → if false : check offlineSince(userId)
 *        → if offline >= 30 minutes : send email
 */
@Component
public class PresenceClient {

    private static final Logger log = LoggerFactory.getLogger(PresenceClient.class);

    private final RestTemplate restTemplate;

    @Value("${presence.service.url:http://localhost:8085}")
    private String presenceServiceUrl;

    public PresenceClient() {
        this.restTemplate = new RestTemplate();
    }

    // ─── Is Online ────────────────────────────────────────────────────────────

    /**
     * Returns true if the user has at least one active WebSocket session.
     * Falls back to false on any error so notifications are not suppressed.
     */
    public boolean isOnline(String userId) {
        try {
            String url = presenceServiceUrl + "/presence/check/" + userId;
            OnlineCheckResponse response = restTemplate.getForObject(url, OnlineCheckResponse.class);
            return response != null && Boolean.TRUE.equals(response.getOnline());
        } catch (Exception ex) {
            log.warn("[PresenceClient] isOnline check failed for user {} — assuming offline: {}",
                    userId, ex.getMessage());
            return false; // safe default: send notification
        }
    }

    // ─── Get Last Ping (offline duration) ────────────────────────────────────

    /**
     * Returns the lastPingAt timestamp from the most recent session of the user.
     * Returns null if the user has no session (fully offline).
     */
    public LocalDateTime getLastPingAt(String userId) {
        try {
            String url = presenceServiceUrl + "/presence/" + userId;
            PresenceCheckResponse response = restTemplate.getForObject(url, PresenceCheckResponse.class);
            if (response != null && response.getLastPingAt() != null) {
                return response.getLastPingAt();
            }
        } catch (Exception ex) {
            log.warn("[PresenceClient] getLastPingAt failed for user {}: {}", userId, ex.getMessage());
        }
        return null;
    }

    // ─── Offline Duration Check ───────────────────────────────────────────────

    /**
     * Returns true if the user has been offline for at least the given minutes.
     * Used to gate email sending: only email if offline >= 30 minutes.
     */
    public boolean hasBeenOfflineFor(String userId, int minutes) {
        if (isOnline(userId)) return false;

        LocalDateTime lastPing = getLastPingAt(userId);
        if (lastPing == null) {
            // No presence record at all — user has never connected or was cleaned up
            return true;
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(minutes);
        return lastPing.isBefore(threshold);
    }

    // ─── Response DTOs (simple inner classes) ─────────────────────────────────

    public static class OnlineCheckResponse {
        private Boolean online;
        public Boolean getOnline() { return online; }
        public void setOnline(Boolean online) { this.online = online; }
    }

    public static class PresenceCheckResponse {
        private String userId;
        private String status;
        private LocalDateTime lastPingAt;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getLastPingAt() { return lastPingAt; }
        public void setLastPingAt(LocalDateTime lastPingAt) { this.lastPingAt = lastPingAt; }
    }
}



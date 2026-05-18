package com.connecthub.presence_service.resource;

import com.connecthub.presence_service.dto.BulkPresenceRequest;
import com.connecthub.presence_service.dto.PresenceResponse;
import com.connecthub.presence_service.dto.SetOnlineRequest;
import com.connecthub.presence_service.dto.UpdateStatusRequest;
import com.connecthub.presence_service.service.PresenceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/presence")
public class PresenceResource {

    private final PresenceService presenceService;

    public PresenceResource(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    // ─── Set Online ───────────────────────────────────────────────────────────
    /**
     * POST /presence/online
     * Called by ChatWebSocketHandler.afterConnectionEstablished()
     */
    @PostMapping("/online")
    public ResponseEntity<PresenceResponse> setOnline(
            @Valid @RequestBody SetOnlineRequest request) {
        return ResponseEntity.ok(presenceService.setOnline(request));
    }

    // ─── Set Offline (all sessions) ──────────────────────────────────────────
    /**
     * DELETE /presence/offline/{userId}
     * Called on explicit logout or account delete
     */
    @DeleteMapping("/offline/{userId}")
    public ResponseEntity<Map<String, String>> setOffline(
            @PathVariable String userId) {
        presenceService.setOffline(userId);
        return ResponseEntity.ok(Map.of("message", "User " + userId + " marked offline"));
    }

    // ─── Set Offline (single session) ────────────────────────────────────────
    /**
     * DELETE /presence/session/{sessionId}
     * Called by ChatWebSocketHandler.afterConnectionClosed()
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, String>> setOfflineBySession(
            @PathVariable String sessionId) {
        presenceService.setOfflineBySession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session " + sessionId + " disconnected"));
    }

    // ─── Update Status ────────────────────────────────────────────────────────
    /**
     * PUT /presence/status
     * Body: { userId, status, customMessage? }
     */
    @PutMapping("/status")
    public ResponseEntity<PresenceResponse> updateStatus(
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(presenceService.updateStatus(request));
    }

    // ─── Get Single Presence ─────────────────────────────────────────────────
    /**
     * GET /presence/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<PresenceResponse> getPresence(
            @PathVariable String userId) {
        return presenceService.getPresence(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Bulk Presence ────────────────────────────────────────────────────────
    /**
     * POST /presence/bulk
     * Body: { userIds: ["uuid1","uuid2",...] }
     * Used to populate room-member-list online indicators
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<PresenceResponse>> getBulkPresence(
            @Valid @RequestBody BulkPresenceRequest request) {
        return ResponseEntity.ok(presenceService.getBulkPresence(request));
    }

    // ─── All Online Users ─────────────────────────────────────────────────────
    /**
     * GET /presence/users/online
     */
    @GetMapping("/users/online")
    public ResponseEntity<List<PresenceResponse>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }

    // ─── Online Count ─────────────────────────────────────────────────────────
    /**
     * GET /presence/count/online
     * Used by admin dashboard
     */
    @GetMapping("/count/online")
    public ResponseEntity<Map<String, Integer>> getOnlineCount() {
        return ResponseEntity.ok(Map.of("onlineCount", presenceService.getOnlineCount()));
    }

    // ─── Ping Session ─────────────────────────────────────────────────────────
    /**
     * PUT /presence/ping/{sessionId}
     * Client calls every 30 s to prevent stale-session cleanup
     */
    @PutMapping("/ping/{sessionId}")
    public ResponseEntity<Map<String, String>> ping(
            @PathVariable String sessionId) {
        presenceService.pingSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "pong"));
    }

    // ─── Is Online ────────────────────────────────────────────────────────────
    /**
     * GET /presence/check/{userId}?online=true
     */
    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Boolean>> isOnline(
            @PathVariable String userId) {
        return ResponseEntity.ok(Map.of("online", presenceService.isOnline(userId)));
    }

    // ─── Manual Stale Cleanup (admin use / testing) ───────────────────────────
    /**
     * DELETE /presence/cleanup
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupStaleSessions() {
        presenceService.cleanStaleSessions();
        return ResponseEntity.ok(Map.of("message", "Stale session cleanup triggered"));
    }
}



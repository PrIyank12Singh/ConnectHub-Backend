package com.connecthub.connecthub_web.controller;

import com.connecthub.connecthub_web.config.ServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * AdminController — Platform Admin operations.
 * Secured with ROLE_ADMIN; provides user management, room management,
 * analytics, broadcast messaging, and audit log viewing.
 *
 * Base path: /web/admin
 */
@RestController
@RequestMapping("/web/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private static final String PRESENCE_COUNT_ONLINE = "/presence/count/online";
    private static final String AUTH_ADMIN_USERS = "/auth/admin/users";

    private final RestTemplate restTemplate;
    private final ServiceProperties svc;

    // ─── Dashboard ───────────────────────────────────────────────────────────

    /**
     * GET /web/admin/dashboard
     * Aggregates: online user count + total platform analytics in one call.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> adminDashboard(@RequestHeader("Authorization") String auth) {
        try {
            ResponseEntity<Object> onlineCount  = forward(HttpMethod.GET,
                svc.getPresenceUrl() + PRESENCE_COUNT_ONLINE, null, auth);
            ResponseEntity<Object> allUsers     = forward(HttpMethod.GET,
                svc.getAuthUrl() + AUTH_ADMIN_USERS, null, auth);
            ResponseEntity<Object> allRooms     = forward(HttpMethod.GET,
                    svc.getRoomUrl() + "/rooms", null, auth);

            Map<String, Object> dashboard = Map.of(
                    "onlineCount", onlineCount.getBody(),
                    "allUsers",    allUsers.getBody(),
                    "allRooms",    allRooms.getBody()
            );
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Admin dashboard failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── User Management ─────────────────────────────────────────────────────

    /** GET /web/admin/users  — list all users */
    @GetMapping("/users")
    public ResponseEntity<Object> getAllUsers(@RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getAuthUrl() + AUTH_ADMIN_USERS, null, auth);
    }

    /** PUT /web/admin/users/{userId}/suspend  — suspend user (set isActive=false) */
    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<Object> suspendUser(@PathVariable String userId,
                                              @RequestHeader("Authorization") String auth) {
        ResponseEntity<Object> response = forward(HttpMethod.PUT,
                                svc.getAuthUrl() + AUTH_ADMIN_USERS + "/" + userId + "/suspend", null, auth);
        if (response.getStatusCode().is2xxSuccessful()) {
            writeAuditLog("SUSPEND_USER", extractUserIdFromToken(auth), userId,
                    "Admin suspended user " + userId, auth);
        }
        return response;
    }

    /** PUT /web/admin/users/{userId}/reactivate */
    @PutMapping("/users/{userId}/reactivate")
    public ResponseEntity<Object> reactivateUser(@PathVariable String userId,
                                                 @RequestHeader("Authorization") String auth) {
        ResponseEntity<Object> response = forward(HttpMethod.PUT,
                                svc.getAuthUrl() + AUTH_ADMIN_USERS + "/" + userId + "/reactivate", null, auth);
        if (response.getStatusCode().is2xxSuccessful()) {
            writeAuditLog("REACTIVATE_USER", extractUserIdFromToken(auth), userId,
                    "Admin reactivated user " + userId, auth);
        }
        return response;
    }

    /** DELETE /web/admin/users/{userId}  — permanently delete user */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Object> deleteUser(@PathVariable String userId,
                                             @RequestHeader("Authorization") String auth) {
        ResponseEntity<Object> response = forward(HttpMethod.DELETE, svc.getAuthUrl() + AUTH_ADMIN_USERS + "/" + userId, null, auth);
        if (response.getStatusCode().is2xxSuccessful()) {
            writeAuditLog("DELETE_USER", extractUserIdFromToken(auth), userId,
                    "Admin deleted user " + userId, auth);
        }
        return response;
    }

    // ─── Room Management ─────────────────────────────────────────────────────

    /** GET /web/admin/rooms  — list all rooms */
    @GetMapping("/rooms")
    public ResponseEntity<Object> getAllRooms(@RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getRoomUrl() + "/rooms", null, auth);
    }

    /** DELETE /web/admin/rooms/{roomId}  — delete any room */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Object> deleteRoom(@PathVariable String roomId,
                                             @RequestHeader("Authorization") String auth) {
        ResponseEntity<Object> response = forward(HttpMethod.DELETE, svc.getRoomUrl() + "/rooms/" + roomId, null, auth);
        if (response.getStatusCode().is2xxSuccessful()) {
            writeAuditLog("DELETE_ROOM", extractUserIdFromToken(auth), roomId,
                    "Admin deleted room " + roomId, auth);
        }
        return response;
    }

    // ─── Message Management ──────────────────────────────────────────────────

    /** GET /web/admin/messages/room/{roomId}  — view all messages in a room */
    @GetMapping("/messages/room/{roomId}")
    public ResponseEntity<Object> getAllMessages(@PathVariable String roomId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size,
                                                @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET,
                svc.getMessageUrl() + "/messages/room/" + roomId + "?page=" + page + "&size=" + size,
                null, auth);
    }

    /** DELETE /web/admin/messages/{messageId}  — delete any message for policy violation */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Object> deleteMessage(@PathVariable String messageId,
                                                @RequestHeader("Authorization") String auth) {
        ResponseEntity<Object> response = forward(HttpMethod.DELETE, svc.getMessageUrl() + "/messages/" + messageId, null, auth);
        if (response.getStatusCode().is2xxSuccessful()) {
            writeAuditLog("DELETE_MESSAGE", extractUserIdFromToken(auth), messageId,
                    "Admin deleted message " + messageId, auth);
        }
        return response;
    }

    // ─── Active Connections ───────────────────────────────────────────────────

    /** GET /web/admin/connections  — live WebSocket connection count */
    @GetMapping("/connections")
    public ResponseEntity<Object> getActiveConnections(@RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getPresenceUrl() + PRESENCE_COUNT_ONLINE, null, auth);
    }

    /** GET /web/admin/users/online  — list currently online users */
    @GetMapping("/users/online")
    public ResponseEntity<Object> getOnlineUsers(@RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getPresenceUrl() + "/presence/users/online", null, auth);
    }

    // ─── Platform Analytics ───────────────────────────────────────────────────

    /**
     * GET /web/admin/analytics
     * Aggregated: total users, online count, total rooms, media count.
     */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getPlatformAnalytics(@RequestHeader("Authorization") String auth) {
        try {
            ResponseEntity<Object> userCount    = forward(HttpMethod.GET,
                    svc.getAuthUrl() + AUTH_ADMIN_USERS + "/count", null, auth);  // ← fixed slash
            ResponseEntity<Object> onlineCount  = forward(HttpMethod.GET,
                svc.getPresenceUrl() + PRESENCE_COUNT_ONLINE, null, auth);
            ResponseEntity<Object> mediaCount   = forward(HttpMethod.GET,
                    svc.getMediaUrl() + "/media/count", null, auth);

            Map<String, Object> analytics = Map.of(
                    "totalUsers",    userCount.getBody(),
                    "onlineUsers",   onlineCount.getBody(),
                    "totalMedia",    mediaCount.getBody()
            );
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Analytics aggregation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── Broadcast Notification ───────────────────────────────────────────────

    /** POST /web/admin/broadcast  — send platform-wide notification */
    @PostMapping("/broadcast")
    public ResponseEntity<Object> sendPlatformNotification(@RequestBody Map<String, Object> body,
                                                           @RequestHeader("Authorization") String auth) {
        try {
            // Fetch all users to get their IDs
            ResponseEntity<Object> usersResponse = forward(HttpMethod.GET,
                    svc.getAuthUrl() + AUTH_ADMIN_USERS, null, auth);

            List<String> recipientIds = new java.util.ArrayList<>();
            if (usersResponse.getBody() instanceof List<?> users) {
                users.forEach(u -> {
                    if (u instanceof Map<?, ?> user && user.get("userId") != null) {
                        recipientIds.add(user.get("userId").toString());
                    }
                });
            }

            Map<String, Object> enrichedBody = new java.util.HashMap<>(body);
            enrichedBody.put("recipientIds", recipientIds);
            return forward(HttpMethod.POST,
                    svc.getNotificationUrl() + "/notifications/bulk", enrichedBody, auth);
        } catch (Exception e) {
            log.error("Broadcast failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Broadcast failed", "detail", e.getMessage()));
        }
    }

    // ─── Audit Logs ──────────────────────────────────────────────────────────

    /** GET /web/admin/audit-logs  — audit log feed from message-service */
    @GetMapping("/audit-logs")
    public ResponseEntity<Object> getAuditLogs(@RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getMessageUrl() + "/audit/logs", null, auth);
    }

    // ─── Media Library ───────────────────────────────────────────────────────

    /** GET /web/admin/media  — all uploaded files */
    @GetMapping("/media")
    public ResponseEntity<Object> getAllMedia(@RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getMediaUrl() + "/media/all", null, auth);
    }

    /** DELETE /web/admin/media/{mediaId} */
    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<Object> deleteMedia(@PathVariable String mediaId,
                                              @RequestHeader("Authorization") String auth) {
        ResponseEntity<Object> response = forward(HttpMethod.DELETE, svc.getMediaUrl() + "/media/" + mediaId, null, auth);
        if (response.getStatusCode().is2xxSuccessful()) {
            writeAuditLog("DELETE_MEDIA", extractUserIdFromToken(auth), mediaId,
                    "Admin deleted media " + mediaId, auth);
        }
        return response;
    }

    // ─── Internal helper ─────────────────────────────────────────────────────

    private ResponseEntity<Object> forward(HttpMethod method, String url, Object body, String authHeader) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeader != null) headers.set("Authorization", authHeader);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, method, entity,
                    new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("Forward failed → {} {}: {}", method, url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Downstream service unavailable", "detail", e.getMessage()));
        }
    }

    private HttpHeaders buildHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authHeader != null) headers.set("Authorization", authHeader);
        return headers;
    }

    private void writeAuditLog(String action, String actorId, String targetId, String details, String auth) {
        try {
            Map<String, Object> logEntry = Map.of(
                    "action", action,
                    "actorId", actorId != null ? actorId : "",
                    "targetId", targetId != null ? targetId : "",
                    "details", details != null ? details : ""
            );
            forward(HttpMethod.POST, svc.getMessageUrl() + "/audit/logs", logEntry, auth);
        } catch (Exception e) {
            log.warn("Audit log write failed: {}", e.getMessage());
        }
    }

    private String extractUserIdFromToken(String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            return payload.split("\"sub\":\"")[1].split("\"")[0];
        } catch (Exception e) {
            return "unknown";
        }
    }
}
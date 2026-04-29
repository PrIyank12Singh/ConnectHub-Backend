package com.connecthub.connecthub_web.controller;

import com.connecthub.connecthub_web.config.ServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.security.Principal;
import java.util.Map;

/**
 * ChatController — User-facing MVC REST endpoints.
 * Aggregates calls to downstream microservices and returns unified responses.
 *
 * Base path: /web/chat
 * All requests are already JWT-authenticated (either via api-gateway headers
 * or the local JwtAuthFilter).
 */
@RestController
@RequestMapping("/web")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RestTemplate restTemplate;
    private final ServiceProperties svc;

    // ─── Auth / User ─────────────────────────────────────────────────────────

    /** POST /web/auth/register  →  auth-service /auth/register */
    @PostMapping("/auth/register")
    public ResponseEntity<Object> register(@RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, svc.getAuthUrl() + "/auth/register", body, null);
    }

    /** POST /web/auth/login  →  auth-service /auth/login */
    @PostMapping("/auth/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, svc.getAuthUrl() + "/auth/login", body, null);
    }

    /** POST /web/auth/logout */
    @PostMapping("/auth/logout")
    public ResponseEntity<Object> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        return forward(HttpMethod.POST, svc.getAuthUrl() + "/auth/logout", null, auth);
    }

    /** POST /web/auth/refresh */
    @PostMapping("/auth/refresh")
    public ResponseEntity<Object> refresh(@RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.POST, svc.getAuthUrl() + "/auth/refresh", null, auth);
    }

    /** GET /web/auth/profile/{userId} */
    @GetMapping("/auth/profile/{userId}")
    public ResponseEntity<Object> getProfile(@PathVariable String userId,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getAuthUrl() + "/auth/profile/" + userId, null, auth);
    }

    /** PUT /web/auth/profile/{userId} */
    @PutMapping("/auth/profile/{userId}")
    public ResponseEntity<Object> updateProfile(@PathVariable String userId,
                                                @RequestBody Map<String, Object> body,
                                                @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT, svc.getAuthUrl() + "/auth/profile/" + userId, body, auth);
    }

    /** PUT /web/auth/profile/{userId}/avatar */
    @PutMapping(value = "/auth/profile/{userId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> updateAvatar(@PathVariable String userId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestHeader("Authorization") String auth) {
        return forwardMultipart(HttpMethod.PUT,
                svc.getAuthUrl() + "/auth/profile/" + userId + "/avatar",
                file,
                auth);
    }

    /** PUT /web/auth/password/{userId} */
    @PutMapping("/auth/password/{userId}")
    public ResponseEntity<Object> changePassword(@PathVariable String userId,
                                                 @RequestBody Map<String, Object> body,
                                                 @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT, svc.getAuthUrl() + "/auth/password/" + userId, body, auth);
    }

    /** GET /web/auth/search?username=xxx */
    @GetMapping("/auth/search")
    public ResponseEntity<Object> searchUsers(@RequestParam String username,
                                              @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getAuthUrl() + "/auth/search?username=" + username, null, auth);
    }

    /** PUT /web/auth/status/{userId}?status=ONLINE */
    @PutMapping("/auth/status/{userId}")
    public ResponseEntity<Object> updateStatus(@PathVariable String userId,
                                               @RequestParam String status,
                                               @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                svc.getAuthUrl() + "/auth/status/" + userId + "?status=" + status, null, auth);
    }

    // ─── Rooms ───────────────────────────────────────────────────────────────

    /** POST /web/rooms  — create room */
    @PostMapping("/rooms")
    public ResponseEntity<Object> createRoom(@RequestBody Map<String, Object> body,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.POST, svc.getRoomUrl() + "/rooms", body, auth);
    }

    /** GET /web/rooms/user/{userId} */
    @GetMapping("/rooms/user/{userId}")
    public ResponseEntity<Object> getRoomsByUser(@PathVariable String userId,
                                                 @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getRoomUrl() + "/rooms/user/" + userId, null, auth);
    }

    /** GET /web/rooms/{roomId} */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Object> getRoomById(@PathVariable String roomId,
                                              @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getRoomUrl() + "/rooms/" + roomId, null, auth);
    }

    /** PUT /web/rooms/{roomId} */
    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<Object> updateRoom(@PathVariable String roomId,
                                             @RequestBody Map<String, Object> body,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT, svc.getRoomUrl() + "/rooms/" + roomId, body, auth);
    }

    /** DELETE /web/rooms/{roomId} */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Object> deleteRoom(@PathVariable String roomId,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.DELETE, svc.getRoomUrl() + "/rooms/" + roomId, null, auth);
    }

    /** GET /web/rooms/{roomId}/members */
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<Object> getRoomMembers(@PathVariable String roomId,
                                                 @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getRoomUrl() + "/rooms/" + roomId + "/members", null, auth);
    }

    /** POST /web/rooms/{roomId}/members */
    @PostMapping("/rooms/{roomId}/members")
    public ResponseEntity<Object> addMember(@PathVariable String roomId,
                                            @RequestBody Map<String, Object> body,
                                            @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.POST, svc.getRoomUrl() + "/rooms/" + roomId + "/members", body, auth);
    }

    /** DELETE /web/rooms/{roomId}/members/{userId} */
    @DeleteMapping("/rooms/{roomId}/members/{userId}")
    public ResponseEntity<Object> removeMember(@PathVariable String roomId,
                                               @PathVariable String userId,
                                               @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.DELETE,
                svc.getRoomUrl() + "/rooms/" + roomId + "/members/" + userId, null, auth);
    }

    /** PUT /web/rooms/{roomId}/members/{userId}/role?role=ADMIN */
    @PutMapping("/rooms/{roomId}/members/{userId}/role")
    public ResponseEntity<Object> updateMemberRole(@PathVariable String roomId,
                                                   @PathVariable String userId,
                                                   @RequestParam String role,
                                                   @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                svc.getRoomUrl() + "/rooms/" + roomId + "/members/" + userId + "/role?role=" + role,
                null, auth);
    }

    /** PUT /web/rooms/{roomId}/members/{userId}/mute?mute=true */
    @PutMapping("/rooms/{roomId}/members/{userId}/mute")
    public ResponseEntity<Object> muteMember(@PathVariable String roomId,
                                             @PathVariable String userId,
                                             @RequestParam boolean mute,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                svc.getRoomUrl() + "/rooms/" + roomId + "/members/" + userId + "/mute?mute=" + mute,
                null, auth);
    }

    /** PUT /web/rooms/{roomId}/members/{userId}/read */
    @PutMapping("/rooms/{roomId}/members/{userId}/read")
    public ResponseEntity<Object> updateLastRead(@PathVariable String roomId,
                                                 @PathVariable String userId,
                                                 @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                svc.getRoomUrl() + "/rooms/" + roomId + "/members/" + userId + "/read",
                null, auth);
    }

    /** GET /web/rooms/{roomId}/unread/{userId} */
    @GetMapping("/rooms/{roomId}/unread/{userId}")
    public ResponseEntity<Object> getUnreadCount(@PathVariable String roomId,
                                                 @PathVariable String userId,
                                                 @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET,
                svc.getRoomUrl() + "/rooms/" + roomId + "/unread/" + userId, null, auth);
    }

    // ─── Messages ────────────────────────────────────────────────────────────

    /** POST /web/messages */
    @PostMapping("/messages")
    public ResponseEntity<Object> sendMessage(@RequestBody Map<String, Object> body,
                                              @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.POST, svc.getMessageUrl() + "/messages", body, auth);
    }

    /** GET /web/messages/room/{roomId}?page=0&size=20 */
    @GetMapping("/messages/room/{roomId}")
    public ResponseEntity<Object> getMessagesByRoom(@PathVariable String roomId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET,
                svc.getMessageUrl() + "/messages/room/" + roomId + "?page=" + page + "&size=" + size,
                null, auth);
    }

    /** GET /web/messages/{messageId} */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<Object> getMessageById(@PathVariable String messageId,
                                                 @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getMessageUrl() + "/messages/" + messageId, null, auth);
    }

    /** PUT /web/messages/{messageId} */
    @PutMapping("/messages/{messageId}")
    public ResponseEntity<Object> editMessage(@PathVariable String messageId,
                                              @RequestBody Map<String, Object> body,
                                              @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT, svc.getMessageUrl() + "/messages/" + messageId, body, auth);
    }

    /** DELETE /web/messages/{messageId} */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Object> deleteMessage(@PathVariable String messageId,
                                                @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.DELETE, svc.getMessageUrl() + "/messages/" + messageId, null, auth);
    }

    /** GET /web/messages/room/{roomId}/search?keyword=hello */
    @GetMapping("/messages/room/{roomId}/search")
    public ResponseEntity<Object> searchMessages(@PathVariable String roomId,
                                                 @RequestParam String keyword,
                                                 @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET,
                svc.getMessageUrl() + "/messages/room/" + roomId + "/search?keyword=" + keyword,
                null, auth);
    }

    /** PUT /web/messages/{messageId}/status?status=READ */
    @PutMapping("/messages/{messageId}/status")
    public ResponseEntity<Object> updateDeliveryStatus(@PathVariable String messageId,
                                                       @RequestParam String status,
                                                       @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                svc.getMessageUrl() + "/messages/" + messageId + "/status?status=" + status,
                null, auth);
    }

    /** GET /web/messages/room/{roomId}/count */
    @GetMapping("/messages/room/{roomId}/count")
    public ResponseEntity<Object> getMessageCount(@PathVariable String roomId,
                                                  @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET,
                svc.getMessageUrl() + "/messages/room/" + roomId + "/count", null, auth);
    }

    // ─── Presence ────────────────────────────────────────────────────────────

    /** POST /web/presence/online/{userId} */
    @PostMapping("/presence/online/{userId}")
    public ResponseEntity<Object> setOnline(@PathVariable String userId,
                                            @RequestBody(required = false) Map<String, Object> body,
                                            @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.POST, svc.getPresenceUrl() + "/presence/online/" + userId, body, auth);
    }

    /** POST /web/presence/offline/{userId} */
    @PostMapping("/presence/offline/{userId}")
    public ResponseEntity<Object> setOffline(@PathVariable String userId,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.POST, svc.getPresenceUrl() + "/presence/offline/" + userId, null, auth);
    }

    /** GET /web/presence/{userId} */
    @GetMapping("/presence/{userId}")
    public ResponseEntity<Object> getPresence(@PathVariable String userId,
                                              @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getPresenceUrl() + "/presence/" + userId, null, auth);
    }

    /** PUT /web/presence/status/{userId}?status=AWAY */
    @PutMapping("/presence/status/{userId}")
    public ResponseEntity<Object> updatePresenceStatus(@PathVariable String userId,
                                                       @RequestParam String status,
                                                       @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                svc.getPresenceUrl() + "/presence/status/" + userId + "?status=" + status,
                null, auth);
    }

    /** GET /web/presence/online/count */
    @GetMapping("/presence/online/count")
    public ResponseEntity<Object> getOnlineCount(@RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getPresenceUrl() + "/presence/online/count", null, auth);
    }

    // ─── Notifications ───────────────────────────────────────────────────────

    /** GET /web/notifications/{userId} */
    @GetMapping("/notifications/{userId}")
    public ResponseEntity<Object> getNotifications(@PathVariable String userId,
                                                   @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getNotificationUrl() + "/notifications/recipient/" + userId, null, auth);
    }

    /** PUT /web/notifications/{notifId}/read */
    @PutMapping("/notifications/{notifId}/read")
    public ResponseEntity<Object> markAsRead(@PathVariable String notifId,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                svc.getNotificationUrl() + "/notifications/" + notifId + "/read", null, auth);
    }

    /** PUT /web/notifications/read-all/{userId} */
    @PutMapping("/notifications/read-all/{userId}")
    public ResponseEntity<Object> markAllRead(@PathVariable String userId,
                                              @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                svc.getNotificationUrl() + "/notifications/read-all/" + userId, null, auth);
    }

    /** GET /web/notifications/{userId}/unread-count */
    @GetMapping("/notifications/{userId}/unread-count")
    public ResponseEntity<Object> getUnreadNotifCount(@PathVariable String userId,
                                                      @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET,
                svc.getNotificationUrl() + "/notifications/" + userId + "/unread-count", null, auth);
    }

    /** DELETE /web/notifications/{notifId} */
    @DeleteMapping("/notifications/{notifId}")
    public ResponseEntity<Object> deleteNotification(@PathVariable String notifId,
                                                     @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.DELETE,
                svc.getNotificationUrl() + "/notifications/" + notifId, null, auth);
    }

    // ─── Media ───────────────────────────────────────────────────────────────

    /** GET /web/media/room/{roomId} */
    @GetMapping("/media/room/{roomId}")
    public ResponseEntity<Object> getRoomMedia(@PathVariable String roomId,
                                               @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getMediaUrl() + "/media/room/" + roomId, null, auth);
    }

    /** DELETE /web/media/{mediaId} */
    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<Object> deleteMedia(@PathVariable String mediaId,
                                              @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.DELETE, svc.getMediaUrl() + "/media/" + mediaId, null, auth);
    }

    // ─── Dashboard Aggregation ────────────────────────────────────────────────

    /**
     * GET /web/dashboard/{userId}
     * Returns aggregated data: user profile + rooms + unread notification count.
     * Called once by Angular on app load.
     */
    @GetMapping("/dashboard/{userId}")
    public ResponseEntity<Map<String, Object>> getDashboard(@PathVariable String userId,
                                                            @RequestHeader("Authorization") String auth) {
        try {
            ResponseEntity<Object> profile    = forward(HttpMethod.GET, svc.getAuthUrl() + "/auth/profile/" + userId, null, auth);
            ResponseEntity<Object> rooms      = forward(HttpMethod.GET, svc.getRoomUrl() + "/rooms/user/" + userId, null, auth);
            ResponseEntity<Object> presence   = forward(HttpMethod.GET, svc.getPresenceUrl() + "/presence/" + userId, null, auth);
            ResponseEntity<Object> notifCount = forward(HttpMethod.GET,
                    svc.getNotificationUrl() + "/notifications/" + userId + "/unread-count", null, auth);

            Map<String, Object> dashboard = Map.of(
                    "profile",            profile.getBody(),
                    "rooms",              rooms.getBody(),
                    "presence",           presence.getBody(),
                    "unreadNotifCount",   notifCount.getBody()
            );
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Dashboard aggregation failed for userId={}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── Internal helper ─────────────────────────────────────────────────────

    private ResponseEntity<Object> forward(HttpMethod method, String url, Object body, String authHeader) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, method, entity,
                    new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("Forward failed → {} {}: {}", method, url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Downstream service unavailable", "detail", e.getMessage()));
        }
    }

    private ResponseEntity<Object> forwardMultipart(HttpMethod method, String url, MultipartFile file, String authHeader) {
        try {
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(
                    file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
            fileHeaders.setContentDispositionFormData("file", file.getOriginalFilename());

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new HttpEntity<Resource>(fileResource, fileHeaders));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, method, entity, new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("Forward multipart failed → {} {}: {}", method, url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Downstream service unavailable", "detail", e.getMessage()));
        }
    }
}

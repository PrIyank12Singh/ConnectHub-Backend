package com.connecthub.connecthub_web.controller;

import com.connecthub.connecthub_web.config.ServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * RoomManagerController — Room Admin operations.
 * Handles room settings, member moderation, message pinning, and room history.
 *
 * Base path: /web/room-manager
 * Requires ROLE_ADMIN or Room-Admin privilege (enforced per-room in room-service).
 */
@RestController
@RequestMapping("/web/room-manager")
@RequiredArgsConstructor
@Slf4j
public class RoomManagerController {

    private static final String ROOMS_PATH = "/rooms/";
    private static final String MEMBERS_PATH = "/members/";

    private final RestTemplate restTemplate;
    private final ServiceProperties svc;

    // ─── Room Settings ───────────────────────────────────────────────────────

    /** GET /web/room-manager/rooms/user/{userId}  — list rooms the admin manages */
    @GetMapping("/rooms/user/{userId}")
    public ResponseEntity<Object> getMyRooms(@PathVariable String userId,
                                             @RequestHeader("Authorization") String auth) {
                            return forward(HttpMethod.GET, svc.getRoomUrl() + ROOMS_PATH + "user/" + userId, null, auth);
    }

    /** GET /web/room-manager/rooms/{roomId}/settings */
    @GetMapping("/rooms/{roomId}/settings")
    public ResponseEntity<Object> getRoomSettings(@PathVariable String roomId,
                                                  @RequestHeader("Authorization") String auth) {
                            return forward(HttpMethod.GET, svc.getRoomUrl() + ROOMS_PATH + roomId, null, auth);
    }

    /** PUT /web/room-manager/rooms/{roomId}  — update name, description, avatar */
    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<Object> updateRoom(@PathVariable String roomId,
                                             @RequestBody Map<String, Object> body,
                                             @RequestHeader("Authorization") String auth) {
                            return forward(HttpMethod.PUT, svc.getRoomUrl() + ROOMS_PATH + roomId, body, auth);
    }

    /** DELETE /web/room-manager/rooms/{roomId}  — delete entire room */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Object> deleteRoom(@PathVariable String roomId,
                                             @RequestHeader("Authorization") String auth) {
                            return forward(HttpMethod.DELETE, svc.getRoomUrl() + ROOMS_PATH + roomId, null, auth);
    }

    // ─── Member Management ───────────────────────────────────────────────────

    /** GET /web/room-manager/rooms/{roomId}/members */
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<Object> getRoomMembers(@PathVariable String roomId,
                                                 @RequestHeader("Authorization") String auth) {
                            return forward(HttpMethod.GET, svc.getRoomUrl() + ROOMS_PATH + roomId + MEMBERS_PATH, null, auth);
    }

    /** POST /web/room-manager/rooms/{roomId}/members  — add member */
    @PostMapping("/rooms/{roomId}/members")
    public ResponseEntity<Object> addMember(@PathVariable String roomId,
                                            @RequestBody Map<String, Object> body,
                                            @RequestHeader("Authorization") String auth) {
                            return forward(HttpMethod.POST, svc.getRoomUrl() + ROOMS_PATH + roomId + MEMBERS_PATH, body, auth);
    }

    /** DELETE /web/room-manager/rooms/{roomId}/members/{userId}  — remove member */
    @DeleteMapping("/rooms/{roomId}/members/{userId}")
    public ResponseEntity<Object> removeMember(@PathVariable String roomId,
                                               @PathVariable String userId,
                                               @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.DELETE,
                                svc.getRoomUrl() + ROOMS_PATH + roomId + MEMBERS_PATH + userId, null, auth);
    }

    /** PUT /web/room-manager/rooms/{roomId}/members/{userId}/role?role=ADMIN */
    @PutMapping("/rooms/{roomId}/members/{userId}/role")
    public ResponseEntity<Object> changeRole(@PathVariable String roomId,
                                             @PathVariable String userId,
                                             @RequestParam String role,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                                svc.getRoomUrl() + ROOMS_PATH + roomId + MEMBERS_PATH + userId + "/role?role=" + role,
                null, auth);
    }

    /** PUT /web/room-manager/rooms/{roomId}/members/{userId}/mute?mute=true */
    @PutMapping("/rooms/{roomId}/members/{userId}/mute")
    public ResponseEntity<Object> muteMember(@PathVariable String roomId,
                                             @PathVariable String userId,
                                             @RequestParam boolean mute,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.PUT,
                                svc.getRoomUrl() + ROOMS_PATH + roomId + MEMBERS_PATH + userId + "/mute?mute=" + mute,
                null, auth);
    }

    // ─── Message Moderation ──────────────────────────────────────────────────

    /** DELETE /web/room-manager/messages/{messageId}  — delete any message (moderation) */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Object> deleteMessage(@PathVariable String messageId,
                                                @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.DELETE, svc.getMessageUrl() + "/messages/" + messageId, null, auth);
    }

    /** DELETE /web/room-manager/rooms/{roomId}/history  — clear entire room history */
    @DeleteMapping("/rooms/{roomId}/history")
    public ResponseEntity<Object> clearRoomHistory(@PathVariable String roomId,
                                                   @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.DELETE,
                svc.getMessageUrl() + "/messages/room/" + roomId + "/history", null, auth);
    }

    // ─── Pin / Unpin ─────────────────────────────────────────────────────────

    /** POST /web/room-manager/rooms/{roomId}/pin/{messageId} */
    @PostMapping("/rooms/{roomId}/pin/{messageId}")
    public ResponseEntity<Object> pinMessage(@PathVariable String roomId,
                                             @PathVariable String messageId,
                                             @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.POST,
                svc.getRoomUrl() + "/rooms/" + roomId + "/pin/" + messageId, null, auth);
    }

    /** DELETE /web/room-manager/rooms/{roomId}/pin/{messageId} */
    @DeleteMapping("/rooms/{roomId}/pin/{messageId}")
    public ResponseEntity<Object> unpinMessage(@PathVariable String roomId,
                                               @PathVariable String messageId,
                                               @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.DELETE,
                svc.getRoomUrl() + "/rooms/" + roomId + "/pin/" + messageId, null, auth);
    }

    // ─── Room Notifications ──────────────────────────────────────────────────

    /** GET /web/room-manager/rooms/{roomId}/notifications */
    @GetMapping("/rooms/{roomId}/notifications")
    public ResponseEntity<Object> getRoomNotifications(@PathVariable String roomId,
                                                       @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET,
                svc.getNotificationUrl() + "/notifications/room/" + roomId, null, auth);
    }

    /** GET /web/room-manager/rooms/{roomId}/media */
    @GetMapping("/rooms/{roomId}/media")
    public ResponseEntity<Object> getRoomMedia(@PathVariable String roomId,
                                               @RequestHeader("Authorization") String auth) {
        return forward(HttpMethod.GET, svc.getMediaUrl() + "/media/room/" + roomId, null, auth);
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
}



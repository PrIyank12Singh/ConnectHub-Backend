package com.ConnectHub.room_service.resource;

import com.ConnectHub.room_service.dto.AddMemberRequest;
import com.ConnectHub.room_service.dto.CreateRoomRequest;
import com.ConnectHub.room_service.dto.MemberResponse;
import com.ConnectHub.room_service.dto.RoomResponse;
import com.ConnectHub.room_service.dto.UpdateRoomRequest;
import com.ConnectHub.room_service.model.MemberRole;
import com.ConnectHub.room_service.service.RoomService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomResource {

    private final RoomService roomService;

    public RoomResource(RoomService roomService) {
        this.roomService = roomService;
    }

    // ─── Room CRUD ────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RoomResponse>> getRoomsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(roomService.getRoomsByUser(userId));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateRoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, request));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Map<String, String>> deleteRoom(@PathVariable UUID roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.ok(Map.of("message", "Room deleted successfully"));
    }

    // ─── Member Management ───────────────────────────────────────────────────

    @PostMapping("/{roomId}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID roomId,
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.addMember(roomId, request));
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable UUID roomId,
            @PathVariable UUID userId) {
        roomService.removeMember(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.getMembers(roomId));
    }

    // ─── Role & Mute ─────────────────────────────────────────────────────────

    @PutMapping("/{roomId}/members/{userId}/role")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            @RequestParam MemberRole role) {
        return ResponseEntity.ok(roomService.updateMemberRole(roomId, userId, role));
    }

    @PutMapping("/{roomId}/members/{userId}/mute")
    public ResponseEntity<MemberResponse> muteUnmuteMember(
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            @RequestParam boolean mute) {
        return ResponseEntity.ok(roomService.muteUnmuteMember(roomId, userId, mute));
    }

    // ─── Read Tracking ───────────────────────────────────────────────────────

    @PutMapping("/{roomId}/members/{userId}/read")
    public ResponseEntity<MemberResponse> updateLastRead(
            @PathVariable UUID roomId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(roomService.updateLastRead(roomId, userId));
    }

    @GetMapping("/{roomId}/unread/{userId}")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            @RequestParam(required = false) LocalDateTime since) {
        int count = roomService.getUnreadCount(roomId, userId, since);
        return ResponseEntity.ok(Map.of("roomId", roomId, "userId", userId, "unreadCount", count));
    }

    // ✅ Called by message-service when a new message is sent
    @PatchMapping("/{roomId}/last-message")
    public ResponseEntity<RoomResponse> updateLastMessageAt(
            @PathVariable UUID roomId,
            @RequestParam LocalDateTime timestamp) {
        return ResponseEntity.ok(roomService.updateLastMessageAt(roomId, timestamp));
    }
}

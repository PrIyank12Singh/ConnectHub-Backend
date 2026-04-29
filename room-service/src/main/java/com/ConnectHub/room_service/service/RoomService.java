package com.ConnectHub.room_service.service;

import com.ConnectHub.room_service.dto.AddMemberRequest;
import com.ConnectHub.room_service.dto.CreateRoomRequest;
import com.ConnectHub.room_service.dto.MemberResponse;
import com.ConnectHub.room_service.dto.RoomResponse;
import com.ConnectHub.room_service.dto.UpdateRoomRequest;
import com.ConnectHub.room_service.model.MemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RoomService {

    RoomResponse createRoom(CreateRoomRequest request);

    RoomResponse getRoomById(UUID roomId);

    List<RoomResponse> getRoomsByUser(UUID userId);

    RoomResponse updateRoom(UUID roomId, UpdateRoomRequest request);

    void deleteRoom(UUID roomId);

    MemberResponse addMember(UUID roomId, AddMemberRequest request);

    void removeMember(UUID roomId, UUID userId);

    List<MemberResponse> getMembers(UUID roomId);

    MemberResponse updateMemberRole(UUID roomId, UUID userId, MemberRole role);

    MemberResponse muteUnmuteMember(UUID roomId, UUID userId, boolean mute);

    MemberResponse updateLastRead(UUID roomId, UUID userId);

    int getUnreadCount(UUID roomId, UUID userId, LocalDateTime since);

    // Called by message-service when a new message is sent
    RoomResponse updateLastMessageAt(UUID roomId, LocalDateTime timestamp);

    // ─── GAP 10 FIX ───────────────────────────────────────────────────────────
    /**
     * Pin a message to the top of the room.
     * Overwrites any previously pinned message.
     *
     * @param roomId    UUID of the target room
     * @param messageId UUID string of the message to pin
     * @return updated RoomResponse with pinnedMessageId set
     */
    RoomResponse pinMessage(UUID roomId, String messageId);

    /**
     * Remove the pinned message from a room.
     * No-ops silently if nothing was pinned.
     *
     * @param roomId UUID of the target room
     * @return updated RoomResponse with pinnedMessageId set to null
     */
    RoomResponse unpinMessage(UUID roomId);
    // ─────────────────────────────────────────────────────────────────────────
}

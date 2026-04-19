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

    // ✅ New — called by message-service when a message is sent
    RoomResponse updateLastMessageAt(UUID roomId, LocalDateTime timestamp);
}

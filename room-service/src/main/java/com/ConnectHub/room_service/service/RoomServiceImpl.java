package com.ConnectHub.room_service.service;

import com.ConnectHub.room_service.dto.AddMemberRequest;
import com.ConnectHub.room_service.dto.CreateRoomRequest;
import com.ConnectHub.room_service.dto.MemberResponse;
import com.ConnectHub.room_service.dto.RoomResponse;
import com.ConnectHub.room_service.dto.UpdateRoomRequest;
import com.ConnectHub.room_service.client.MessageServiceClient;
import com.ConnectHub.room_service.model.MemberRole;
import com.ConnectHub.room_service.model.Room;
import com.ConnectHub.room_service.model.RoomMember;
import com.ConnectHub.room_service.model.RoomType;
import com.ConnectHub.room_service.repository.RoomMemberRepository;
import com.ConnectHub.room_service.repository.RoomRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final MessageServiceClient messageServiceClient;

    public RoomServiceImpl(RoomRepository roomRepository,
                           RoomMemberRepository memberRepository,
                           MessageServiceClient messageServiceClient) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.messageServiceClient = messageServiceClient;
    }

    @Override
    public RoomResponse createRoom(CreateRoomRequest request) {
        // Duplicate DM prevention
        if (request.getType() == RoomType.DM && request.getRecipientId() != null) {
            List<Room> existingRooms = roomRepository.findRoomsByUserId(request.getCreatedById());
            for (Room r : existingRooms) {
                if (r.getType() == RoomType.DM &&
                    memberRepository.existsByRoomIdAndUserId(r.getRoomId(), request.getRecipientId())) {
                    return toRoomResponse(r);
                }
            }
        }

        if (request.getType() == RoomType.DM) {
            request.setMaxMembers(2);
            request.setIsPrivate(true);
        }

        Room room = new Room();
        room.setName(request.getName().trim());
        room.setDescription(request.getDescription());
        room.setType(request.getType());
        room.setCreatedById(request.getCreatedById());
        room.setAvatarUrl(request.getAvatarUrl());
        room.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() :
                request.getType() == RoomType.DM);
        room.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() :
                request.getType() == RoomType.DM ? 2 : 100);

        Room saved = roomRepository.save(room);

        // Auto-add creator as ADMIN
        RoomMember creatorMember = new RoomMember();
        creatorMember.setRoomId(saved.getRoomId());
        creatorMember.setUserId(request.getCreatedById());
        creatorMember.setRole(MemberRole.ADMIN);
        memberRepository.save(creatorMember);

        // Auto-add recipient for DM
        if (request.getType() == RoomType.DM && request.getRecipientId() != null) {
            RoomMember recipientMember = new RoomMember();
            recipientMember.setRoomId(saved.getRoomId());
            recipientMember.setUserId(request.getRecipientId());
            recipientMember.setRole(MemberRole.MEMBER);
            memberRepository.save(recipientMember);
        }

        return toRoomResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(UUID roomId) {
        return toRoomResponse(findRoom(roomId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByUser(UUID userId) {
        return roomRepository.findRoomsByUserId(userId)
                .stream()
                .map(this::toRoomResponse)
                .toList();
    }

    @Override
    public RoomResponse updateRoom(UUID roomId, UpdateRoomRequest request) {
        Room room = findRoom(roomId);

        if (request.getName() != null && !request.getName().isBlank()) {
            room.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            room.setDescription(request.getDescription());
        }
        if (request.getAvatarUrl() != null) {
            room.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getMaxMembers() != null) {
            if (room.getType() == RoomType.DM) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot change member limit for DM rooms");
            }
            room.setMaxMembers(request.getMaxMembers());
        }

        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    public void deleteRoom(UUID roomId) {
        findRoom(roomId);
        memberRepository.deleteByRoomId(roomId);
        roomRepository.deleteByRoomId(roomId);
    }

    @Override
    public MemberResponse addMember(UUID roomId, AddMemberRequest request) {
        Room room = findRoom(roomId);

        if (memberRepository.existsByRoomIdAndUserId(roomId, request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User is already a member of this room");
        }

        long currentCount = memberRepository.countByRoomId(roomId);
        if (currentCount >= room.getMaxMembers()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Room has reached its maximum member limit of " + room.getMaxMembers());
        }

        RoomMember member = new RoomMember();
        member.setRoomId(roomId);
        member.setUserId(request.getUserId());
        member.setRole(request.getRole() != null ? request.getRole() : MemberRole.MEMBER);

        return toMemberResponse(memberRepository.save(member));
    }

    @Override
    public void removeMember(UUID roomId, UUID userId) {
        findRoom(roomId);
        if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User is not a member of this room");
        }
        memberRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(UUID roomId) {
        findRoom(roomId);
        return memberRepository.findByRoomId(roomId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Override
    public MemberResponse updateMemberRole(UUID roomId, UUID userId, MemberRole role) {
        findRoom(roomId);
        RoomMember member = findMember(roomId, userId);
        member.setRole(role);
        return toMemberResponse(memberRepository.save(member));
    }

    @Override
    public MemberResponse muteUnmuteMember(UUID roomId, UUID userId, boolean mute) {
        findRoom(roomId);
        RoomMember member = findMember(roomId, userId);
        member.setIsMuted(mute);
        return toMemberResponse(memberRepository.save(member));
    }

    @Override
    public MemberResponse updateLastRead(UUID roomId, UUID userId) {
        findRoom(roomId);
        RoomMember member = findMember(roomId, userId);
        member.setLastReadAt(LocalDateTime.now());
        return toMemberResponse(memberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID roomId, UUID userId, LocalDateTime since) {
        findRoom(roomId);
        findMember(roomId, userId);
        return (int) messageServiceClient.getUnreadCount(roomId.toString(), since);
    }

    @Override
    public RoomResponse updateLastMessageAt(UUID roomId, LocalDateTime timestamp) {
        Room room = findRoom(roomId);
        room.setLastMessageAt(timestamp);
        return toRoomResponse(roomRepository.save(room));
    }

    // ─── GAP 10 FIX ───────────────────────────────────────────────────────────

    @Override
    public RoomResponse pinMessage(UUID roomId, String messageId) {
        if (messageId == null || messageId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "messageId must not be blank");
        }
        Room room = findRoom(roomId);
        room.setPinnedMessageId(messageId);
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    public RoomResponse unpinMessage(UUID roomId) {
        Room room = findRoom(roomId);
        room.setPinnedMessageId(null);
        return toRoomResponse(roomRepository.save(room));
    }

    // ─────────────────────────────────────────────────────────────────────────

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Room findRoom(UUID roomId) {
        return roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Room not found: " + roomId));
    }

    private RoomMember findMember(UUID roomId, UUID userId) {
        return memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Member not found in room"));
    }

    private RoomResponse toRoomResponse(Room room) {
        long memberCount = memberRepository.countByRoomId(room.getRoomId());
        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .name(room.getName())
                .description(room.getDescription())
                .type(room.getType())
                .createdById(room.getCreatedById())
                .avatarUrl(room.getAvatarUrl())
                .isPrivate(room.getIsPrivate())
                .maxMembers(room.getMaxMembers())
                .memberCount(memberCount)
                .lastMessageAt(room.getLastMessageAt())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                // GAP 10 FIX: expose pinnedMessageId in every room response
                .pinnedMessageId(room.getPinnedMessageId())
                .build();
    }

    private MemberResponse toMemberResponse(RoomMember member) {
        return MemberResponse.builder()
                .memberId(member.getMemberId())
                .roomId(member.getRoomId())
                .userId(member.getUserId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .lastReadAt(member.getLastReadAt())
                .isMuted(member.getIsMuted())
                .build();
    }
}

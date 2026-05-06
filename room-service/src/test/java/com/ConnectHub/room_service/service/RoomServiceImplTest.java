package com.connecthub.room_service.service;

import com.connecthub.room_service.dto.AddMemberRequest;
import com.connecthub.room_service.dto.CreateRoomRequest;
import com.connecthub.room_service.dto.MemberResponse;
import com.connecthub.room_service.dto.RoomResponse;
import com.connecthub.room_service.dto.UpdateRoomRequest;
import com.connecthub.room_service.client.MessageServiceClient;
import com.connecthub.room_service.model.MemberRole;
import com.connecthub.room_service.model.Room;
import com.connecthub.room_service.model.RoomMember;
import com.connecthub.room_service.model.RoomType;
import com.connecthub.room_service.repository.RoomMemberRepository;
import com.connecthub.room_service.repository.RoomRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomServiceImpl Unit Tests")
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository memberRepository;

    @Mock
    private MessageServiceClient messageServiceClient;

    @InjectMocks
    private RoomServiceImpl roomService;

    private UUID roomId;
    private UUID creatorId;
    private UUID userId;
    private Room testRoom;
    private RoomMember testMember;

    @BeforeEach
    void setUp() {
        roomId    = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        userId    = UUID.randomUUID();

        testRoom = new Room();
        testRoom.setRoomId(roomId);
        testRoom.setName("General");
        testRoom.setDescription("Main chat room");
        testRoom.setType(RoomType.GROUP);
        testRoom.setCreatedById(creatorId);
        testRoom.setIsPrivate(false);
        testRoom.setMaxMembers(100);
        testRoom.setCreatedAt(LocalDateTime.now());
        testRoom.setUpdatedAt(LocalDateTime.now());

        testMember = new RoomMember();
        testMember.setMemberId(1L);
        testMember.setRoomId(roomId);
        testMember.setUserId(userId);
        testMember.setRole(MemberRole.MEMBER);
        testMember.setJoinedAt(LocalDateTime.now());
        testMember.setIsMuted(false);
    }

    // ─── createRoom() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRoom: GROUP room — saves room and auto-adds creator as ADMIN")
    void createRoom_group_savesRoomAndAddsMember() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("General");
        request.setType(RoomType.GROUP);
        request.setCreatedById(creatorId);
        request.setIsPrivate(false);

        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setRoomId(roomId);
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            return r;
        });
        when(memberRepository.save(any(RoomMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.countByRoomId(roomId)).thenReturn(1L);

        RoomResponse response = roomService.createRoom(request);

        assertThat(response.getRoomId()).isEqualTo(roomId);
        assertThat(response.getName()).isEqualTo("General");
        verify(roomRepository).save(any(Room.class));
        // creator auto-added as ADMIN
        verify(memberRepository, atLeastOnce()).save(argThat(m ->
                m.getUserId().equals(creatorId) && m.getRole() == MemberRole.ADMIN));
    }

    @Test
    @DisplayName("createRoom: DM room — forces maxMembers=2, isPrivate=true, adds both parties")
    void createRoom_dm_forcesPrivateAndTwoMembers() {
        UUID recipientId = UUID.randomUUID();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("DM");
        request.setType(RoomType.DM);
        request.setCreatedById(creatorId);
        request.setRecipientId(recipientId);

        when(roomRepository.findRoomsByUserId(creatorId)).thenReturn(List.of());
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setRoomId(roomId);
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            return r;
        });
        when(memberRepository.save(any(RoomMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.countByRoomId(roomId)).thenReturn(2L);

        RoomResponse response = roomService.createRoom(request);

        assertThat(response.getMaxMembers()).isEqualTo(2);
        assertThat(response.getIsPrivate()).isTrue();
        // both creator and recipient added
        verify(memberRepository, times(2)).save(any(RoomMember.class));
    }

    @Test
    @DisplayName("createRoom: duplicate DM — returns existing room without creating new one")
    void createRoom_duplicateDm_returnsExisting() {
        UUID recipientId = UUID.randomUUID();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("DM");
        request.setType(RoomType.DM);
        request.setCreatedById(creatorId);
        request.setRecipientId(recipientId);

        testRoom.setType(RoomType.DM);
        when(roomRepository.findRoomsByUserId(creatorId)).thenReturn(List.of(testRoom));
        when(memberRepository.existsByRoomIdAndUserId(roomId, recipientId)).thenReturn(true);
        when(memberRepository.countByRoomId(roomId)).thenReturn(2L);

        RoomResponse response = roomService.createRoom(request);

        assertThat(response.getRoomId()).isEqualTo(roomId);
        verify(roomRepository, never()).save(any());
    }

    // ─── getRoomById() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRoomById: existing room — returns correct RoomResponse")
    void getRoomById_found_returnsResponse() {
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.countByRoomId(roomId)).thenReturn(5L);

        RoomResponse response = roomService.getRoomById(roomId);

        assertThat(response.getRoomId()).isEqualTo(roomId);
        assertThat(response.getMemberCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getRoomById: room not found — throws 404 NOT_FOUND")
    void getRoomById_notFound_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(roomRepository.findByRoomId(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomById(unknown))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ─── addMember() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addMember: new member — saves with MEMBER role")
    void addMember_success_savesNewMember() {
        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(userId);
        request.setRole(MemberRole.MEMBER);

        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(false);
        when(memberRepository.countByRoomId(roomId)).thenReturn(1L);
        when(memberRepository.save(any(RoomMember.class))).thenReturn(testMember);

        MemberResponse response = roomService.addMember(roomId, request);

        assertThat(response.getUserId()).isEqualTo(userId);
        verify(memberRepository).save(any(RoomMember.class));
    }

    @Test
    @DisplayName("addMember: already a member — throws 409 CONFLICT")
    void addMember_alreadyMember_throwsConflict() {
        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(userId);

        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(true);

        assertThatThrownBy(() -> roomService.addMember(roomId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("addMember: room at capacity — throws 400 BAD_REQUEST")
    void addMember_roomFull_throwsBadRequest() {
        testRoom.setMaxMembers(2);
        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(userId);

        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(false);
        when(memberRepository.countByRoomId(roomId)).thenReturn(2L); // already at max

        assertThatThrownBy(() -> roomService.addMember(roomId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ─── removeMember() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("removeMember: member exists — deletes successfully")
    void removeMember_exists_deletesSuccessfully() {
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(true);

        roomService.removeMember(roomId, userId);

        verify(memberRepository).deleteByRoomIdAndUserId(roomId, userId);
    }

    @Test
    @DisplayName("removeMember: member not in room — throws 404 NOT_FOUND")
    void removeMember_notMember_throwsNotFound() {
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(false);

        assertThatThrownBy(() -> roomService.removeMember(roomId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ─── updateRoom() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateRoom: change name — persists updated name")
    void updateRoom_changeName_persistsUpdate() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setName("Updated Room");

        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.countByRoomId(roomId)).thenReturn(3L);

        RoomResponse response = roomService.updateRoom(roomId, request);

        assertThat(response.getName()).isEqualTo("Updated Room");
        verify(roomRepository).save(testRoom);
    }

    @Test
    @DisplayName("updateRoom: DM room cannot change maxMembers — throws 400 BAD_REQUEST")
    void updateRoom_dmMaxMembers_throwsBadRequest() {
        testRoom.setType(RoomType.DM);
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setMaxMembers(10);

        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));

        assertThatThrownBy(() -> roomService.updateRoom(roomId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ─── muteUnmuteMember() ───────────────────────────────────────────────────

    @Test
    @DisplayName("muteUnmuteMember: mute — sets isMuted=true")
    void muteUnmuteMember_mute_setsMuted() {
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findByRoomIdAndUserId(roomId, userId)).thenReturn(Optional.of(testMember));
        when(memberRepository.save(any(RoomMember.class))).thenReturn(testMember);

        roomService.muteUnmuteMember(roomId, userId, true);

        assertThat(testMember.getIsMuted()).isTrue();
        verify(memberRepository).save(testMember);
    }

    // ─── pinMessage() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("pinMessage: valid messageId — persists pinnedMessageId")
    void pinMessage_valid_persistsPinned() {
        String messageId = UUID.randomUUID().toString();
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.countByRoomId(roomId)).thenReturn(2L);

        RoomResponse response = roomService.pinMessage(roomId, messageId);

        assertThat(testRoom.getPinnedMessageId()).isEqualTo(messageId);
    }

    @Test
    @DisplayName("pinMessage: blank messageId — throws 400 BAD_REQUEST")
    void pinMessage_blank_throwsBadRequest() {
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));

        assertThatThrownBy(() -> roomService.pinMessage(roomId, "  "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ─── deleteRoom() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteRoom: existing room — removes members then room")
    void deleteRoom_existing_deletesAll() {
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));

        roomService.deleteRoom(roomId);

        verify(memberRepository).deleteByRoomId(roomId);
        verify(roomRepository).deleteByRoomId(roomId);
    }
}

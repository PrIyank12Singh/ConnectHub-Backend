package com.ConnectHub.message_service.service;

import com.ConnectHub.message_service.dto.EditMessageRequest;
import com.ConnectHub.message_service.dto.MessageResponse;
import com.ConnectHub.message_service.dto.SendMessageRequest;
import com.ConnectHub.message_service.model.DeliveryStatus;
import com.ConnectHub.message_service.model.Message;
import com.ConnectHub.message_service.model.MessageType;
import com.ConnectHub.message_service.repository.AuditLogRepository;
import com.ConnectHub.message_service.repository.MessageRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageServiceImpl Unit Tests")
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private UUID messageId;
    private UUID roomId;
    private UUID senderId;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        messageId = UUID.randomUUID();
        roomId    = UUID.randomUUID();
        senderId  = UUID.randomUUID();

        testMessage = new Message();
        testMessage.setMessageId(messageId);
        testMessage.setRoomId(roomId);
        testMessage.setSenderId(senderId);
        testMessage.setContent("Hello, world!");
        testMessage.setType(MessageType.TEXT);
        testMessage.setIsEdited(false);
        testMessage.setIsDeleted(false);
        testMessage.setDeliveryStatus(DeliveryStatus.SENT);
        testMessage.setSentAt(LocalDateTime.now());
    }

    // ─── sendMessage() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendMessage: TEXT message — persists and returns MessageResponse")
    void sendMessage_textMessage_persistsAndReturns() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(roomId);
        request.setSenderId(senderId);
        request.setContent("Hello, world!");
        request.setType(MessageType.TEXT);

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setMessageId(messageId);
            m.setSentAt(LocalDateTime.now());
            m.setIsEdited(false);
            m.setIsDeleted(false);
            m.setDeliveryStatus(DeliveryStatus.SENT);
            return m;
        });

        MessageResponse response = messageService.sendMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Hello, world!");
        assertThat(response.getType()).isEqualTo(MessageType.TEXT);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("sendMessage: TEXT with blank content — throws 400 BAD_REQUEST")
    void sendMessage_blankTextContent_throwsBadRequest() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(roomId);
        request.setSenderId(senderId);
        request.setContent("   ");
        request.setType(MessageType.TEXT);

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage: IMAGE without mediaUrl — throws 400 BAD_REQUEST")
    void sendMessage_imageWithoutMediaUrl_throwsBadRequest() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(roomId);
        request.setSenderId(senderId);
        request.setType(MessageType.IMAGE);
        request.setMediaUrl(null);

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("sendMessage: null type — defaults to TEXT")
    void sendMessage_nullType_defaultsToText() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(roomId);
        request.setSenderId(senderId);
        request.setContent("Hello");
        request.setType(null);

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setMessageId(messageId);
            m.setSentAt(LocalDateTime.now());
            m.setIsEdited(false);
            m.setIsDeleted(false);
            m.setDeliveryStatus(DeliveryStatus.SENT);
            return m;
        });

        MessageResponse response = messageService.sendMessage(request);

        assertThat(response.getType()).isEqualTo(MessageType.TEXT);
    }

    // ─── getMessageById() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getMessageById: existing message — returns MessageResponse")
    void getMessageById_found_returnsResponse() {
        when(messageRepository.findByMessageId(messageId)).thenReturn(Optional.of(testMessage));

        MessageResponse response = messageService.getMessageById(messageId);

        assertThat(response.getMessageId()).isEqualTo(messageId);
        assertThat(response.getContent()).isEqualTo("Hello, world!");
    }

    @Test
    @DisplayName("getMessageById: not found — throws 404 NOT_FOUND")
    void getMessageById_notFound_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(messageRepository.findByMessageId(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.getMessageById(unknown))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ─── getMessagesByRoom() ──────────────────────────────────────────────────

    @Test
    @DisplayName("getMessagesByRoom: returns paginated messages")
    void getMessagesByRoom_returnsPaginatedResults() {
        Page<Message> page = new PageImpl<>(List.of(testMessage));
        when(messageRepository.findByRoomIdAndIsDeletedFalseOrderBySentAtDesc(
                eq(roomId), any(Pageable.class))).thenReturn(page);

        Page<MessageResponse> result = messageService.getMessagesByRoom(roomId, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Hello, world!");
    }

    // ─── editMessage() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("editMessage: valid edit — updates content, sets isEdited=true, saves audit log")
    void editMessage_valid_updatesContent() {
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("Updated message content");

        when(messageRepository.findByMessageId(messageId)).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = messageService.editMessage(messageId, request);

        assertThat(response.getContent()).isEqualTo("Updated message content");
        assertThat(testMessage.getIsEdited()).isTrue();
        assertThat(testMessage.getEditedAt()).isNotNull();
        verify(messageRepository).save(testMessage);
    }

    @Test
    @DisplayName("editMessage: deleted message — throws 400 BAD_REQUEST")
    void editMessage_deletedMessage_throwsBadRequest() {
        testMessage.setIsDeleted(true);
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("Too late");

        when(messageRepository.findByMessageId(messageId)).thenReturn(Optional.of(testMessage));

        assertThatThrownBy(() -> messageService.editMessage(messageId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(messageRepository, never()).save(any());
    }

    // ─── deleteMessage() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteMessage: existing message — soft-deletes and masks content")
    void deleteMessage_existing_softDeletes() {
        when(messageRepository.findByMessageId(messageId)).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        messageService.deleteMessage(messageId);

        assertThat(testMessage.getIsDeleted()).isTrue();
        assertThat(testMessage.getContent()).isEqualTo("[Message deleted]");
        verify(messageRepository).save(testMessage);
    }

    @Test
    @DisplayName("deleteMessage: not found — throws 404 NOT_FOUND")
    void deleteMessage_notFound_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(messageRepository.findByMessageId(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.deleteMessage(unknown))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ─── searchMessages() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("searchMessages: matching keyword — returns list")
    void searchMessages_matchingKeyword_returnsList() {
        when(messageRepository.searchInRoom(roomId, "world")).thenReturn(List.of(testMessage));

        List<MessageResponse> result = messageService.searchMessages(roomId, "world");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).contains("world");
    }

    @Test
    @DisplayName("searchMessages: blank keyword — returns empty list without hitting repository")
    void searchMessages_blankKeyword_returnsEmpty() {
        List<MessageResponse> result = messageService.searchMessages(roomId, "  ");

        assertThat(result).isEmpty();
        verify(messageRepository, never()).searchInRoom(any(), any());
    }

    // ─── updateDeliveryStatus() ───────────────────────────────────────────────

    @Test
    @DisplayName("updateDeliveryStatus: DELIVERED — updates status field")
    void updateDeliveryStatus_delivered_updatesField() {
        when(messageRepository.findByMessageId(messageId)).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        messageService.updateDeliveryStatus(messageId, DeliveryStatus.DELIVERED);

        assertThat(testMessage.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(messageRepository).save(testMessage);
    }

    @Test
    @DisplayName("updateDeliveryStatus: READ — updates status to READ")
    void updateDeliveryStatus_read_updatesField() {
        when(messageRepository.findByMessageId(messageId)).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        messageService.updateDeliveryStatus(messageId, DeliveryStatus.READ);

        assertThat(testMessage.getDeliveryStatus()).isEqualTo(DeliveryStatus.READ);
    }

    // ─── getMessageCount() ────────────────────────────────────────────────────

    @Test
    @DisplayName("getMessageCount: returns count from repository")
    void getMessageCount_returnsCount() {
        when(messageRepository.countByRoomIdAndIsDeletedFalse(roomId)).thenReturn(42L);

        long count = messageService.getMessageCount(roomId);

        assertThat(count).isEqualTo(42L);
    }

    // ─── getMessagesBefore() ──────────────────────────────────────────────────

    @Test
    @DisplayName("getMessagesBefore: returns messages before given timestamp")
    void getMessagesBefore_returnsCorrectMessages() {
        LocalDateTime before = LocalDateTime.now();
        when(messageRepository.findByRoomIdAndSentAtBeforeAndIsDeletedFalseOrderBySentAtDesc(
                eq(roomId), eq(before), any(Pageable.class)))
                .thenReturn(List.of(testMessage));

        List<MessageResponse> result = messageService.getMessagesBefore(roomId, before, 10);

        assertThat(result).hasSize(1);
        verify(messageRepository).findByRoomIdAndSentAtBeforeAndIsDeletedFalseOrderBySentAtDesc(
                eq(roomId), eq(before), any(Pageable.class));
    }
}

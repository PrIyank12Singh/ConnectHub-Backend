package com.ConnectHub.message_service.service;

import com.ConnectHub.message_service.dto.EditMessageRequest;
import com.ConnectHub.message_service.dto.MessageResponse;
import com.ConnectHub.message_service.dto.SendMessageRequest;
import com.ConnectHub.message_service.model.DeliveryStatus;
import com.ConnectHub.message_service.model.Message;
import com.ConnectHub.message_service.model.MessageType;
import com.ConnectHub.message_service.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    public MessageServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public MessageResponse sendMessage(SendMessageRequest request) {
        if (request.getType() == null) {
            request.setType(MessageType.TEXT);
        }

        if (request.getType() == MessageType.TEXT && 
            (request.getContent() == null || request.getContent().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Content cannot be empty for TEXT messages");
        }

        Message message = new Message();
        message.setRoomId(request.getRoomId());
        message.setSenderId(request.getSenderId());
        message.setContent(request.getContent());
        message.setType(request.getType());
        message.setMediaUrl(request.getMediaUrl());
        message.setReplyToMessageId(request.getReplyToMessageId());

        return toMessageResponse(messageRepository.save(message));
    }

    @Override
    @Transactional(readOnly = true)
    public MessageResponse getMessageById(UUID messageId) {
        return toMessageResponse(findMessage(messageId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesByRoom(UUID roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository
                .findByRoomIdAndIsDeletedFalseOrderBySentAtDesc(roomId, pageable)
                .map(this::toMessageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesBefore(UUID roomId, LocalDateTime before, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return messageRepository
                .findByRoomIdAndSentAtBeforeAndIsDeletedFalseOrderBySentAtDesc(roomId, before, pageable)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    public MessageResponse editMessage(UUID messageId, EditMessageRequest request) {
        Message message = findMessage(messageId);

        if (message.getIsDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot edit a deleted message");
        }

        message.setContent(request.getContent().trim());
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        return toMessageResponse(messageRepository.save(message));
    }

    @Override
    public void deleteMessage(UUID messageId) {
        Message message = findMessage(messageId);
        message.setIsDeleted(true);
        message.setContent("[Message deleted]");
        messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> searchMessages(UUID roomId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return messageRepository.searchInRoom(roomId, keyword.trim())
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    public void updateDeliveryStatus(UUID messageId, DeliveryStatus status) {
        Message message = findMessage(messageId);
        message.setDeliveryStatus(status);
        messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public long getMessageCount(UUID roomId) {
        return messageRepository.countByRoomIdAndIsDeletedFalse(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getUnreadMessages(UUID roomId, LocalDateTime after) {
        return messageRepository.findUnreadMessages(roomId, after)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Message findMessage(UUID messageId) {
        return messageRepository.findByMessageId(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Message not found: " + messageId));
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .type(message.getType())
                .mediaUrl(message.getMediaUrl())
                .replyToMessageId(message.getReplyToMessageId())
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted())
                .deliveryStatus(message.getDeliveryStatus())
                .sentAt(message.getSentAt())
                .editedAt(message.getEditedAt())
                .build();
    }
}

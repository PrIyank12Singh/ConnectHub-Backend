package com.ConnectHub.message_service.service;

import com.ConnectHub.message_service.dto.EditMessageRequest;
import com.ConnectHub.message_service.dto.MessageResponse;
import com.ConnectHub.message_service.dto.SendMessageRequest;
import com.ConnectHub.message_service.model.DeliveryStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface MessageService {

    MessageResponse sendMessage(SendMessageRequest request);

    MessageResponse getMessageById(UUID messageId);

    Page<MessageResponse> getMessagesByRoom(UUID roomId, int page, int size);

    List<MessageResponse> getMessagesBefore(UUID roomId, LocalDateTime before, int limit);

    MessageResponse editMessage(UUID messageId, EditMessageRequest request);

    void deleteMessage(UUID messageId);

    List<MessageResponse> searchMessages(UUID roomId, String keyword);

    void updateDeliveryStatus(UUID messageId, DeliveryStatus status);

    long getMessageCount(UUID roomId);

    List<MessageResponse> getUnreadMessages(UUID roomId, LocalDateTime after);
}

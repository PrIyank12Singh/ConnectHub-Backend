package com.ConnectHub.message_service.dto;

import com.ConnectHub.message_service.model.DeliveryStatus;
import com.ConnectHub.message_service.model.MessageType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageResponse {

    private UUID messageId;
    private UUID roomId;
    private UUID senderId;
    private String content;
    private MessageType type;
    private String mediaUrl;
    private UUID replyToMessageId;
    private Boolean isEdited;
    private Boolean isDeleted;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;
}

package com.ConnectHub.message_service.dto;

import com.ConnectHub.message_service.model.MessageType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotNull
    private UUID roomId;

    @NotNull
    private UUID senderId;

    private String content;

    private MessageType type;

    private String mediaUrl;

    private UUID replyToMessageId;
}

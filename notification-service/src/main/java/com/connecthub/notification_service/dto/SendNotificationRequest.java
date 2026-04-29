package com.connecthub.notification_service.dto;

import com.connecthub.notification_service.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @NotBlank(message = "recipientId is required")
    private String recipientId;

    /** actorId is optional — null for SYSTEM notifications */
    private String actorId;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    /** Optional — room context */
    private String roomId;

    /** Optional — message context */
    private String messageId;
}



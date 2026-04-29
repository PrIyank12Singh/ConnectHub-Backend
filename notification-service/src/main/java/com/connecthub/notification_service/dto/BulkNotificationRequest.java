package com.ConnectHub.notification_service.dto;

import com.ConnectHub.notification_service.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class BulkNotificationRequest {

    @NotEmpty(message = "At least one recipientId is required")
    private List<String> recipientIds;

    /** actorId optional */
    private String actorId;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    private String roomId;
    private String messageId;
}

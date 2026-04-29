package com.connecthub.notification_service.dto;

import com.connecthub.notification_service.model.NotificationType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long notificationId;
    private String recipientId;
    private String actorId;
    private NotificationType type;
    private String title;
    private String message;
    private String roomId;
    private String messageId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}



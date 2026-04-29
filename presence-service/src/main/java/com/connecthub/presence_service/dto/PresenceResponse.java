package com.ConnectHub.presence_service.dto;

import com.ConnectHub.presence_service.model.UserStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceResponse {

    private Long presenceId;
    private String userId;
    private UserStatus status;
    private String customMessage;
    private String deviceType;
    private LocalDateTime connectedAt;
    private LocalDateTime lastPingAt;
    private String sessionId;
}

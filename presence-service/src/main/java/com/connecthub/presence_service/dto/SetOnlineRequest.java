package com.ConnectHub.presence_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetOnlineRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "sessionId is required")
    private String sessionId;

    /** WEB | MOBILE | DESKTOP */
    private String deviceType;

    private String ipAddress;
}

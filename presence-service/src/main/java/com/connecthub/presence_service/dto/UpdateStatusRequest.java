package com.connecthub.presence_service.dto;

import com.connecthub.presence_service.model.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotNull(message = "status is required")
    private UserStatus status;

    /** Optional free-text, max 160 chars */
    private String customMessage;
}



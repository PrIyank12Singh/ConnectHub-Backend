package com.connecthub.presence_service.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class BulkPresenceRequest {

    @NotEmpty(message = "At least one userId is required")
    private List<String> userIds;
}



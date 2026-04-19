package com.ConnectHub.auth_service.dto;

import com.ConnectHub.auth_service.model.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private UUID userId;
    private String username;
    private String email;
    private UserRole role;
    private String accessToken;
    private String tokenType;
    private LocalDateTime issuedAt;
}

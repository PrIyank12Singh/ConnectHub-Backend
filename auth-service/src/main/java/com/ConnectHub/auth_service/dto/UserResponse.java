package com.ConnectHub.auth_service.dto;

import com.ConnectHub.auth_service.model.AuthProvider;
import com.ConnectHub.auth_service.model.UserRole;
import com.ConnectHub.auth_service.model.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private UUID userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private UserStatus status;
    private AuthProvider provider;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
}

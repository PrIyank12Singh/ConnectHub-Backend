package com.connecthub.auth_service.service;

import com.connecthub.auth_service.dto.AuthResponse;
import com.connecthub.auth_service.dto.ChangePasswordRequest;
import com.connecthub.auth_service.dto.LoginRequest;
import com.connecthub.auth_service.dto.RegisterRequest;
import com.connecthub.auth_service.dto.UpdateProfileRequest;
import com.connecthub.auth_service.dto.UserResponse;
import com.connecthub.auth_service.model.UserStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String token);

    boolean validateToken(String token);

    AuthResponse refreshToken(String token);

    UserResponse getUserById(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserResponse updateAvatar(UUID userId, MultipartFile avatarFile);

    void changePassword(UUID userId, ChangePasswordRequest request);

    List<UserResponse> searchUsers(String username);

    UserResponse updateStatus(UUID userId, UserStatus status);

    UserResponse recordLastSeen(UUID userId);
}



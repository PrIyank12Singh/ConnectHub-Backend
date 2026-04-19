package com.ConnectHub.auth_service.service;

import com.ConnectHub.auth_service.dto.AuthResponse;
import com.ConnectHub.auth_service.dto.ChangePasswordRequest;
import com.ConnectHub.auth_service.dto.LoginRequest;
import com.ConnectHub.auth_service.dto.RegisterRequest;
import com.ConnectHub.auth_service.dto.UpdateProfileRequest;
import com.ConnectHub.auth_service.dto.UserResponse;
import com.ConnectHub.auth_service.model.AuthProvider;
import com.ConnectHub.auth_service.model.User;
import com.ConnectHub.auth_service.model.UserStatus;
import com.ConnectHub.auth_service.repository.UserRepository;
import com.ConnectHub.auth_service.security.JwtUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setFullName(request.getFullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProvider(AuthProvider.LOCAL);
        user.setStatus(UserStatus.ONLINE);
        user.setIsActive(true);

        return toUserResponse(userRepository.save(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.setStatus(UserStatus.ONLINE);
        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (!jwtUtil.validateToken(token)) {
            return;
        }

        UUID userId = jwtUtil.extractUserId(token);
        recordLastSeen(userId);
    }

    @Override
    public boolean validateToken(String token) {
        return token != null && !token.isBlank() && jwtUtil.validateToken(token);
    }

    @Override
    public AuthResponse refreshToken(String token) {
        if (!validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        UUID userId = jwtUtil.extractUserId(token);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setFullName(request.getFullName().trim());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setBio(request.getBio());

        return toUserResponse(userRepository.save(user));
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }

        return userRepository.findByUsernameContainingIgnoreCase(username.trim())
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Override
    public UserResponse updateStatus(UUID userId, UserStatus status) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setStatus(status);
        if (status == UserStatus.INVISIBLE) {
            user.setLastSeenAt(LocalDateTime.now());
        }

        return toUserResponse(userRepository.save(user));
    }

    @Override
    public UserResponse recordLastSeen(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setStatus(UserStatus.INVISIBLE);
        user.setLastSeenAt(LocalDateTime.now());

        return toUserResponse(userRepository.save(user));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .tokenType("Bearer")
                .issuedAt(LocalDateTime.now())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .status(user.getStatus())
                .provider(user.getProvider())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .lastSeenAt(user.getLastSeenAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

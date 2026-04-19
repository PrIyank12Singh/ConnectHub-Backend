package com.ConnectHub.auth_service.service;

import com.ConnectHub.auth_service.dto.AuthResponse;
import com.ConnectHub.auth_service.model.AuthProvider;
import com.ConnectHub.auth_service.model.User;
import com.ConnectHub.auth_service.model.UserRole;
import com.ConnectHub.auth_service.model.UserStatus;
import com.ConnectHub.auth_service.repository.UserRepository;
import com.ConnectHub.auth_service.security.JwtUtil;
import java.time.LocalDateTime;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public GoogleAuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse handleGoogleLogin(OAuth2User oauthUser) {
        String email     = oauthUser.getAttribute("email");
        String fullName  = oauthUser.getAttribute("name");
        String avatarUrl = oauthUser.getAttribute("picture");

        // Find existing user or create a new one
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // Derive a unique username from the email prefix
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
            String username = baseUsername;
            int suffix = 1;
            while (userRepository.existsByUsername(username)) {
                username = baseUsername + suffix++;
            }

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(fullName != null ? fullName : email);
            newUser.setUsername(username);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setPasswordHash("GOOGLE_AUTH_NO_PASSWORD");
            newUser.setProvider(AuthProvider.GOOGLE);
            newUser.setRole(UserRole.USER);
            newUser.setStatus(UserStatus.ONLINE);
            newUser.setIsActive(true);
            return userRepository.save(newUser);
        });

        // Update status & last seen on every login
        user.setStatus(UserStatus.ONLINE);
        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);

        // Issue your own JWT token
        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(token)
                .tokenType("Bearer")
                .issuedAt(LocalDateTime.now())
                .build();
    }
}

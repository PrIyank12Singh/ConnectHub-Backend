package com.ConnectHub.auth_service.resource;

import com.ConnectHub.auth_service.dto.AuthResponse;
import com.ConnectHub.auth_service.dto.ChangePasswordRequest;
import com.ConnectHub.auth_service.dto.LoginRequest;
import com.ConnectHub.auth_service.dto.RegisterRequest;
import com.ConnectHub.auth_service.dto.UpdateProfileRequest;
import com.ConnectHub.auth_service.dto.UserResponse;
import com.ConnectHub.auth_service.model.UserStatus;
import com.ConnectHub.auth_service.service.AuthService;
import com.ConnectHub.auth_service.service.GoogleAuthService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthResource {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public AuthResource(AuthService authService, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    // ─── Local Auth ───────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(extractToken(authHeader));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        boolean valid = authService.validateToken(extractToken(authHeader));
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(authService.refreshToken(extractToken(authHeader)));
    }

    // ─── Google OAuth2 ────────────────────────────────────────────────────────

    @GetMapping("/google/callback")
    public ResponseEntity<AuthResponse> googleCallback(
            @AuthenticationPrincipal OAuth2User oauthUser) {
        return ResponseEntity.ok(googleAuthService.handleGoogleLogin(oauthUser));
    }

    // ─── Profile & User Management ───────────────────────────────────────────

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserResponse> getProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/password/{userId}")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam("username") String username) {
        return ResponseEntity.ok(authService.searchUsers(username));
    }

    @PutMapping("/status/{userId}")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable UUID userId,
            @RequestParam UserStatus status) {
        return ResponseEntity.ok(authService.updateStatus(userId, status));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return authHeader;
    }
}

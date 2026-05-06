package com.connecthub.auth_service.service;

import com.connecthub.auth_service.dto.AuthResponse;
import com.connecthub.auth_service.dto.ChangePasswordRequest;
import com.connecthub.auth_service.dto.LoginRequest;
import com.connecthub.auth_service.dto.RegisterRequest;
import com.connecthub.auth_service.dto.UpdateProfileRequest;
import com.connecthub.auth_service.dto.UserResponse;
import com.connecthub.auth_service.model.AuthProvider;
import com.connecthub.auth_service.model.User;
import com.connecthub.auth_service.model.UserRole;
import com.connecthub.auth_service.model.UserStatus;
import com.connecthub.auth_service.repository.UserRepository;
import com.connecthub.auth_service.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private MediaServiceClient mediaServiceClient;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setUserId(testUserId);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setPasswordHash("$2a$10$hashedpassword");
        testUser.setFullName("John Doe");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setProvider(AuthProvider.LOCAL);
        testUser.setRole(UserRole.USER);
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    // ─── register() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: success — returns UserResponse with correct fields")
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("secret123");
        request.setFullName("John Doe");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(testUserId);
            u.setRole(UserRole.USER);
            u.setCreatedAt(LocalDateTime.now());
            u.setUpdatedAt(LocalDateTime.now());
            return u;
        });

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getUsername()).isEqualTo("john_doe");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: duplicate email — throws 409 CONFLICT")
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("secret123");
        request.setFullName("John Doe");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: duplicate username — throws 409 CONFLICT")
    void register_duplicateUsername_throwsConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("new@example.com");
        request.setPassword("secret123");
        request.setFullName("John Doe");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // ─── login() ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: valid credentials — returns AuthResponse with token")
    void login_validCredentials_returnsToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("secret123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(testUserId, "john@example.com", "USER")).thenReturn("mock.jwt.token");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("login: user not found — throws 401 UNAUTHORIZED")
    void login_userNotFound_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@example.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("login: wrong password — throws 401 UNAUTHORIZED")
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("login: inactive account — throws 403 FORBIDDEN")
    void login_inactiveUser_throwsForbidden() {
        testUser.setIsActive(false);
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ─── getUserById() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById: existing user — returns UserResponse")
    void getUserById_found_returnsUserResponse() {
        when(userRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUser));

        UserResponse response = authService.getUserById(testUserId);

        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("getUserById: not found — throws 404 NOT_FOUND")
    void getUserById_notFound_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findByUserId(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(unknown))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ─── updateProfile() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfile: valid update — saves and returns updated response")
    void updateProfile_valid_savesChanges() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Johnny Updated");
        request.setAvatarUrl("https://cdn.example.com/avatar.png");
        request.setBio("Updated bio");

        when(userRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.updateProfile(testUserId, request);

        assertThat(response.getFullName()).isEqualTo("Johnny Updated");
        assertThat(response.getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
        verify(userRepository).save(testUser);
    }

    // ─── changePassword() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword: correct current password — encodes and saves new password")
    void changePassword_correct_encodesNewPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldpass");
        request.setNewPassword("newpass123");

        when(userRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldpass", testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("$2a$10$newhashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.changePassword(testUserId, request);

        verify(passwordEncoder).encode("newpass123");
        verify(userRepository).save(testUser);
        assertThat(testUser.getPasswordHash()).isEqualTo("$2a$10$newhashedpassword");
    }

    @Test
    @DisplayName("changePassword: wrong current password — throws 400 BAD_REQUEST")
    void changePassword_wrongCurrent_throwsBadRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongpass");
        request.setNewPassword("newpass123");

        when(userRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpass", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(testUserId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).save(any());
    }

    // ─── searchUsers() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchUsers: matching query — returns list of UserResponse")
    void searchUsers_matchingQuery_returnsList() {
        when(userRepository.findByUsernameContainingIgnoreCase("john")).thenReturn(List.of(testUser));

        List<UserResponse> result = authService.searchUsers("john");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("searchUsers: blank query — returns empty list without hitting repository")
    void searchUsers_blankQuery_returnsEmpty() {
        List<UserResponse> result = authService.searchUsers("  ");

        assertThat(result).isEmpty();
        verify(userRepository, never()).findByUsernameContainingIgnoreCase(anyString());
    }

    // ─── updateStatus() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: INVISIBLE — records lastSeenAt")
    void updateStatus_invisible_recordsLastSeen() {
        when(userRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.updateStatus(testUserId, UserStatus.INVISIBLE);

        assertThat(testUser.getStatus()).isEqualTo(UserStatus.INVISIBLE);
        assertThat(testUser.getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus: AWAY — sets status without touching lastSeenAt")
    void updateStatus_away_setsStatus() {
        when(userRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.updateStatus(testUserId, UserStatus.AWAY);

        assertThat(testUser.getStatus()).isEqualTo(UserStatus.AWAY);
        verify(userRepository).save(testUser);
    }

    // ─── validateToken() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken: valid token — returns true")
    void validateToken_validToken_returnsTrue() {
        when(jwtUtil.validateToken("valid.jwt.token")).thenReturn(true);

        assertThat(authService.validateToken("valid.jwt.token")).isTrue();
    }

    @Test
    @DisplayName("validateToken: null token — returns false")
    void validateToken_nullToken_returnsFalse() {
        assertThat(authService.validateToken(null)).isFalse();
        verify(jwtUtil, never()).validateToken(any());
    }
}

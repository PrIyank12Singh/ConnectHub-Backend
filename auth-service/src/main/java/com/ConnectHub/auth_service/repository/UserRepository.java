package com.ConnectHub.auth_service.repository;

import com.ConnectHub.auth_service.model.User;
import com.ConnectHub.auth_service.model.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUserId(UUID userId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByStatus(UserStatus status);

    List<User> findByUsernameContainingIgnoreCase(String username);

    void deleteByUserId(UUID userId);
}

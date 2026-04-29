package com.ConnectHub.presence_service.repository;

import com.ConnectHub.presence_service.model.UserPresence;
import com.ConnectHub.presence_service.model.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PresenceRepository extends JpaRepository<UserPresence, Long> {

    /** Find all sessions belonging to a user */
    List<UserPresence> findByUserId(String userId);

    /** Find a specific session */
    Optional<UserPresence> findBySessionId(String sessionId);

    /** Find the most recent session of a user (for single-presence reads) */
    Optional<UserPresence> findTopByUserIdOrderByLastPingAtDesc(String userId);

    /** Bulk presence: get presence for a list of userIds */
    @Query("SELECT p FROM UserPresence p WHERE p.userId IN :userIds " +
           "ORDER BY p.lastPingAt DESC")
    List<UserPresence> findByUserIdIn(@Param("userIds") List<String> userIds);

    /** All sessions by status */
    List<UserPresence> findByStatus(UserStatus status);

    /** All ONLINE sessions */
    @Query("SELECT p FROM UserPresence p WHERE p.status = 'ONLINE'")
    List<UserPresence> findOnlineUsers();

    /** Count of distinct online users */
    @Query("SELECT COUNT(DISTINCT p.userId) FROM UserPresence p WHERE p.status = 'ONLINE'")
    int countDistinctOnlineUsers();

    /** Check if any active session exists for a user */
    boolean existsByUserId(String userId);

    /** Delete all sessions for a user (on logout / account delete) */
    @Modifying
    @Query("DELETE FROM UserPresence p WHERE p.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);

    /** Delete a specific session */
    @Modifying
    @Query("DELETE FROM UserPresence p WHERE p.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") String sessionId);

    /** Find sessions that have not pinged since a given time (stale) */
    @Query("SELECT p FROM UserPresence p WHERE p.lastPingAt < :cutoff")
    List<UserPresence> findStaleSessions(@Param("cutoff") LocalDateTime cutoff);

    /** Delete all sessions older than a given lastPingAt (bulk stale cleanup) */
    @Modifying
    @Query("DELETE FROM UserPresence p WHERE p.lastPingAt < :cutoff")
    int deleteStaleSessionsBefore(@Param("cutoff") LocalDateTime cutoff);
}

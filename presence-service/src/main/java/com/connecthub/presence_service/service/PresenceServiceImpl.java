package com.ConnectHub.presence_service.service;

import com.ConnectHub.presence_service.dto.BulkPresenceRequest;
import com.ConnectHub.presence_service.dto.PresenceResponse;
import com.ConnectHub.presence_service.dto.SetOnlineRequest;
import com.ConnectHub.presence_service.dto.UpdateStatusRequest;
import com.ConnectHub.presence_service.model.UserPresence;
import com.ConnectHub.presence_service.model.UserStatus;
import com.ConnectHub.presence_service.repository.PresenceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PresenceServiceImpl implements PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceServiceImpl.class);

    private final PresenceRepository presenceRepository;

    @Value("${presence.stale.timeout-seconds:60}")
    private int staleTimeoutSeconds;

    public PresenceServiceImpl(PresenceRepository presenceRepository) {
        this.presenceRepository = presenceRepository;
    }

    // ─── Set Online ───────────────────────────────────────────────────────────

    @Override
    public PresenceResponse setOnline(SetOnlineRequest request) {
        // Upsert: if the session already exists refresh it, otherwise create new
        UserPresence presence = presenceRepository.findBySessionId(request.getSessionId())
                .orElseGet(UserPresence::new);

        presence.setUserId(request.getUserId());
        presence.setSessionId(request.getSessionId());
        presence.setStatus(UserStatus.ONLINE);
        presence.setDeviceType(
                request.getDeviceType() != null ? request.getDeviceType() : "WEB");
        presence.setIpAddress(request.getIpAddress());
        presence.setLastPingAt(LocalDateTime.now());

        if (presence.getPresenceId() == null) {
            // New session — connectedAt set by @PrePersist
        }

        UserPresence saved = presenceRepository.save(presence);
        log.info("User {} is now ONLINE (session: {})", request.getUserId(), request.getSessionId());
        return toResponse(saved);
    }

    // ─── Set Offline (all sessions of user) ──────────────────────────────────

    @Override
    public void setOffline(String userId) {
        presenceRepository.deleteByUserId(userId);
        log.info("User {} went OFFLINE — all sessions removed", userId);
    }

    // ─── Set Offline (specific session) ──────────────────────────────────────

    @Override
    public void setOfflineBySession(String sessionId) {
        presenceRepository.findBySessionId(sessionId).ifPresent(p -> {
            presenceRepository.deleteBySessionId(sessionId);
            log.info("Session {} disconnected for user {}", sessionId, p.getUserId());
        });
    }

    // ─── Update Status ────────────────────────────────────────────────────────

    @Override
    public PresenceResponse updateStatus(UpdateStatusRequest request) {
        List<UserPresence> sessions = presenceRepository.findByUserId(request.getUserId());
        if (sessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No active presence for user: " + request.getUserId());
        }

        sessions.forEach(p -> {
            p.setStatus(request.getStatus());
            if (request.getCustomMessage() != null) {
                p.setCustomMessage(request.getCustomMessage());
            }
        });
        presenceRepository.saveAll(sessions);

        // Return the most recently pinged session
        UserPresence latest = sessions.stream()
                .max((a, b) -> a.getLastPingAt().compareTo(b.getLastPingAt()))
                .orElse(sessions.get(0));

        log.info("User {} status updated to {}", request.getUserId(), request.getStatus());
        return toResponse(latest);
    }

    // ─── Get Presence ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<PresenceResponse> getPresence(String userId) {
        return presenceRepository.findTopByUserIdOrderByLastPingAtDesc(userId)
                .map(this::toResponse);
    }

    // ─── Bulk Presence ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PresenceResponse> getBulkPresence(BulkPresenceRequest request) {
        List<UserPresence> allSessions =
                presenceRepository.findByUserIdIn(request.getUserIds());

        // Deduplicate — keep the most recently pinged session per user
        Map<String, UserPresence> latestPerUser = allSessions.stream()
                .collect(Collectors.toMap(
                        UserPresence::getUserId,
                        p -> p,
                        (existing, incoming) ->
                                incoming.getLastPingAt().isAfter(existing.getLastPingAt())
                                        ? incoming : existing
                ));

        return request.getUserIds().stream()
                .map(uid -> {
                    if (latestPerUser.containsKey(uid)) {
                        return toResponse(latestPerUser.get(uid));
                    }
                    // User has no active session — return OFFLINE placeholder
                    return PresenceResponse.builder()
                            .userId(uid)
                            .status(UserStatus.INVISIBLE)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Online Users ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PresenceResponse> getOnlineUsers() {
        return presenceRepository.findOnlineUsers().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Online Count ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public int getOnlineCount() {
        return presenceRepository.countDistinctOnlineUsers();
    }

    // ─── Ping Session ─────────────────────────────────────────────────────────

    @Override
    public void pingSession(String sessionId) {
        UserPresence presence = presenceRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Session not found: " + sessionId));
        presence.setLastPingAt(LocalDateTime.now());
        presenceRepository.save(presence);
    }

    // ─── Stale Session Cleanup (@Scheduled every 60s) ─────────────────────────

    @Override
    @Scheduled(fixedDelayString = "${presence.stale.timeout-seconds:60}000")
    public void cleanStaleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(staleTimeoutSeconds);
        int removed = presenceRepository.deleteStaleSessionsBefore(cutoff);
        if (removed > 0) {
            log.info("Stale session cleanup: removed {} session(s) not pinged since {}", removed, cutoff);
        }
    }

    // ─── Is Online ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isOnline(String userId) {
        return presenceRepository.existsByUserId(userId);
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private PresenceResponse toResponse(UserPresence p) {
        return PresenceResponse.builder()
                .presenceId(p.getPresenceId())
                .userId(p.getUserId())
                .status(p.getStatus())
                .customMessage(p.getCustomMessage())
                .deviceType(p.getDeviceType())
                .connectedAt(p.getConnectedAt())
                .lastPingAt(p.getLastPingAt())
                .sessionId(p.getSessionId())
                .build();
    }
}

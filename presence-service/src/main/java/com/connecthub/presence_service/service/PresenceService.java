package com.ConnectHub.presence_service.service;

import com.ConnectHub.presence_service.dto.BulkPresenceRequest;
import com.ConnectHub.presence_service.dto.PresenceResponse;
import com.ConnectHub.presence_service.dto.SetOnlineRequest;
import com.ConnectHub.presence_service.dto.UpdateStatusRequest;
import java.util.List;
import java.util.Optional;

public interface PresenceService {

    /** Called when a WebSocket session is established */
    PresenceResponse setOnline(SetOnlineRequest request);

    /** Called when a WebSocket session is closed */
    void setOffline(String userId);

    /** Disconnect a specific session (not all sessions of user) */
    void setOfflineBySession(String sessionId);

    /** Update status + optional custom message for all sessions of user */
    PresenceResponse updateStatus(UpdateStatusRequest request);

    /** Get the latest presence record for a single user */
    Optional<PresenceResponse> getPresence(String userId);

    /** Get latest presence for each userId in the list (room member list) */
    List<PresenceResponse> getBulkPresence(BulkPresenceRequest request);

    /** Get all currently ONLINE users */
    List<PresenceResponse> getOnlineUsers();

    /** Count of distinct users with ONLINE status */
    int getOnlineCount();

    /** Update lastPingAt for a session (called by client heartbeat) */
    void pingSession(String sessionId);

    /** @Scheduled job: remove sessions with lastPingAt older than timeout */
    void cleanStaleSessions();

    /** Check if a specific user has at least one active session */
    boolean isOnline(String userId);
}

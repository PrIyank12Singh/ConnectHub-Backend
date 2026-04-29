package com.connecthub.websocket_handler.handler;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for the admin dashboard.
 * GET /ws-stats/connections → returns live active WebSocket connection count.
 * Called by the API Gateway admin panel (PDF Section 2.9).
 */
@RestController
@RequestMapping("/ws-stats")
public class AdminStatsController {

    private final ChatWebSocketHandler chatHandler;

    public AdminStatsController(ChatWebSocketHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @GetMapping("/connections")
    public ResponseEntity<Map<String, Integer>> getActiveConnections() {
        return ResponseEntity.ok(
                Map.of("activeConnections", chatHandler.getActiveConnectionCount()));
    }
}



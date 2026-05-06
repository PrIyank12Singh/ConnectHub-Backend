package com.connecthub.websocket_handler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.util.Map;

/**
 * Configures the STOMP message broker.
 *
 * From PDF Section 4.7:
 *   - Application prefix : /app
 *   - Broker prefix      : /topic, /queue
 *   - SockJS fallback    : /ws
 *   - Heartbeat          : configurable via TaskScheduler
 *   - JWT validation     : on every STOMP CONNECT frame
 *
 * Subscription topics (PDF 4.7):
 *   /topic/room/{roomId}  — room messages and events
 *   /topic/user/{userId}  — personal alerts and DM notifications
 *   /topic/presence       — online/offline presence broadcasts
 *
 * Inbound endpoints (PDF 4.7):
 *   /app/chat.send   — send a chat message
 *   /app/chat.typing — send a typing indicator
 *   /app/chat.read   — send a read receipt
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Value("${cors.allowed.origins:http://localhost:4200}")
    private String allowedOrigins;

    @Value("${stomp.heartbeat.send:10000}")
    private long heartbeatSend;

    @Value("${stomp.heartbeat.receive:10000}")
    private long heartbeatReceive;

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean(name = "broadcastExecutor")
    public TaskExecutor broadcastExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(1000);
        exec.setThreadNamePrefix("ws-broadcast-");
        exec.initialize();
        return exec;
    }

    // ─── STOMP Endpoint ───────────────────────────────────────────────────────

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins, "http://localhost:*")
                // SockJS fallback — activates automatically when WebSocket is blocked by proxy
                .withSockJS();
    }

    // ─── Message Broker ───────────────────────────────────────────────────────

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable built-in in-memory STOMP broker for /topic and /queue prefixes
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{heartbeatSend, heartbeatReceive})
                .setTaskScheduler(heartbeatScheduler());

        // All messages sent from client to /app/... are routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // User-specific queue prefix — /topic/user/{userId}
        registry.setUserDestinationPrefix("/topic/user");
    }

    // ─── JWT Interceptor on STOMP CONNECT ─────────────────────────────────────

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract JWT from Authorization header: "Bearer <token>"
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (jwtUtil.isValid(token)) {
                            String userId = jwtUtil.extractUserId(token);
                            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                            if (sessionAttributes == null) {
                                return message;
                            }
                            sessionAttributes.put("userId", userId);
                        } else {
                            throw new IllegalArgumentException("Invalid JWT on STOMP CONNECT");
                        }
                    }
                }
                return message;
            }
        });
    }

    // ─── Heartbeat Scheduler ──────────────────────────────────────────────────

    private TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}



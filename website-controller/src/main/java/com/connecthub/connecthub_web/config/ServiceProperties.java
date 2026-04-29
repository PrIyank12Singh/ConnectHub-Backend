package com.connecthub.connecthub_web.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class ServiceProperties {

    @Value("${service.auth.url}")
    private String authUrl;

    @Value("${service.room.url}")
    private String roomUrl;

    @Value("${service.message.url}")
    private String messageUrl;

    @Value("${service.media.url}")
    private String mediaUrl;

    @Value("${service.presence.url}")
    private String presenceUrl;

    @Value("${service.notification.url}")
    private String notificationUrl;

    @Value("${service.websocket.url}")
    private String websocketUrl;
}

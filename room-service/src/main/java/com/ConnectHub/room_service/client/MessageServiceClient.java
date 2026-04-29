package com.ConnectHub.room_service.client;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class MessageServiceClient {

    private final RestTemplate restTemplate;

    @Value("${message.service.url:http://localhost:8083}")
    private String messageServiceUrl;

    public MessageServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public long getUnreadCount(String roomId, LocalDateTime after) {
        String url = messageServiceUrl + "/messages/room/" + roomId + "/unread/count?after=" + after;
        Map response = restTemplate.getForObject(url, Map.class);
        if (response == null || response.get("unreadCount") == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(response.get("unreadCount")));
    }
}

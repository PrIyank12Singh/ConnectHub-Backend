package com.ConnectHub.room_service.dto;

import com.ConnectHub.room_service.model.RoomType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RoomResponse {

    private UUID roomId;
    private String name;
    private String description;
    private RoomType type;
    private UUID createdById;
    private String avatarUrl;
    private Boolean isPrivate;
    private Integer maxMembers;
    private Long memberCount;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

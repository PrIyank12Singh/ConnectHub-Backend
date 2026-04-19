package com.ConnectHub.room_service.dto;

import com.ConnectHub.room_service.model.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomRequest {

    @NotBlank
    @Size(min = 1, max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    private RoomType type;

    @NotNull
    private UUID createdById;

    private UUID recipientId;
    private String avatarUrl;

    private Boolean isPrivate;

    private Integer maxMembers;
}

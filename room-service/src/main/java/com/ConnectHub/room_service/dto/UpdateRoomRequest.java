package com.ConnectHub.room_service.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoomRequest {

    @Size(min = 1, max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private String avatarUrl;

    private Integer maxMembers;
}

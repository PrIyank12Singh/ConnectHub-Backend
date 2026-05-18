package com.connecthub.room_service.dto;

import com.connecthub.room_service.model.MemberRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddMemberRequest {

    @NotNull
    private UUID userId;

    private MemberRole role;
}



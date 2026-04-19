package com.ConnectHub.room_service.dto;

import com.ConnectHub.room_service.model.MemberRole;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MemberResponse {

    private Long memberId;
    private UUID roomId;
    private UUID userId;
    private MemberRole role;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
    private Boolean isMuted;
}

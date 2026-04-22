package com.ConnectHub.media_service.dto;

import com.ConnectHub.media_service.model.MediaType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MediaResponse {

    private UUID mediaId;
    private UUID uploaderId;
    private UUID roomId;
    private UUID messageId;
    private String filename;
    private String originalName;
    private String url;
    private String thumbnailUrl;
    private String mimeType;
    private Long sizeKb;
    private MediaType mediaType;
    private Integer width;
    private Integer height;
    private LocalDateTime uploadedAt;
}

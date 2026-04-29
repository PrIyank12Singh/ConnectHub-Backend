package com.connecthub.media_service.repository;

import com.connecthub.media_service.model.MediaFile;
import com.connecthub.media_service.model.MediaType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaFile, UUID> {

    Optional<MediaFile> findByMediaId(UUID mediaId);

    List<MediaFile> findByUploaderId(UUID uploaderId);

    List<MediaFile> findByRoomId(UUID roomId);

    List<MediaFile> findByMessageId(UUID messageId);

    List<MediaFile> findByRoomIdAndMediaType(UUID roomId, MediaType mediaType);

    long countByRoomId(UUID roomId);

    void deleteByMediaId(UUID mediaId);
}



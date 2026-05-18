package com.connecthub.media_service.service;

import com.connecthub.media_service.dto.MediaResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {

    MediaResponse uploadFile(MultipartFile file, UUID uploaderId, UUID roomId, UUID messageId);

    MediaResponse uploadImage(MultipartFile file, UUID uploaderId, UUID roomId, UUID messageId);

    MediaResponse getFileById(UUID mediaId);

    List<MediaResponse> getFilesByRoom(UUID roomId);

    List<MediaResponse> getImagesByRoom(UUID roomId);

    List<MediaResponse> getFilesByUploader(UUID uploaderId);

    List<MediaResponse> getFilesByMessage(UUID messageId);

    void deleteFile(UUID mediaId);

    long getFileCount(UUID roomId);

    long getTotalFileCount();

    List<MediaResponse> getAllFiles();
}
package com.connecthub.media_service.service;

import com.connecthub.media_service.dto.MediaResponse;
import com.connecthub.media_service.model.MediaFile;
import com.connecthub.media_service.model.MediaType;
import com.connecthub.media_service.repository.MediaRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;

    @Value("${media.upload.dir:uploads}")
    private String uploadDir;

    @Value("${media.base.url:http://localhost:8084}")
    private String baseUrl;

    @Value("${media.max.size.mb:25}")
    private long maxSizeMb;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_FILE_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip",
            "text/plain"
    );

    public MediaServiceImpl(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(uploadDir, "thumbnails"));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    @Override
    public MediaResponse uploadImage(MultipartFile file, UUID uploaderId, UUID roomId, UUID messageId) {
        validateFileSize(file);
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid image type. Allowed: JPEG, PNG, GIF, WebP");
        }
        return saveFile(file, uploaderId, roomId, messageId, MediaType.IMAGE, true);
    }

    @Override
    public MediaResponse uploadFile(MultipartFile file, UUID uploaderId, UUID roomId, UUID messageId) {
        validateFileSize(file);
        String contentType = file.getContentType();
        if (ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return saveFile(file, uploaderId, roomId, messageId, MediaType.IMAGE, true);
        }
        if (!ALLOWED_FILE_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed");
        }
        return saveFile(file, uploaderId, roomId, messageId, MediaType.FILE, false);
    }

    private MediaResponse saveFile(MultipartFile file, UUID uploaderId, UUID roomId,
                                    UUID messageId, MediaType mediaType, boolean generateThumbnail) {
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        Path filePath = Paths.get(uploadDir, filename);

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
        }

        String url = baseUrl + "/media/files/" + filename;
        String thumbnailUrl = null;

        if (generateThumbnail && mediaType == MediaType.IMAGE) {
            thumbnailUrl = generateThumbnail(filePath, filename);
        }

        MediaFile mediaFile = new MediaFile();
        mediaFile.setUploaderId(uploaderId);
        mediaFile.setRoomId(roomId);
        mediaFile.setMessageId(messageId);
        mediaFile.setFilename(filename);
        mediaFile.setOriginalName(file.getOriginalFilename());
        mediaFile.setUrl(url);
        mediaFile.setThumbnailUrl(thumbnailUrl);
        mediaFile.setMimeType(file.getContentType());
        mediaFile.setSizeKb(file.getSize() / 1024);
        mediaFile.setMediaType(mediaType);

        return toMediaResponse(mediaRepository.save(mediaFile));
    }

    private String generateThumbnail(Path originalPath, String filename) {
        try {
            String thumbFilename = "thumb_" + filename;
            Path thumbPath = Paths.get(uploadDir, "thumbnails", thumbFilename);
            Thumbnails.of(originalPath.toFile())
                    .size(300, 300)
                    .keepAspectRatio(true)
                    .toFile(thumbPath.toFile());
            return baseUrl + "/media/thumbnails/" + thumbFilename;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse getFileById(UUID mediaId) {
        return toMediaResponse(findMedia(mediaId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getFilesByRoom(UUID roomId) {
        return mediaRepository.findByRoomId(roomId)
                .stream().map(this::toMediaResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getImagesByRoom(UUID roomId) {
        return mediaRepository.findByRoomIdAndMediaType(roomId, MediaType.IMAGE)
                .stream().map(this::toMediaResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getFilesByUploader(UUID uploaderId) {
        return mediaRepository.findByUploaderId(uploaderId)
                .stream().map(this::toMediaResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getFilesByMessage(UUID messageId) {
        return mediaRepository.findByMessageId(messageId)
                .stream().map(this::toMediaResponse).toList();
    }

    @Override
    public void deleteFile(UUID mediaId) {
        MediaFile mediaFile = findMedia(mediaId);
        try {
            Files.deleteIfExists(Paths.get(uploadDir, mediaFile.getFilename()));
            if (mediaFile.getThumbnailUrl() != null) {
                Files.deleteIfExists(Paths.get(uploadDir, "thumbnails", "thumb_" + mediaFile.getFilename()));
            }
        } catch (IOException e) {
            // log but continue
        }
        mediaRepository.deleteByMediaId(mediaId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getFileCount(UUID roomId) {
        return mediaRepository.countByRoomId(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getAllFiles() {
        return mediaRepository.findAll()
                .stream().map(this::toMediaResponse).toList();
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > maxSizeMb * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File size exceeds maximum allowed size of " + maxSizeMb + "MB");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
    }

    private MediaFile findMedia(UUID mediaId) {
        return mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Media file not found: " + mediaId));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private MediaResponse toMediaResponse(MediaFile media) {
        return MediaResponse.builder()
                .mediaId(media.getMediaId())
                .uploaderId(media.getUploaderId())
                .roomId(media.getRoomId())
                .messageId(media.getMessageId())
                .filename(media.getFilename())
                .originalName(media.getOriginalName())
                .url(media.getUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .mimeType(media.getMimeType())
                .sizeKb(media.getSizeKb())
                .mediaType(media.getMediaType())
                .width(media.getWidth())
                .height(media.getHeight())
                .uploadedAt(media.getUploadedAt())
                .build();
    }
}



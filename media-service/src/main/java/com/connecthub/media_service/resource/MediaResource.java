package com.ConnectHub.media_service.resource;

import com.ConnectHub.media_service.dto.MediaResponse;
import com.ConnectHub.media_service.service.MediaService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/media")
public class MediaResource {

    private final MediaService mediaService;

    @Value("${media.upload.dir:uploads}")
    private String uploadDir;

    public MediaResource(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    // ─── Upload File ──────────────────────────────────────────────────────────
    @PostMapping("/upload")
    public ResponseEntity<MediaResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderId") UUID uploaderId,
            @RequestParam(value = "roomId", required = false) UUID roomId,
            @RequestParam(value = "messageId", required = false) UUID messageId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.uploadFile(file, uploaderId, roomId, messageId));
    }

    // ─── Upload Image (with thumbnail) ───────────────────────────────────────
    @PostMapping("/upload/image")
    public ResponseEntity<MediaResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderId") UUID uploaderId,
            @RequestParam(value = "roomId", required = false) UUID roomId,
            @RequestParam(value = "messageId", required = false) UUID messageId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.uploadImage(file, uploaderId, roomId, messageId));
    }

    // ─── Serve File ───────────────────────────────────────────────────────────
    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        return serveFileFromPath(Paths.get(uploadDir, filename));
    }

    // ─── Serve Thumbnail ──────────────────────────────────────────────────────
    @GetMapping("/thumbnails/{filename}")
    public ResponseEntity<Resource> serveThumbnail(@PathVariable String filename) {
        return serveFileFromPath(Paths.get(uploadDir, "thumbnails", filename));
    }

    // ─── Get File by ID ───────────────────────────────────────────────────────
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaResponse> getFileById(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(mediaService.getFileById(mediaId));
    }

    // ─── Get Files by Room ────────────────────────────────────────────────────
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<MediaResponse>> getFilesByRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(mediaService.getFilesByRoom(roomId));
    }

    // ─── Get Images by Room (media gallery) ───────────────────────────────────
    @GetMapping("/room/{roomId}/images")
    public ResponseEntity<List<MediaResponse>> getImagesByRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(mediaService.getImagesByRoom(roomId));
    }

    // ─── Get Files by Uploader ────────────────────────────────────────────────
    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<List<MediaResponse>> getFilesByUploader(@PathVariable UUID uploaderId) {
        return ResponseEntity.ok(mediaService.getFilesByUploader(uploaderId));
    }

    // ─── Get Files by Message ─────────────────────────────────────────────────
    @GetMapping("/message/{messageId}")
    public ResponseEntity<List<MediaResponse>> getFilesByMessage(@PathVariable UUID messageId) {
        return ResponseEntity.ok(mediaService.getFilesByMessage(messageId));
    }

    // ─── Get File Count ───────────────────────────────────────────────────────
    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Map<String, Object>> getFileCount(@PathVariable UUID roomId) {
        return ResponseEntity.ok(Map.of("roomId", roomId, "fileCount", mediaService.getFileCount(roomId)));
    }

    // ─── Delete File ──────────────────────────────────────────────────────────
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable UUID mediaId) {
        mediaService.deleteFile(mediaId);
        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }

    // ─── Get All Files ────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<MediaResponse>> getAllFiles() {
        return ResponseEntity.ok(mediaService.getAllFiles());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────
    private ResponseEntity<Resource> serveFileFromPath(Path filePath) {
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }
}

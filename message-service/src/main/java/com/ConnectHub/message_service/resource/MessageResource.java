package com.ConnectHub.message_service.resource;

import com.ConnectHub.message_service.dto.EditMessageRequest;
import com.ConnectHub.message_service.dto.MessageResponse;
import com.ConnectHub.message_service.dto.SendMessageRequest;
import com.ConnectHub.message_service.model.DeliveryStatus;
import com.ConnectHub.message_service.service.MessageService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
public class MessageResource {

    private final MessageService messageService;

    public MessageResource(MessageService messageService) {
        this.messageService = messageService;
    }

    // ─── Send Message ─────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(request));
    }

    // ─── Get Message ──────────────────────────────────────────────────────────

    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse> getMessageById(@PathVariable UUID messageId) {
        return ResponseEntity.ok(messageService.getMessageById(messageId));
    }

    // ─── Get Messages by Room (paginated) ────────────────────────────────────

    @GetMapping("/room/{roomId}")
    public ResponseEntity<Page<MessageResponse>> getMessagesByRoom(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(messageService.getMessagesByRoom(roomId, page, size));
    }

    // ─── Get Messages Before (infinite scroll) ───────────────────────────────

    @GetMapping("/room/{roomId}/before")
    public ResponseEntity<List<MessageResponse>> getMessagesBefore(
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(messageService.getMessagesBefore(roomId, before, limit));
    }

    // ─── Edit Message ─────────────────────────────────────────────────────────

    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(messageService.editMessage(messageId, request));
    }

    // ─── Delete Message (soft delete) ────────────────────────────────────────

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Map<String, String>> deleteMessage(@PathVariable UUID messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.ok(Map.of("message", "Message deleted successfully"));
    }

    // ─── Search Messages ──────────────────────────────────────────────────────

    @GetMapping("/room/{roomId}/search")
    public ResponseEntity<List<MessageResponse>> searchMessages(
            @PathVariable UUID roomId,
            @RequestParam String keyword) {
        return ResponseEntity.ok(messageService.searchMessages(roomId, keyword));
    }

    // ─── Update Delivery Status ───────────────────────────────────────────────

    @PutMapping("/{messageId}/status")
    public ResponseEntity<Map<String, String>> updateDeliveryStatus(
            @PathVariable UUID messageId,
            @RequestParam DeliveryStatus status) {
        messageService.updateDeliveryStatus(messageId, status);
        return ResponseEntity.ok(Map.of("message", "Status updated to " + status));
    }

    // ─── Get Message Count ────────────────────────────────────────────────────

    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Map<String, Object>> getMessageCount(@PathVariable UUID roomId) {
        long count = messageService.getMessageCount(roomId);
        return ResponseEntity.ok(Map.of("roomId", roomId, "messageCount", count));
    }

    // ─── Get Unread Messages ──────────────────────────────────────────────────

    @GetMapping("/room/{roomId}/unread")
    public ResponseEntity<List<MessageResponse>> getUnreadMessages(
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after) {
        return ResponseEntity.ok(messageService.getUnreadMessages(roomId, after));
    }
}

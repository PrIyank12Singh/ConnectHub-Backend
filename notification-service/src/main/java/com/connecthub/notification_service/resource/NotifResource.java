package com.ConnectHub.notification_service.resource;

import com.ConnectHub.notification_service.dto.BulkNotificationRequest;
import com.ConnectHub.notification_service.dto.EmailNotificationRequest;
import com.ConnectHub.notification_service.dto.NotificationResponse;
import com.ConnectHub.notification_service.dto.SendNotificationRequest;
import com.ConnectHub.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotifResource {

    private final NotificationService notificationService;

    public NotifResource(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ─── Send Single Notification ─────────────────────────────────────────────
    /**
     * POST /notifications
     * Called by WebSocket handler when a message arrives for an offline user,
     * or by any service that needs to dispatch an alert.
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> send(
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    // ─── Send Bulk Notifications ──────────────────────────────────────────────
    /**
     * POST /notifications/bulk
     * Used by admin to broadcast platform-wide or room-wide alerts.
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<NotificationResponse>> sendBulk(
            @Valid @RequestBody BulkNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendBulk(request));
    }

    // ─── Send Email ───────────────────────────────────────────────────────────
    /**
     * POST /notifications/email
     * Triggered when user has been offline for 30+ minutes and receives a DM.
     */
    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendEmail(
            @Valid @RequestBody EmailNotificationRequest request) {
        notificationService.sendEmail(request);
        return ResponseEntity.ok(Map.of("message", "Email dispatched to " + request.getToEmail()));
    }

    // ─── Send FCM Push ────────────────────────────────────────────────────────
    /**
     * POST /notifications/push/{recipientId}
     */
    @PostMapping("/push/{recipientId}")
    public ResponseEntity<Map<String, String>> sendPush(
            @PathVariable String recipientId,
            @RequestBody Map<String, String> payload) {
        String title = payload.getOrDefault("title", "ConnectHub");
        String body  = payload.getOrDefault("body", "You have a new notification");
        notificationService.sendPushNotification(recipientId, title, body);
        return ResponseEntity.ok(Map.of("message", "Push notification queued"));
    }

    // ─── Get All By Recipient ─────────────────────────────────────────────────
    /**
     * GET /notifications/recipient/{recipientId}
     */
    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<NotificationResponse>> getByRecipient(
            @PathVariable String recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    // ─── Unread Count ─────────────────────────────────────────────────────────
    /**
     * GET /notifications/recipient/{recipientId}/unread-count
     */
    @GetMapping("/recipient/{recipientId}/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(
            @PathVariable String recipientId) {
        return ResponseEntity.ok(
                Map.of("unreadCount", notificationService.getUnreadCount(recipientId)));
    }

    // ─── Mark Single As Read ──────────────────────────────────────────────────
    /**
     * PUT /notifications/{notificationId}/read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    // ─── Mark All Read ────────────────────────────────────────────────────────
    /**
     * PUT /notifications/recipient/{recipientId}/read-all
     */
    @PutMapping("/recipient/{recipientId}/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(
            @PathVariable String recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // ─── Delete Notification ──────────────────────────────────────────────────
    /**
     * DELETE /notifications/{notificationId}
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    // ─── Get All (admin) ──────────────────────────────────────────────────────
    /**
     * GET /notifications/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<NotificationResponse>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }
}

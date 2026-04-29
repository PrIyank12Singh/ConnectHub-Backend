package com.ConnectHub.notification_service.service;

import com.ConnectHub.notification_service.client.PresenceClient;
import com.ConnectHub.notification_service.dto.BulkNotificationRequest;
import com.ConnectHub.notification_service.dto.EmailNotificationRequest;
import com.ConnectHub.notification_service.dto.NotificationResponse;
import com.ConnectHub.notification_service.dto.SendNotificationRequest;
import com.ConnectHub.notification_service.model.Notification;
import com.ConnectHub.notification_service.model.NotificationType;
import com.ConnectHub.notification_service.repository.NotificationRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final PresenceClient presenceClient;  // ← NEW: presence-service client

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:noreply@connecthub.com}")
    private String emailFrom;

    @Value("${notification.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Value("${notification.email.offline-threshold-minutes:30}")
    private int offlineThresholdMinutes;  // ← NEW: 30 min threshold from properties

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            JavaMailSender mailSender,
            PresenceClient presenceClient) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
        this.presenceClient = presenceClient;
    }

    // ─── Send Single Notification ─────────────────────────────────────────────

    @Override
    public NotificationResponse send(SendNotificationRequest request) {
        Notification notification = new Notification();
        notification.setRecipientId(request.getRecipientId());
        notification.setActorId(request.getActorId());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRoomId(request.getRoomId());
        notification.setMessageId(request.getMessageId());

        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved for user {} — type: {}", request.getRecipientId(), request.getType());

        // ── Connection: Check presence before sending email for DM/MENTION ──
        if (request.getType() == NotificationType.NEW_MESSAGE
                || request.getType() == NotificationType.MENTION) {
            triggerEmailIfOfflineLongEnough(request.getRecipientId(), request.getTitle(), request.getMessage());
        }

        // ── Push notification for offline users (FCM stub) ──
        if (!presenceClient.isOnline(request.getRecipientId())) {
            sendPushNotification(request.getRecipientId(), request.getTitle(), request.getMessage());
        }

        return toResponse(saved);
    }

    // ─── Send Bulk Notifications ──────────────────────────────────────────────

    @Override
    public List<NotificationResponse> sendBulk(BulkNotificationRequest request) {
        List<Notification> notifications = request.getRecipientIds().stream()
                .map(recipientId -> {
                    Notification n = new Notification();
                    n.setRecipientId(recipientId);
                    n.setActorId(request.getActorId());
                    n.setType(request.getType());
                    n.setTitle(request.getTitle());
                    n.setMessage(request.getMessage());
                    n.setRoomId(request.getRoomId());
                    n.setMessageId(request.getMessageId());
                    return n;
                })
                .collect(Collectors.toList());

        List<Notification> saved = notificationRepository.saveAll(notifications);
        log.info("Bulk notification saved for {} users — type: {}",
                request.getRecipientIds().size(), request.getType());

        // ── Push to offline users only ──
        request.getRecipientIds().forEach(uid -> {
            if (!presenceClient.isOnline(uid)) {
                sendPushNotification(uid, request.getTitle(), request.getMessage());
            }
        });

        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Send Push Notification (FCM stub) ────────────────────────────────────

    @Override
    public void sendPushNotification(String recipientId, String title, String body) {
        if (!fcmEnabled) {
            log.info("[FCM STUB] Push to user {}: '{}' — '{}'", recipientId, title, body);
            return;
        }
        // Production: integrate Firebase Admin SDK here
        log.info("[FCM] Push notification queued for user {}", recipientId);
    }

    // ─── Mark As Read ─────────────────────────────────────────────────────────

    @Override
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notification not found: " + notificationId));
        notificationRepository.markAsReadById(notification.getNotificationId());
    }

    // ─── Mark All Read ────────────────────────────────────────────────────────

    @Override
    public void markAllRead(String recipientId) {
        notificationRepository.markAllReadByRecipient(recipientId);
    }

    // ─── Get By Recipient ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByRecipient(String recipientId) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Get Unread Count ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    // ─── Delete Notification ──────────────────────────────────────────────────

    @Override
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Notification not found: " + notificationId);
        }
        notificationRepository.deleteByNotificationId(notificationId);
    }

    // ─── Send Email ───────────────────────────────────────────────────────────

    @Override
    public void sendEmail(EmailNotificationRequest request) {
        if (!emailEnabled) {
            log.info("[EMAIL STUB] To: {} | Subject: {} | Body: {}",
                    request.getToEmail(), request.getSubject(), request.getBody());
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(emailFrom);
            mail.setTo(request.getToEmail());
            mail.setSubject(request.getSubject());
            mail.setText(request.getBody());
            mailSender.send(mail);
            log.info("[EMAIL] Sent to {}", request.getToEmail());
        } catch (Exception ex) {
            log.error("[EMAIL] Failed to send to {}: {}", request.getToEmail(), ex.getMessage());
        }
    }

    // ─── Get All (admin) ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Private: Email threshold check via presence-service ─────────────────

    /**
     * Only sends an email if:
     *   1. Email is enabled in properties
     *   2. The recipient is NOT online (presence-service check)
     *   3. The recipient has been offline for >= offlineThresholdMinutes (default 30)
     */
    private void triggerEmailIfOfflineLongEnough(String recipientId, String title, String message) {
        if (!emailEnabled) return;

        boolean offlineLongEnough = presenceClient.hasBeenOfflineFor(recipientId, offlineThresholdMinutes);

        if (offlineLongEnough) {
            log.info("[EMAIL] User {} has been offline >= {} min — sending missed message email",
                    recipientId, offlineThresholdMinutes);

            EmailNotificationRequest emailReq = new EmailNotificationRequest();
            emailReq.setToEmail(recipientId); // In real usage resolve email from auth-service
            emailReq.setSubject("You missed a message on ConnectHub: " + title);
            emailReq.setBody("Hi! You received a new message while you were offline.\n\n"
                    + message + "\n\nLog in to ConnectHub to read it.");
            sendEmail(emailReq);
        } else {
            log.debug("[EMAIL] Skipping email for user {} — online or offline < {} min",
                    recipientId, offlineThresholdMinutes);
        }
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .recipientId(n.getRecipientId())
                .actorId(n.getActorId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .roomId(n.getRoomId())
                .messageId(n.getMessageId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

package com.connecthub.notification_service.service;

import com.connecthub.notification_service.dto.BulkNotificationRequest;
import com.connecthub.notification_service.dto.EmailNotificationRequest;
import com.connecthub.notification_service.dto.NotificationResponse;
import com.connecthub.notification_service.dto.SendNotificationRequest;
import java.util.List;

public interface NotificationService {

    /** Send a single in-app notification */
    NotificationResponse send(SendNotificationRequest request);

    /** Send the same notification to multiple recipients */
    List<NotificationResponse> sendBulk(BulkNotificationRequest request);

    /** Send FCM push notification (stub — logs if FCM disabled) */
    void sendPushNotification(String recipientId, String title, String body);

    /** Mark a single notification as read */
    void markAsRead(Long notificationId);

    /** Mark all notifications as read for a recipient */
    void markAllRead(String recipientId);

    /** Get all notifications for a recipient (newest first) */
    List<NotificationResponse> getByRecipient(String recipientId);

    /** Get unread count (for badge) */
    int getUnreadCount(String recipientId);

    /** Delete a single notification */
    void deleteNotification(Long notificationId);

    /** Send email notification via JavaMailSender */
    void sendEmail(EmailNotificationRequest request);

    /** Get all notifications (admin use) */
    List<NotificationResponse> getAll();
}



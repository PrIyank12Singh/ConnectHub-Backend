package com.connecthub.notification_service.repository;

import com.connecthub.notification_service.model.Notification;
import com.connecthub.notification_service.model.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** All notifications for a recipient, newest first */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    /** Unread/read notifications for a recipient */
    List<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(
            String recipientId, Boolean isRead);

    /** Count unread for badge display */
    int countByRecipientIdAndIsRead(String recipientId, Boolean isRead);

    /** Filter by type */
    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);

    /** All notifications for a specific room */
    List<Notification> findByRoomIdOrderByCreatedAtDesc(String roomId);

    /** Mark one notification read */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :id")
    void markAsReadById(@Param("id") Long notificationId);

    /** Mark all notifications read for a recipient */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId")
    void markAllReadByRecipient(@Param("recipientId") String recipientId);

    /** Delete a specific notification */
    void deleteByNotificationId(Long notificationId);

    /** Delete all read notifications for a recipient */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = true")
    void deleteReadByRecipient(@Param("recipientId") String recipientId);

    /** Delete all notifications for a recipient (on account delete) */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId")
    void deleteAllByRecipient(@Param("recipientId") String recipientId);
}



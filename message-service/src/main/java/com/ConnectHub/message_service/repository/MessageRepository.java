package com.ConnectHub.message_service.repository;

import com.ConnectHub.message_service.model.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findByMessageId(UUID messageId);

    Page<Message> findByRoomIdAndIsDeletedFalseOrderBySentAtDesc(UUID roomId, Pageable pageable);

    List<Message> findByRoomIdAndSentAtBeforeAndIsDeletedFalseOrderBySentAtDesc(
            UUID roomId, LocalDateTime before, Pageable pageable);

    List<Message> findBySenderId(UUID senderId);

    long countByRoomIdAndIsDeletedFalse(UUID roomId);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.isDeleted = false AND " +
           "(LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Message> searchInRoom(@Param("roomId") UUID roomId, @Param("keyword") String keyword);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.sentAt > :after AND m.isDeleted = false")
    List<Message> findUnreadMessages(@Param("roomId") UUID roomId, @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.roomId = :roomId AND m.sentAt > :after AND m.isDeleted = false")
    long countUnreadMessages(@Param("roomId") UUID roomId, @Param("after") LocalDateTime after);

    void deleteByMessageId(UUID messageId);
}

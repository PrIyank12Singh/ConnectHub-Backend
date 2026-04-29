package com.connecthub.room_service.repository;

import com.connecthub.room_service.model.Room;
import com.connecthub.room_service.model.RoomType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByRoomId(UUID roomId);

    List<Room> findByCreatedById(UUID createdById);

    List<Room> findByType(RoomType type);

    @Query("SELECT r FROM Room r JOIN RoomMember rm ON r.roomId = rm.roomId WHERE rm.userId = :userId ORDER BY r.lastMessageAt DESC NULLS LAST")
    List<Room> findRoomsByUserId(@Param("userId") UUID userId);

    void deleteByRoomId(UUID roomId);
}



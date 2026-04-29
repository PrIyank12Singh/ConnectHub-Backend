package com.connecthub.room_service.repository;

import com.connecthub.room_service.model.RoomMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    List<RoomMember> findByRoomId(UUID roomId);

    List<RoomMember> findByUserId(UUID userId);

    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    long countByRoomId(UUID roomId);

    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);

    void deleteByRoomId(UUID roomId);

    @Query("SELECT rm FROM RoomMember rm WHERE rm.roomId = :roomId AND rm.lastReadAt IS NOT NULL")
    List<RoomMember> findMembersWithReadAt(@Param("roomId") UUID roomId);
}



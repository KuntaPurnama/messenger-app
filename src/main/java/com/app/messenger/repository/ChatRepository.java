package com.app.messenger.repository;

import com.app.messenger.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    boolean existsById(long id);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM chats c JOIN chat_participants cp ON c.id = cp.id " +
            "WHERE c.is_group = false AND " +
            "c.created_by = :createdBy AND " +
            "cp.phone_number = :participantPhoneNumber)", nativeQuery = true)
    boolean isPrivateChatExists(@Param("createdBy") String createdBy, @Param("participantPhoneNumber") String participantPhoneNumber);
}

package com.app.messenger.repository;

import com.app.messenger.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    boolean existsById(long id);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM chats c JOIN chat_participants cp ON c.id = cp.chat_id " +
            "WHERE c.is_group = false AND " +
            "cp.phone_number IN (:participantPhoneNumbers))", nativeQuery = true)
    boolean isPrivateChatExists(@Param("participantPhoneNumbers") List<String> participantPhoneNumber);

    @Query("SELECT DISTINCT c FROM Chat c JOIN FETCH c.chatParticipants WHERE c.id IN (:ids)")
    List<Chat> getChatWithParticipantWithIds(@Param("ids") List<Long> ids);
}

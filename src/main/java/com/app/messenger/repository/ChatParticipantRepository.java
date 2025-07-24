package com.app.messenger.repository;

import com.app.messenger.model.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    @Query("SELECT DISTINCT cp FROM ChatParticipant cp LEFT JOIN FETCH cp.chat WHERE cp.phoneNumber = :phoneNumber")
    List<ChatParticipant> findAllChatWithParticipantByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    boolean existsChatParticipantByChatIdAndPhoneNumber(long chatId, String phoneNumber);
    List<ChatParticipant> findAllChatParticipantByChatId(long chatId);
    Optional<ChatParticipant> findChatParticipantByChatIdAndPhoneNumber(long chatId, String phoneNumber);
}

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

    @Query(value = "SELECT cp.phone_number FROM chat_participants cp WHERE cp.chat_id = :chatId", nativeQuery = true)
    List<String> findAllChatParticipantNumberOnlyByChatId(@Param("chatId") long chatId);

    @Query(value = "SELECT cp.chat_id FROM chat_participants cp WHERE cp.phone_number = :phoneNumber", nativeQuery = true)
    List<Long> findAllChatParticipantChatIdNumberByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}

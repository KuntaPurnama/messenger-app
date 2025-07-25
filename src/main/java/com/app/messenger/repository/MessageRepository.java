package com.app.messenger.repository;

import com.app.messenger.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface MessageRepository extends JpaRepository<Message,Long> {
    boolean existsById(Long id);

    Page<Message> findAllByChatId(@Param("chatId") long chatId, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Message m JOIN m.messageActivities ma " +
            "WHERE m.chatId = :chatId AND " +
            "ma.userPhoneNumber = :phoneNumber AND " +
            "ma.status = 'SENT'")
    Page<Message> getListUndeliveredMessage(@Param("chatId") long chatId, @Param("phoneNumber")  String phoneNumber, Pageable pageable);

    @Query(value = "SELECT COUNT(m.id) FROM messages m JOIN message_activities ma ON m.id = ma.message_id WHERE ma.status = 'SENT' " +
            "AND ma.user_phone_number = :phoneNumber " +
            "AND m.chat_id = :chatId", nativeQuery = true)
    int countListUndeliveredMessage(@Param("chatId") long chatId, @Param("phoneNumber")  String phoneNumber);

    @Query(value = "SELECT DISTINCT m.id FROM message_activities ma JOIN messages m ON m.id = ma.message_id WHERE m.id = ma.message_id AND " +
            "m.chat_id = :chatId AND " +
            "ma.user_phone_number = :phoneNumber AND " +
            "ma.status != 'READ'", nativeQuery = true)
    List<Long> getListUnreadMessageId(@Param("chatId") long chatId, @Param("phoneNumber") String phoneNumber);
}

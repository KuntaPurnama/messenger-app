package com.app.messenger.repository;

import com.app.messenger.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface MessageRepository extends JpaRepository<Message,Long> {
    boolean existsById(Long id);

    @Query("SELECT DISTINCT m FROM Message m " +
            "LEFT JOIN FETCH m.messageActivities " +
            "LEFT JOIN FETCH m.messageAttachments " +
            "WHERE m.chatId = :chatId")
    List<Message> getListMessageByChatId(@Param("chatId") long chatId);

    @Query("SELECT DISTINCT m FROM Message m JOIN FETCH m.messageActivities ma JOIN FETCH m.messageAttachments " +
            "WHERE m.chatId = :chatId AND " +
            "ma.userPhoneNumber = :phoneNumber AND " +
            "ma.status = 'SENT'")
    List<Message> getListUndeliveredMessage(@Param("chatId") long chatId, @Param("phoneNumber")  String phoneNumber);

    @Query(value = "SELECT COUNT(m.id) FROM messages m JOIN message_activites ma ON m.id = ma.messasge_id WHERE ma.status = 'SENT'", nativeQuery = true)
    int countListUndeliveredMessage(@Param("chatId") long chatId, @Param("phoneNumber")  String phoneNumber);
}

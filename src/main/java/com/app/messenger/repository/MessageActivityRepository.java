package com.app.messenger.repository;

import com.app.messenger.model.MessageActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageActivityRepository extends JpaRepository<MessageActivity, Long> {
    Optional<MessageActivity> findByMessageId(long id);

    @Query(value = "UPDATE message_activities ma SET ma.status= :status WHERE ma.message_id IN (:messageIds) AND ma.phone_number = :phoneNumber AND ma.status != :status",
            nativeQuery = true)
    void updateMessageActivityStatus(@Param("messageIds") List<Long> messageIds, @Param("phoneNumber") String phoneNumber, @Param("status") String status);
}

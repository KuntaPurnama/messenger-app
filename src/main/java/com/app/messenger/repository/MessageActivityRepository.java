package com.app.messenger.repository;

import com.app.messenger.model.MessageActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageActivityRepository extends JpaRepository<MessageActivity, Long> {
    MessageActivity findByMessageIdAndUserPhoneNumber(long messageId, String userPhoneNumber);

    @Modifying
    @Query(value = "UPDATE message_activities SET status= :status WHERE message_id IN (:messageIds) AND user_phone_number = :phoneNumber AND status != :status",
            nativeQuery = true)
    void updateMessageActivityStatus(@Param("messageIds") List<Long> messageIds, @Param("phoneNumber") String phoneNumber, @Param("status") String status);

    @Query("SELECT ma FROM MessageActivity ma WHERE ma.messageId IN (:messageIds)")
    List<MessageActivity> findByMessageIds(@Param("messageIds") List<Long> messageIds);
}

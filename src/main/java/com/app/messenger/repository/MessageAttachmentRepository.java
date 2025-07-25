package com.app.messenger.repository;

import com.app.messenger.model.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    @Query("SELECT ma FROM MessageAttachment ma WHERE ma.message.id IN (:messageIds)")
    List<MessageAttachment> findByMessageIds(@Param("messageIds") List<Long> messageIds);
}

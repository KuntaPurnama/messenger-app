package com.app.messenger.repository;

import com.app.messenger.model.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
}

package com.app.messenger.repository;

import com.app.messenger.model.MessageActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageActivityRepository extends JpaRepository<MessageActivity, Long> {
    Optional<MessageActivity> findByMessageId(long id);
}

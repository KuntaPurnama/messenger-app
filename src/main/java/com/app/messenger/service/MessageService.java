package com.app.messenger.service;

import com.app.messenger.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {
    void sendMessage(MessageRequestDTO dto, String phoneNumber);
    void deleteMessage(long messageId, String phoneNumber);
    void editMessage(UpdateMessageRequestDTO dto, String phoneNumber);
    void updateMessageActivity(MessageActivityDTO dto);
    FileUploadDTO uploadFile(List<MultipartFile> file);
    int countUndeliveredMessage(long chatId, String phoneNumber);
    Page<MessageDTO> getUndeliveredMessage(long chatId, Pageable pageable, String phoneNumber);
    void updateMessageActivityStatusToRead(long chatId, String phoneNumber);
    void notifyUserTypingStatus(UserTypingStatusDTO dto, String phoneNumber);
    void updateMessageReaction(MessageReactionRequestDTO dto, String phoneNumber);
}

package com.app.messenger.service;

import com.app.messenger.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {
    void sendMessage(MessageRequestDTO dto, String phoneNumber);
    void deleteMessage(MessageDTO dto);
    void editMessage(MessageDTO dto);
    void updateMessageActivity(MessageActivityDTO dto);
    FileUploadDTO uploadFile(List<MultipartFile> file);
    int countUndeliveredMessage(long chatId, String phoneNumber);
    List<MessageDTO> getUndeliveredMessage(long chatId, String phoneNumber);
    void updateMessageActivityStatusToRead(long chatId, String phoneNumber);
    void notifyUserTypingStatus(UserTypingStatusDTO dto, String phoneNumber);
}

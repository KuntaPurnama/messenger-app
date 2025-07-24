package com.app.messenger.service;

import com.app.messenger.dto.FileUploadDTO;
import com.app.messenger.dto.MessageActivityDTO;
import com.app.messenger.dto.MessageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {
    void sendMessage(MessageDTO dto);
    void deleteMessage(MessageDTO dto);
    void editMessage(MessageDTO dto);
    void updateMessageActivity(MessageActivityDTO dto);
    FileUploadDTO uploadFile(List<MultipartFile> file);
    int countUndeliveredMessage(long chatId, String phoneNumber);
    List<MessageDTO> getUndeliveredMessage(long chatId, String phoneNumber);
}

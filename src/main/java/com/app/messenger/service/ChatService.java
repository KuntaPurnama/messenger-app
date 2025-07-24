package com.app.messenger.service;

import com.app.messenger.dto.ChatDTO;
import com.app.messenger.dto.MessageActivityDTO;
import com.app.messenger.dto.MessageDTO;
import com.app.messenger.model.Chat;
import com.app.messenger.model.Message;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatService {
    void createChat(ChatDTO chat);
    void deleteChat(long chatId, String phoneNumber);
    List<MessageDTO> getListMessage(long chatId, String phoneNumber);
    List<ChatDTO> getListChat(String phoneNumber);
    List<String> getChatParticipants(long chatId, String phoneNumber);
}

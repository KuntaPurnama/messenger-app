package com.app.messenger.service;

import com.app.messenger.dto.ChatDTO;
import com.app.messenger.dto.ChatRequestDTO;
import com.app.messenger.dto.MessageDTO;

import java.util.List;

public interface ChatService {
    void createChat(ChatRequestDTO chat, String phoneNumber);
    void deleteChat(long chatId, String phoneNumber);
    List<MessageDTO> getListMessage(long chatId, String phoneNumber);
    List<ChatDTO> getListChat(String phoneNumber);
    List<String> getChatParticipants(long chatId, String phoneNumber);
}

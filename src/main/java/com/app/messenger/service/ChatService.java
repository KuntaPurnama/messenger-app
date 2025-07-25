package com.app.messenger.service;

import com.app.messenger.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatService {
    void createChat(ChatRequestDTO chat, String phoneNumber);
    void deleteChat(long chatId, String phoneNumber);
    void editChatName(EditChatDTO dto, String phoneNumber);
    void leaveChat(long chatId, String phoneNumber);
    void addParticipant(AddParticipantDTO dto);
    Page<MessageDTO> getListMessage(long chatId, Pageable pageable, String phoneNumber);
    Page<ChatDTO> getListChat(String phoneNumber, Pageable pageable);
    Page<UserDetailResponseDTO> getChatParticipants(long chatId, Pageable pageable, String phoneNumber);
}

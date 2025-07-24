package com.app.messenger.controller;

import com.app.messenger.dto.MessageDTO;
import com.app.messenger.dto.MessageRequestDTO;
import com.app.messenger.dto.UserTypingStatusDTO;
import com.app.messenger.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MessageSocketController {
    private final MessageService messageService;

    @MessageMapping("/chat/message")
    public void handleChat(@Payload MessageRequestDTO messageDTO, Principal principal) {
        messageService.sendMessage(messageDTO, principal.getName());
    }

    @MessageMapping("/chat/message/typing")
    public void handleUserTypingStatus(@Payload UserTypingStatusDTO userTypingStatusDTO, Principal principal) {
        messageService.notifyUserTypingStatus(userTypingStatusDTO, principal.getName());
    }
}

package com.app.messenger.controller;

import com.app.messenger.dto.MessageDTO;
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

    @MessageMapping("/chat/message/{chatId}")
    public void handleChat(@Payload MessageDTO messageDTO, Principal principal) {
        messageDTO.setSenderPhoneNumber(principal.getName());
        messageService.sendMessage(messageDTO);
    }
}

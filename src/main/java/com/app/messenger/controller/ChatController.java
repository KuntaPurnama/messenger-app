package com.app.messenger.controller;

import com.app.messenger.dto.ChatDTO;
import com.app.messenger.dto.ChatRequestDTO;
import com.app.messenger.dto.MessageDTO;
import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping( "/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> createChat(@RequestBody ChatRequestDTO chatDTO, Principal principal) {
        chatService.createChat(chatDTO, principal.getName());
        return ResponseDTO.ok();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<List<ChatDTO>> getListChat(Principal principal) {
        return ResponseDTO.ok(chatService.getListChat(principal.getName()));
    }

    @DeleteMapping(value = "/{chatId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> deleteChat(@PathVariable long chatId, Principal principal) {
        chatService.deleteChat(chatId, principal.getName());
        return ResponseDTO.ok();
    }

    //Can be enhanced
    @GetMapping(value = "/participant/{chatId}")
    public ResponseDTO<List<String>> getChatParticipant(@PathVariable Long chatId, Principal principal) {
        return ResponseDTO.ok(chatService.getChatParticipants(chatId, principal.getName()));
    }

    @GetMapping(value = "/message/{chatId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<List<MessageDTO>> getListOfMessage(@PathVariable Long chatId, Principal principal) {
        return ResponseDTO.ok(chatService.getListMessage(chatId, principal.getName()));
    }
}

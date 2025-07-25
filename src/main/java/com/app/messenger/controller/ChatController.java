package com.app.messenger.controller;

import com.app.messenger.dto.*;
import com.app.messenger.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping( "/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> createChat(@Valid @RequestBody ChatRequestDTO chatDTO, Principal principal) {
        chatService.createChat(chatDTO, principal.getName());
        return ResponseDTO.ok();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Page<ChatDTO>> getListChat(@ParameterObject
                                                  @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
                                                  @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                  Principal principal) {
        return ResponseDTO.ok(chatService.getListChat(principal.getName(), pageable));
    }

    @DeleteMapping(value = "/{chatId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> deleteChat(@PathVariable long chatId, Principal principal) {
        chatService.deleteChat(chatId, principal.getName());
        return ResponseDTO.ok();
    }

    //Can be enhanced
    @GetMapping(value = "/participant/{chatId}")
    public ResponseDTO<Page<UserDetailResponseDTO>> getChatParticipant(@PathVariable Long chatId,
                                                                       @ParameterObject
                                                                       @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
                                                                       @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                                       Principal principal) {
        return ResponseDTO.ok(chatService.getChatParticipants(chatId, pageable, principal.getName()));
    }

    @GetMapping(value = "/message/{chatId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Page<MessageDTO>> getListOfMessage(@PathVariable Long chatId,
                                                          @ParameterObject
                                                          @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
                                                          @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                          Principal principal) {
        return ResponseDTO.ok(chatService.getListMessage(chatId, pageable, principal.getName()));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> updateChat(@Valid @RequestBody EditChatDTO dto, Principal principal) {
        chatService.editChatName(dto, principal.getName());
        return ResponseDTO.ok();
    }

    @PostMapping(value = "/{chatId}/leave")
    public ResponseDTO<Void> leaveChat(@PathVariable Long chatId, Principal principal) {
        chatService.leaveChat(chatId, principal.getName());
        return ResponseDTO.ok();
    }

    @PostMapping(value = "/participant/add")
    public ResponseDTO<Void> addParticipant(@Valid @RequestBody AddParticipantDTO dto) {
        chatService.addParticipant(dto);
        return ResponseDTO.ok();
    }
}

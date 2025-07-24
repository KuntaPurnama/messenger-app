package com.app.messenger.controller;

import com.app.messenger.dto.*;
import com.app.messenger.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/messsage")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<FileUploadDTO> handleUpload(@RequestParam("files") List<MultipartFile> files) {
        FileUploadDTO fileUploadDTO = messageService.uploadFile(files);

        return ResponseDTO.ok(fileUploadDTO);
    }

    @GetMapping(value = "/{chatId}/undelivered", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<List<MessageDTO>> getUndeliveredMessage(@PathVariable long chatId, Principal principal) {
        return ResponseDTO.ok(messageService.getUndeliveredMessage(chatId, principal.getName()));
    }

    @GetMapping(value = "/{chatId}/count/undelivered", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Integer> countUndeliveredMessage(@PathVariable long chatId, Principal principal) {
        return ResponseDTO.ok(messageService.countUndeliveredMessage(chatId, principal.getName()));
    }

    @GetMapping(value = "/{chatId}/status/read", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> updateMessageActivityStatusToRead(@PathVariable long chatId, Principal principal) {
        messageService.updateMessageActivityStatusToRead(chatId, principal.getName());
        return ResponseDTO.ok();
    }

    @PostMapping(value = "/chat/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> handleChat(@RequestBody MessageRequestDTO messageDTO, Principal principal) {
        messageService.sendMessage(messageDTO, principal.getName());

        return ResponseDTO.ok();
    }

    @PostMapping(value = "/chat/message/typing", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> handleUserTypingStatus(@RequestBody UserTypingStatusDTO userTypingStatusDTO, Principal principal) {
        messageService.notifyUserTypingStatus(userTypingStatusDTO, principal.getName());

        return ResponseDTO.ok();
    }
}

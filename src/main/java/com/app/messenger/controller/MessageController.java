package com.app.messenger.controller;

import com.app.messenger.dto.FileUploadDTO;
import com.app.messenger.dto.MessageDTO;
import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController("/api/v1/messsage")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<FileUploadDTO> handleUpload(@RequestParam("files") List<MultipartFile> files) {
        FileUploadDTO fileUploadDTO = messageService.uploadFile(files);

        return ResponseDTO.ok(fileUploadDTO);
    }

    @GetMapping(value = "/{chatId}/sent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<List<MessageDTO>> getUndeliveredMessage(@PathVariable long chatId, Principal principal) {
        return ResponseDTO.ok(messageService.getUndeliveredMessage(chatId, principal.getName()));
    }

    @GetMapping(value = "/{chatId}/count/sent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Integer> countUndeliveredMessage(@PathVariable long chatId, Principal principal) {
        return ResponseDTO.ok(messageService.countUndeliveredMessage(chatId, principal.getName()));
    }
}

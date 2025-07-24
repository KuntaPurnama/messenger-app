package com.app.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private Long id;
    private long chatId;
    private String content;
    private String senderPhoneNumber;
    private List<MessageActivityDTO> activities;
    private List<MessageAttachmentDTO> attachments;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}

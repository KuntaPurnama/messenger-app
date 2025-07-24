package com.app.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageEventDTO {
    private long id;
    private long chatId;
    private String content;
    private String senderPhoneNumber;
    private String recipientPhoneNumber;
    private List<MessageActivityDTO> activities;
    private List<MessageAttachmentDTO> attachments;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}

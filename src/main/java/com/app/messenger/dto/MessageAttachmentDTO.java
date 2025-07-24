package com.app.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentDTO {
    private long id;
    private long messageId;
    private String fileUrl;
    private String fileType;
    private long fileSize;
}

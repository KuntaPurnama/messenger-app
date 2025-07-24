package com.app.messenger.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessageRequestDTO {
    private long chatId;
    private String content;
    private List<String> attachmentURLs;
}

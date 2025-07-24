package com.app.messenger.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserTypingStatusDTO {
    private long chatId;
    private boolean isTyping;
}

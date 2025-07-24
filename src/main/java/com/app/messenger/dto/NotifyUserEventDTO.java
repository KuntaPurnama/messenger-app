package com.app.messenger.dto;

import com.app.messenger.dto.enumeration.NotificationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotifyUserEventDTO {
    private long chatId;
    private String recipientPhoneNumber;
    private String senderPhoneNumber;
    private NotificationType type;
}

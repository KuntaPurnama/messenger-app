package com.app.messenger.dto;

import com.app.messenger.dto.enumeration.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class NotifyUserEventDTO {
    private long chatId;
    private long messageId;
    private String recipientPhoneNumber;
    private String senderPhoneNumber;
    private NotificationType type;
    private ZonedDateTime userLastSeen;
}

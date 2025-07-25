package com.app.messenger.dto;

import com.app.messenger.dto.enumeration.MessageReactionEnum;
import com.app.messenger.dto.enumeration.MessageStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageActivityDTO {
    private long id;
    private long messageId;
    private String userPhoneNumber;
    private MessageStatusEnum status;
    private MessageReactionEnum reaction;
}

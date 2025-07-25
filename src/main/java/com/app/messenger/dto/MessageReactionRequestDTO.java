package com.app.messenger.dto;

import com.app.messenger.dto.enumeration.MessageReactionEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageReactionRequestDTO {
    @NotNull(message = "messageId can't be null")
    private Long messageId;

    @NotNull(message = "reaction can't be null")
    private MessageReactionEnum reaction;
}

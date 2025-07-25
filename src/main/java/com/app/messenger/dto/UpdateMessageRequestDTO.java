package com.app.messenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateMessageRequestDTO {
    @NotNull(message = "messageId can't be null")
    private Long messageId;

    @NotBlank(message = "content can't be empty")
    private String content;
}

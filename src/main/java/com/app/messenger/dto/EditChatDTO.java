package com.app.messenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EditChatDTO {
    @NotBlank(message = "chatId can't be empty")
    private Long chatId;

    @NotBlank(message = "name can't be empty")
    private String name;
}

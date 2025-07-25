package com.app.messenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatRequestDTO {
    private String name;

    @NotNull(message = "participantPhoneNumbers can't be null")
    @NotEmpty(message = "participantPhoneNumbers can't be empty")
    private List<String> participantPhoneNumbers;
    private boolean isGroup;
}

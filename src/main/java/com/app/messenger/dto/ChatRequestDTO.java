package com.app.messenger.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatRequestDTO {
    private String name;
    private List<String> participantPhoneNumbers;
    private boolean isGroup;
}

package com.app.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO {
    private long id;
    private String name;
    private boolean isGroup;
    private String createdBy;
    private List<String> participantPhoneNumbers;
}

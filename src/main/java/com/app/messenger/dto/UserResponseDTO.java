package com.app.messenger.dto;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class UserResponseDTO {
    private String username;
    private String phoneNumber;
    private String profileImageUrl;
    private ZonedDateTime lastSeenAt;
}

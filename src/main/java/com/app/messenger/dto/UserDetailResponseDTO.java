package com.app.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailResponseDTO {
    private String phoneNumber;
    private String username;
    private String profileImageUrl;
    private ZonedDateTime lastSeenAt;
}

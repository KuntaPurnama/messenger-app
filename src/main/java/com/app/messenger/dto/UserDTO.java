package com.app.messenger.dto;

import com.app.messenger.dto.enumeration.UserStatusEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private String username;
    private String phoneNumber;
    private String profileImageURL;
}

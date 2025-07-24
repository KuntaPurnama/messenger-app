package com.app.messenger.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRequestDTO {
    private String phoneNumber;
    private String password;
}

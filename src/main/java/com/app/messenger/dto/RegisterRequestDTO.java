package com.app.messenger.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequestDTO {
    private String username;
    private String password;
    private String phoneNumber;
}

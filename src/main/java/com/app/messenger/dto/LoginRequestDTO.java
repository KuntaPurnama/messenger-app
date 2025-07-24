package com.app.messenger.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRequestDTO {
    @NotNull(message = "Phone number can't be null")
    @NotEmpty(message = "Phone number can't be empty")
    private String phoneNumber;

    @NotNull(message = "Password must not be null")
    private String password;
}

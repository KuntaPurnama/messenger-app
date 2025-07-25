package com.app.messenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRequestDTO {
    @NotBlank(message = "Phone number can't be empty")
    private String phoneNumber;

    @NotBlank(message = "Password must can't be empty")
    private String password;
}

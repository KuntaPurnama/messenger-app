package com.app.messenger.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequestDTO {
    @NotNull(message = "Phone number can't be null")
    @NotEmpty(message = "Phone number can't be empty")
    private String phoneNumber;

    @NotNull(message = "Password can't be null")
    @NotEmpty(message = "Password can't be empty")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
            message = "Password must be at least 8 characters and contain both letters and numbers"
    )
    private String password;

    private String username;

}

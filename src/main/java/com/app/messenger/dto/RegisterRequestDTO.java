package com.app.messenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequestDTO {
    @NotBlank(message = "Phone number can't be empty")
    @Pattern(regexp = "\\d{8,}", message = "Phone number must contain only digits and be at least 8 digits")
    private String phoneNumber;

    @NotBlank(message = "Password can't be empty")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
            message = "Password must be at least 8 characters and contain both letters and numbers"
    )
    private String password;

    @NotBlank(message = "username can't be empty")
    private String username;

}

package com.app.messenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ContactRequestDTO {
    @NotBlank(message = "contactPhoneNumber can't be empty")
    @Pattern(regexp = "\\d{8,}", message = "Phone number must contain only digits and be at least 8 digits")
    private String contactPhoneNumber;
}

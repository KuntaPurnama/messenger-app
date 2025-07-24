package com.app.messenger.error.exception;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseException extends RuntimeException {
    private int code;
    private String message;
    private HttpStatus httpStatus;
    private Map<String, String> errors;
}

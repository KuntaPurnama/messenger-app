package com.app.messenger.error.handler;

import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.error.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(value = BaseException.class)
    public ResponseEntity<ResponseDTO<String>> handleBaseException(BaseException ex){
        ResponseDTO<String> responseDTO = new ResponseDTO<>();
        responseDTO.setMessage(ex.getMessage());
        responseDTO.setCode(ex.getCode());

        return new ResponseEntity<>(responseDTO, ex.getHttpStatus());
    }

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<ResponseDTO<String>> handleRuntimeException(RuntimeException ex){
        ResponseDTO<String> responseDTO = new ResponseDTO<>();
        responseDTO.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        responseDTO.setMessage(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());

        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ResponseDTO<Map<String,String>> responseDTO = new ResponseDTO<>();
        responseDTO.setMessage(ex.getMessage());
        responseDTO.setCode(HttpStatus.BAD_REQUEST.value());
        responseDTO.setErrors(errors);

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }
}

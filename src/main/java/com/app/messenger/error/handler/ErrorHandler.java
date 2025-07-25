package com.app.messenger.error.handler;

import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.error.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.naming.SizeLimitExceededException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(value = BaseException.class)
    public ResponseEntity<ResponseDTO<String>> handleBaseException(BaseException ex){
        log.error("error", ex);

        ResponseDTO<String> responseDTO = new ResponseDTO<>();
        responseDTO.setMessage(ex.getMessage());
        responseDTO.setCode(ex.getCode());

        return new ResponseEntity<>(responseDTO, ex.getHttpStatus());
    }

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<ResponseDTO<String>> handleRuntimeException(RuntimeException ex){
        log.error("error", ex);

        ResponseDTO<String> responseDTO = new ResponseDTO<>();
        responseDTO.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        responseDTO.setMessage(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());

        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("error", ex);

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDTO<String>> handleEnumConversionError(HttpMessageNotReadableException ex) {
        log.error("error", ex);

        ResponseDTO<String> responseDTO = new ResponseDTO<>();
        responseDTO.setCode(HttpStatus.BAD_REQUEST.value());
        responseDTO.setMessage(HttpStatus.BAD_REQUEST.getReasonPhrase());

        Throwable mostSpecificCause = ex.getMostSpecificCause();
        if (mostSpecificCause instanceof IllegalArgumentException && ex.getMessage().contains("Enum")) {
            responseDTO.setErrors("Invalid value for reaction: Allowed value: THUMBS_UP, LOVE, CRYING, SURPRISED");
        }else {
            responseDTO.setErrors(ex.getMessage());
        }

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseDTO<String>> handleUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.error("error", ex);

        ResponseDTO<String> responseDTO = new ResponseDTO<>();
        responseDTO.setCode(HttpStatus.BAD_REQUEST.value());
        responseDTO.setMessage("File too large, max 10MB");

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SizeLimitExceededException.class)
    public ResponseEntity<ResponseDTO<String>> handleSizeLimitExceededException(SizeLimitExceededException ex) {
        log.error("error", ex);

        ResponseDTO<String> responseDTO = new ResponseDTO<>();
        responseDTO.setCode(HttpStatus.BAD_REQUEST.value());
        responseDTO.setMessage(HttpStatus.BAD_REQUEST.getReasonPhrase());

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }
}

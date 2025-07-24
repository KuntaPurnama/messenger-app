package com.app.messenger.error.handler;

import com.app.messenger.error.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = BaseException.class)
    public ResponseEntity<BaseException> handleBaseException(BaseException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
    }

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<BaseException> handleRuntimeException(RuntimeException ex){
        BaseException baseException = new BaseException();
        baseException.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        baseException.setMessage(ex.getMessage());
        baseException.setStackTrace(ex.getStackTrace());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(baseException);
    }
}

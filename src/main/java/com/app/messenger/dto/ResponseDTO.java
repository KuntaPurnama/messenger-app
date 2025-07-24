package com.app.messenger.dto;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ResponseDTO<T> {
    private String message;
    private Integer code;
    private T data;
    private T errors;

    public static <T> ResponseDTO<T> ok(T data){
        ResponseDTO<T> res = new ResponseDTO<>();
        res.setCode(HttpStatus.OK.value());
        res.setData(data);
        res.setMessage("");
        return res;
    }

    public static <T> ResponseDTO<T> ok(){
        ResponseDTO<T> res = new ResponseDTO<>();
        res.setCode(HttpStatus.OK.value());
        res.setMessage(HttpStatus.OK.getReasonPhrase());
        return res;
    }

    public static <T> ResponseDTO<T> badRequest(T errors, String message, Integer code) {
        ResponseDTO<T> res = new ResponseDTO<>();
        res.setErrors(errors);
        res.setCode(code);
        res.setMessage(message);
        return res;
    }

    public static ResponseDTO general(String message) {
        ResponseDTO res = new ResponseDTO<>();
        res.setMessage(message);
        return res;
    }
}

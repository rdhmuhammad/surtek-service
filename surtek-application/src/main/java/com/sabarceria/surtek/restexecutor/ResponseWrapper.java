package com.sabarceria.surtek.restexecutor;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ResponseWrapper<T> {
    private T resBody;

    private HttpStatus statusCode;

    private String message;

    public ResponseWrapper(T resBody, HttpStatus statusCode, String message) {
        this.resBody = resBody;
        this.statusCode = statusCode;
        this.message = message;
    }
}

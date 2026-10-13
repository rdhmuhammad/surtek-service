package com.sabarceria.surtek.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response<T> {
    private String messages;

    private int code = 200;

    private Boolean success;

    private T data;

    private Map<String, String> errors = new HashMap<>();

    public Response(String defaultSuccess, boolean status, int code) {
        this.messages = defaultSuccess;
        this.success = status;
        this.data = null;
        this.code = code;
    }

    public Response(String msg, boolean b) {
        this.messages = msg;
        this.success = b;
        this.code = 200;
    }

    public Response(String msg, boolean b, T body) {
        this.messages = msg;
        this.success = b;
        this.code = 200;
        this.data = body;
    }
}

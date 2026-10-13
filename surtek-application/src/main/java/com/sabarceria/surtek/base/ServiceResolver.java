package com.sabarceria.surtek.base;

import lombok.Data;

@Data
public class ServiceResolver<T> {
    private String message;

    private T data;

    private Integer code;

    public ServiceResolver(String success, Integer successCode) {
        this.message = success;
        this.code = successCode;
    }

    public ServiceResolver(T data, String success, Integer successCode) {
        this.data =data;
        this.message = success;
        this.code = successCode;
    }
}

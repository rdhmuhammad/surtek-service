package com.sabarceria.surtek.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthSecurityBaseResponse<T> {
    private int code;

    private String message;

    private T result;

    private Boolean success = true;

    private Boolean error = false;
}

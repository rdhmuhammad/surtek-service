package com.sabarceria.surtek.exception;

public class AccessNotAllowException extends RuntimeException {

    public AccessNotAllowException() {
        super("Anda tidak memiliki akses ke halaman ini");
    }

    public AccessNotAllowException(String username) {
        super(username + " tidak memiliki akses ke halaman ini");
    }

}

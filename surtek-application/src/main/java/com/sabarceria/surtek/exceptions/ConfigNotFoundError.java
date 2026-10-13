package com.sabarceria.surtek.exceptions;

public class ConfigNotFoundError extends RuntimeException {
    public ConfigNotFoundError() {super();}

    public ConfigNotFoundError(String message) {
        super(message);
    }
}

package com.example.inventory.exceptions.auth;

public class InvalidTokenTypeException extends RuntimeException {
    public InvalidTokenTypeException(String message) {
        super(message);
    }

    public InvalidTokenTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.example.fencing.error;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}

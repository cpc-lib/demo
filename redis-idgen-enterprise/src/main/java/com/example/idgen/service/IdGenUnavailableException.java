package com.example.idgen.service;

/**
 * Financial-grade failure: rather fail than generate any potentially duplicate IDs.
 */
public class IdGenUnavailableException extends RuntimeException {
    public IdGenUnavailableException(String message) {
        super(message);
    }
    public IdGenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

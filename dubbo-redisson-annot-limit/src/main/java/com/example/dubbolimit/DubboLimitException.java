package com.example.dubbolimit;

public class DubboLimitException extends RuntimeException {
    public DubboLimitException(String msg) {
        super(msg);
    }
}

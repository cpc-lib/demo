package com.example.limitdemo;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConcurrencyLimit {

    int value() default 5;

    String key();

    boolean blocking() default false;

    long timeoutMs() default 0L;

    String message() default "系统繁忙，请稍后重试";
}

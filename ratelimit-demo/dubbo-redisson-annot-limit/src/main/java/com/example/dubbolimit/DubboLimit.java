package com.example.dubbolimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DubboLimit {
    int value() default 5;

    String key();

    boolean blocking() default false;

    long timeoutMs() default 0;

    String message() default "Dubbo 并发已满，请稍后重试";
}

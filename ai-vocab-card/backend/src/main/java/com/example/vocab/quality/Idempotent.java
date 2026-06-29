package com.example.vocab.quality;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String header() default "Idempotency-Key";
}

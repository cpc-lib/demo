package com.example.vocab.quality;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    String key() default "global";
    long permitsPerMinute() default -1;
}

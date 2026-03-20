package cc.ivera.annotation;

import java.lang.annotation.*;

/**
 * 延时双删
 **/
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface ClearCache {
    String name() default "";
}

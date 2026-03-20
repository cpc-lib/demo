package cc.ivera.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveData {

    int prefixLen() default 0;

    /**
     * 后置不需要打码的长度
     */
    int suffixLen() default 0;

    /**
     * 遮罩字符
     */
    String maskingChar() default "*";
}
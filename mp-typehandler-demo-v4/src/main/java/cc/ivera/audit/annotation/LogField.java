package cc.ivera.audit.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogField {

    /**
     * 日志展示名称
     */
    String label();

    /**
     * 是否记录该字段
     */
    boolean enabled() default true;
}

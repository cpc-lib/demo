package cc.ivera.audit.annotation;

import cc.ivera.audit.enums.ChangeLogMode;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ChangeLog {

    String bizType();

    Class<?> entityClass();

    String bizId();

    String operator() default "'system'";

    String requestUri() default "''";

    String operationType() default "UPDATE";

    ChangeLogMode mode() default ChangeLogMode.ANNOTATION;

    /**
     * 硬编码字段配置，格式：fieldName:字段中文名
     */
    String[] hardcodedFields() default {};
}

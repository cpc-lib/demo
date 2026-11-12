package com.demo.seata.common.annotation;

import java.lang.annotation.*;

/**
 * 接口与业务幂等注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等唯一Key的SpEL表达式，例如: "#request.orderNo"
     */
    String key();

    /**
     * 业务场景标识，例如: "ORDER_CREATE", "STORAGE_DEDUCT", "ACCOUNT_DEBIT"
     */
    String bizType() default "DEFAULT";

    /**
     * 重复提交时的提示消息
     */
    String message() default "请求正在处理中，请勿重复提交！";
}

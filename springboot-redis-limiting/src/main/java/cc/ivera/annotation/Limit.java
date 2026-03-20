package cc.ivera.annotation;


import cc.ivera.constant.Constants;
import cc.ivera.enums.LimitType;

import java.lang.annotation.*;

/**
 * 限流注解
 *
 * @author jae
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Limit {
    /**
     * 限流key
     */
    String key() default Constants.RATE_LIMIT_KEY;

    /**
     * 限流时间,单位秒
     */
    int time() default 60;

    /**
     * 限流次数
     */
    int count() default 100;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 限流后返回的文字
     */
    String limitMsg() default "访问过于频繁，请稍候再试";
}

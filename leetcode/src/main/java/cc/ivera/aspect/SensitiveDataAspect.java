package cc.ivera.aspect;

import cn.hutool.core.util.StrUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.FieldSignature;
import org.springframework.stereotype.Component;
import cc.ivera.annotation.SensitiveData;

import java.lang.reflect.Field;

//处理接口参数
@Aspect
@Component
public class SensitiveDataAspect {

    @Pointcut("@annotation(cc.ivera.annotation.SensitiveData)")
    public void sensitiveDataPointcut() {
    }

    @Before("sensitiveDataPointcut()")
    public void beforeMethodExecution(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        Field field = ((FieldSignature) joinPoint.getSignature()).getField();
        field.setAccessible(true);
        try {
            Object fieldValue = field.get(target);
            if (fieldValue instanceof String) {
                SensitiveData annotation = field.getAnnotation(SensitiveData.class);
                int prefixLen = annotation.prefixLen();
                int suffixLen = annotation.suffixLen();
                String encryptedValue = StrUtil.hide((String) fieldValue, prefixLen, suffixLen);
                field.set(target, encryptedValue);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
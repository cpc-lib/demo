package cc.ivera.util;

import cn.hutool.core.util.StrUtil;
import cc.ivera.annotation.SensitiveData;

import java.lang.reflect.Field;

//数据隐藏工具类处理数字类字符串如身份证
public class HideUtil {
    public static Object mask(Object target) {
        Field[] fields = target.getClass().getDeclaredFields();
        for (Field field : fields) {
            boolean annotationPresent = field.isAnnotationPresent(SensitiveData.class);
            if (annotationPresent) {
                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(target);
                    if (fieldValue instanceof String) {
                        SensitiveData annotation = field.getAnnotation(SensitiveData.class);
                        int prefixLen = annotation.prefixLen();
                        int suffixLen = annotation.suffixLen();
                        String value = (String) fieldValue;
                        String encryptedValue = StrUtil.hide(value, prefixLen, suffixLen);
                        field.set(target, encryptedValue);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
        return target;
    }
}
package cc.ivera.aspect;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cc.ivera.annotation.Cache;
import cc.ivera.model.vo.AjaxResult;
import cc.ivera.util.JacksonUtil;
import cc.ivera.util.RedisUtil;
import cc.ivera.util.SpringElUtil;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;


/**
 * 缓存切面类
 */
@Component
@Aspect
@Slf4j
public class CacheAspect {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * aop切点
     * 拦截被指定注解修饰的方法
     */
    @Pointcut("@annotation(cc.ivera.annotation.Cache)")
    public void cache() {
    }

    /**
     * 缓存操作
     *
     * @param pjp
     * @return
     */
    @Around("cache()")
    public Object toCache(ProceedingJoinPoint pjp) {
        try {
            Signature signature = pjp.getSignature();
            String className = pjp.getTarget().getClass().getSimpleName();
            String methodName = signature.getName();
            Object[] args = pjp.getArgs();
            Class[] parameterTypes = new Class[args.length];
            String params = "";
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    parameterTypes[i] = args[i].getClass();
                    params += JacksonUtil.parseObjToJson(args[i]);
                }
            }
            if (StringUtils.isNotEmpty(params)) {
                params = DigestUtils.md5Hex(params);
            }
            Method method = signature.getDeclaringType().getMethod(methodName, parameterTypes);
            // 获取Cache注解
            Cache cache = method.getAnnotation(Cache.class);
            long expire = cache.expire();
            String name = cache.name();
            // 支持spring El表达式
            name = SpringElUtil.parse(name, method, args);
            String redisValue = redisUtil.get(name);
            if (StringUtils.isNotEmpty(redisValue)) {
                AjaxResult result = JSON.parseObject(redisValue, AjaxResult.class);
                log.info("数据从redis缓存中获取,key: {}", name);
                return result; // 跳出方法
            }
            Object proceed = pjp.proceed();
            redisUtil.set(name, JacksonUtil.parseObjToJson(proceed), expire, TimeUnit.MILLISECONDS);
            log.info("数据存入redis缓存,key: {}", name);
            return proceed;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return AjaxResult.error(999, "系统错误", null);
    }

}

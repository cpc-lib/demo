package cc.ivera.cache;

import com.alibaba.fastjson.JSON;
import cc.ivera.entity.Result;
import cc.ivera.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Aspect;

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
    private RedisUtils redisUtils; // json数据

    /**
     * aop切点
     * 拦截被指定注解修饰的方法
     */
    @Pointcut("@annotation(cc.ivera.cache.Cache)")
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
            // 思路： 设置存储的格式，获取即可

            Signature signature = pjp.getSignature();
            // 类名
            String className = pjp.getTarget().getClass().getSimpleName();
            // 方法名
            String methodName = signature.getName();

            // 参数处理
            Object[] args = pjp.getArgs();
            Class[] parameterTypes = new Class[args.length];
            String params = "";
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    parameterTypes[i] = args[i].getClass();
                    params += JSON.toJSONString(args[i]);
                }
            }
            if (StringUtils.isNotEmpty(params)) {
                //加密 以防出现key过长以及字符转义获取不到的情况
                params = DigestUtils.md5Hex(params);
            }

            // 获取controller中对应的方法
            Method method = signature.getDeclaringType().getMethod(methodName, parameterTypes);

            // 获取Cache注解
            Cache annotation = method.getAnnotation(Cache.class);
            long expire = annotation.expire();
            String name = annotation.name();

            // 访问redis（先尝试获取，没有则访问数据库）
            String redisKey = name + "::" + className + "::" + methodName + "::" + params;
            String redisValue = redisUtils.get(redisKey);
            if (StringUtils.isNotEmpty(redisValue)) {
                // 不为空返回数据
                Result result = JSON.parseObject(redisValue, Result.class);
                log.info("数据从redis缓存中获取,key: {}", redisKey);
                return result; // 跳出方法
            }
            Object proceed = pjp.proceed();
            redisUtils.set(redisKey, JSON.toJSONString(proceed), expire, TimeUnit.MILLISECONDS);
            log.info("数据存入redis缓存,key: {}", redisKey);
            return proceed;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return Result.fail(999, "系统错误");
    }

}

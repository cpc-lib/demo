package cc.ivera.aspectj;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import cc.ivera.annotation.Limit;
import cc.ivera.enums.LimitType;
import cc.ivera.exception.ServiceRuntimeException;
import cc.ivera.util.RedisUtil;
import cc.ivera.util.WebUtil;

import java.lang.reflect.Method;

@Aspect
@Component
public class LimitAspect {

    private final static Logger log = LoggerFactory.getLogger(LimitAspect.class);

    @Autowired
    private RedisUtil redisUtil;

    @Before("@annotation(limit)")
    public void doBefore(JoinPoint point, Limit limit) throws Throwable {
        int time = limit.time();
        int count = limit.count();
        long total = 1L;

        String combineKey = getCombineKey(limit, point);
        try {
            if (redisUtil.hasKey(combineKey)) {
                total = redisUtil.incr(combineKey, 1);  //请求进来，对应的key加1
                if (total > count)
                    throw new ServiceRuntimeException(limit.limitMsg());
            } else {
                redisUtil.set(combineKey, 1, time);  //初始化key
            }
        } catch (ServiceRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceRuntimeException("网络繁忙，请稍候再试");
        }
    }

    /**
     * 获取限流key
     *
     * @param limit
     * @param point
     * @return
     */
    public String getCombineKey(Limit limit, JoinPoint point) {
        StringBuffer stringBuffer = new StringBuffer(limit.key());
        if (limit.limitType() == LimitType.IP) {
            stringBuffer.append(WebUtil.getIpAddress(((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest())).append("-");
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        stringBuffer.append(targetClass.getName()).append("-").append(method.getName());
        return stringBuffer.toString();
    }


}

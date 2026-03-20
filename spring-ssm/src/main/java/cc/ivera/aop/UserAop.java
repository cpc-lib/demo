package cc.ivera.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class UserAop {
    @Pointcut(value="execution(* cc.ivera.*.*Service.login(..))")
    public void pt(){}


    @Around("UserAop.pt()")
    public Object method(ProceedingJoinPoint pjp){
        return null;
    }
}

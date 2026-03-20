package cc.ivera.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Aspect
public class Advice {

    /**
     * 切点表达式声明进行增强方法的表达式，声明哪些方法可被增强
     * (..)方法的参数可有可变的
     * (*)方法存在一个参数
     * (String,String)两个参数都为String
     */
    @Pointcut(value = "execution(* cc.ivera.service.AccoutService3.add(..))")
    public void pt(){}

    @Pointcut(value="execution(* cc.ivera.service.PasswordService3.openURL(..))")
    public void dataPt(){}
    /***
     * @Before
     * 增强方法的执行时机（在基本方法执行之前执行）
     */
    @Before("Advice.pt()")
    public void  before(JoinPoint joinPoint){
        System.out.println("before...");
        getArgs(joinPoint);
        //
    }

    @After("Advice.pt()")
    public void after(JoinPoint joinPoint){
        //
        System.out.println("after...");
       getArgs(joinPoint);
    }

    @AfterReturning("Advice.pt()")
    public void afterReturning(JoinPoint joinPoint){
        System.out.println("afterReturning...");
        getArgs(joinPoint);
    }

    @AfterThrowing("Advice.pt()")
    public void afterThrowing(JoinPoint joinPoint){
        System.out.println("afterThrowing...");
        getArgs(joinPoint);
    }

    @Around("Advice.dataPt()")
    public Object passwordTrim(ProceedingJoinPoint pjp) throws Throwable {
        //数据拦截处理
        Object[] args = pjp.getArgs();
        for (int i = 0; i < args.length; i++) {
            if(args[i].getClass().equals(String.class)){
                //trim()的作用去除空格
                args[i] = args[i].toString().trim();
            }
        }
        //放置处理后的数据并返回
        Object ret = pjp.proceed(args);
        return ret;
    }


    @Around("Advice.pt()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint){
        System.out.println("****************************************");
        System.out.println(Arrays.toString(proceedingJoinPoint.getArgs()));
        Object obj = null;
        try {
            obj = proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.getMessage();
        }
        return obj;
    }

    private static void getArgs(JoinPoint joinPoint){
        System.out.println("--------------------------------------------");
        Object[] args = joinPoint.getArgs();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        System.out.println(className+"."+methodName+"<<EOF");

    }
}

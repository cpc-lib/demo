package cc.ivera.service.proxy;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import cc.ivera.service.UserService;

import java.lang.reflect.Method;

public class CglibProxy {

    public static UserService createUserService(Class clazz){
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                //return method.invoke(o,args); //报错
                //return methodProxy.invoke(o,args);//报错
                Object ret = methodProxy.invokeSuper(o,args);
                if("action".equals(method.getName())){
                    System.out.println("睡觉");
                    System.out.println("打豆豆");
                }


                return ret;
            }
        });



        return (UserService)enhancer.create();
    }






}

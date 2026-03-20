package cc.ivera.service.proxy;

import com.sun.corba.se.impl.presentation.rmi.StubInvocationHandlerImpl;
import cc.ivera.service.UserService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JDKProxy {

    public static UserService createUserService(final UserService userService) {
        ClassLoader loader = userService.getClass().getClassLoader();
        Class[] interfaces = userService.getClass().getInterfaces();
        InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object ret = method.invoke(userService, args);
                if("action".equals(method.getName())){
                    System.out.println("睡觉");
                    System.out.println("打豆豆");
                }
                return ret;
            }
        };

        UserService o = (UserService) Proxy.newProxyInstance(loader, interfaces, invocationHandler);
        return o;
    }


}

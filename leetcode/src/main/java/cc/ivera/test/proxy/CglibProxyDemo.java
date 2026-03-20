package cc.ivera.test.proxy;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

// 1. 创建被代理的类（不需要实现接口）
class UserService {
    public void saveUser() {
        System.out.println("UserService: 执行保存用户逻辑");
    }
    
    public String getUserInfo(String userId) {
        return "用户信息: " + userId;
    }
}

// 2. 创建方法拦截器（相当于JDK动态代理的InvocationHandler）
class MyMethodInterceptor implements MethodInterceptor {
    /**
     * @param obj        代理对象
     * @param method      被代理的方法
     * @param args        方法参数
     * @param methodProxy 方法代理（可以用于调用父类方法）
     */
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        System.out.println("【前置处理】方法名: " + method.getName());
        
        // 调用原始方法（可以改为调用methodProxy.invokeSuper()）
        Object result = method.invoke(obj, args);
        
        System.out.println("【后置处理】返回值: " + result);
        return result;
    }
}
//https://stackoverflow.com/questions/75116023/unable-to-make-protected-final-java-lang-class-java-lang-classloader-defineclass
// 3. 客户端使用
public class CglibProxyDemo {
    public static void main(String[] args) {
        // 创建Enhancer对象，相当于JDK动态代理的Proxy类
        Enhancer enhancer = new Enhancer();
        
        // 设置父类（被代理的类）
        enhancer.setSuperclass(UserService.class);
        
        // 设置回调（方法拦截器）
        enhancer.setCallback(new MyMethodInterceptor());
        
        // 创建代理对象
        UserService proxy = (UserService) enhancer.create();
        
        // 通过代理对象调用方法
        proxy.saveUser();
        System.out.println(proxy.getUserInfo("1001"));
    }
}
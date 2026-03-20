package cc.ivera.test.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// 1. 定义接口
interface Subject {
    void doSomething();
    String getSomething(String param);
}

// 2. 真实主题类（被代理类）
class RealSubject implements Subject {
    @Override
    public void doSomething() {
        System.out.println("RealSubject: 执行具体业务逻辑");
    }

    @Override
    public String getSomething(String param) {
        return "RealSubject返回: " + param;
    }
}

// 3. 创建InvocationHandler实现类
class MyInvocationHandler implements InvocationHandler {
    private final Object target; // 被代理对象

    public MyInvocationHandler(Object target) {
        this.target = target;
    }

    // 代理方法执行逻辑
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("代理前：方法名=" + method.getName());
        
        // 调用真实对象的方法
        Object result = method.invoke(target, args);
        
        System.out.println("代理后：返回值=" + result);
        return result;
    }
}

// 4. 客户端使用
public class JdkDynamicProxyDemo {
    public static void main(String[] args) {
        // 创建被代理对象
        Subject realSubject = new RealSubject();
        
        // 创建代理处理器
        MyInvocationHandler handler = new MyInvocationHandler(realSubject);
        
        // 创建代理对象（核心方法）
        Subject proxy = (Subject) Proxy.newProxyInstance(
            realSubject.getClass().getClassLoader(), // 类加载器
            realSubject.getClass().getInterfaces(),   // 实现的接口数组
            handler                                   // 调用处理器
        );
        
        // 通过代理对象调用方法
        proxy.doSomething();
        System.out.println(proxy.getSomething("测试参数"));
    }
}
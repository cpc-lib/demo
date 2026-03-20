package cc.ivera.DMA3_单例模式;

import cc.ivera.DMA3_单例模式.DM3.*;
import org.junit.jupiter.api.Test;

/**
 * <p>单例模式测试</p>
 *
 * @Author Appleyk
 * @Blob https://blog.csdn.net/appleyk
 * @Date Created on 下午 1:09 2018-11-6
 * @Version V.1.0.1
 */
public class SingletonTest {

    /**
     * 以上四种单例模式，没有一种是十全十美的
     * 就拿最后一种来说，通过内部类机制达到了线程安全且实例只被创建一次，这些都是JVM帮我们实现的，我们无需关心
     * 但是，如果内部类自己的构造函数出现了问题，而不是在创建instance实例的时候出现了问题
     * 这时候，我们试想一下，我们外部还能顺利拿到单例对象吗？答案是肯定不行
     * 虽然这无疑会增加一道风险，但是相比我们手动加同步块来说，要好的多了
     */
    @Test
    public void test1() {
        // 1、单例模式 -- 非线程安全
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Singleton1 singleton1 = Singleton1.getInstance();
                    System.out.println(singleton1);
                }
            }).start();
        }
    }

    @Test
    public void test2() {
        // 2、单例模式 -- 线程安全,每次获取对象，都对当前的对象加锁
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //执行加锁
                    Singleton2 singleton2 = Singleton2.getInstance();
                    System.out.println(singleton2);
                }
            }).start();
        }

    }

    @Test
    public void test3() {
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 3、单例模式 -- 线程安全,只在第一次创建实例对象的时候加锁
                    Singleton3 singleton3 = Singleton3.getInstance();
                    System.out.println(singleton3);
                }
            }).start();

        }

    }

    @Test
    public void test4() {
        //类加载执行初始化一次
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 4、单例模式 -- 内部静态工厂类提前创建好实例，JVM保证instance只创建一次，且线程安全
                    Singleton4 singleton4 = Singleton4.getInstance();
                    System.out.println(singleton4);
                }
            }).start();
        }
    }

    @Test
    public void test5() {
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Singleton5 instance = Singleton5.instance;
                    System.out.println(instance);
                }
            }).start();
        }
    }
}

package cc.ivera.test.thread.create;

/**
 * @author e2607
 * @version 1.0
 * @description: 继承Thread类实现多线程
 * @date 11/27/2021 10:52 AM
 */
public class CreateThreadMethodA extends Thread {

    @Override
    public void run() {
        printHello();
    }

    private void printHello() {
        for (int i = 0; i < 10; i++) {
            System.out.println("Hello!---" + (i + 1));
        }
    }
}

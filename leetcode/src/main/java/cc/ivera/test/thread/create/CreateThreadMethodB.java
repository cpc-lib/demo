package cc.ivera.test.thread.create;

/**
 * @author e2607
 * @version 1.0
 * @description: 实现Runnable接口实现多线程
 * @date 11/27/2021 10:52 AM
 */
public class CreateThreadMethodB implements Runnable {

    @Override
    public void run() {
        printHola();
    }

    private void printHola() {
        for (int i = 0; i < 10; i++) {
            System.out.println("Hola!---" + (i + 1));
        }
    }
}

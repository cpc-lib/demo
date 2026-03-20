package cc.ivera.test.thread.communication;

/**
 * @author e2607
 * @version 1.0
 * @description: 等待唤醒机制，生产者与消费者
 * @date 11/27/2021 9:04 PM
 */
public class Main {

    public static void main(String[] args) {
        Queue queue = new Queue();
        //锁对象的唯一，使用等待唤醒确保线程交替执行
        new Producer(queue).start();
        new Consumer(queue).start();
    }
}

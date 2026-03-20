package cc.ivera.test.thread.communication;

/**
 * @author e2607
 * @version 1.0
 * @description: 生产者
 * @date 11/27/2021 9:00 PM
 */
public class Producer extends Thread {

    private int index = 0;

    private Queue queue;

    public Producer(Queue queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while ((index++) < 50) {
            synchronized (queue) {
                if (queue.getFlag() == true) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //flag为false时，consumer获取锁就唤醒producer执行操作
                //producer获取锁进行生产，之后唤醒consumer线程进行消费
                System.out.println("producer's work done");
                //修改队列信息为有
                queue.setFlag(true);
                queue.notify();
            }
        }
    }
}

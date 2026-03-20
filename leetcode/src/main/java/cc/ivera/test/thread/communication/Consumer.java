package cc.ivera.test.thread.communication;

/**
 * @author e2607
 * @version 1.0
 * @description: 消费者
 * @date 11/27/2021 9:01 PM
 */
public class Consumer extends Thread {

    private int index = 0;

    private Queue queue;


    public Consumer(Queue queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while ((index++) < 50) {
            synchronized (queue) {
                if (queue.getFlag() == false) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //如果队列信息为有，producer获取锁唤醒consumer进行消费
                //consumer获取锁，消费后设置状态并唤醒生产者进行工作
                System.out.println("consumer's work done");
                //消费者消费完，设置状态为无，唤醒producer
                queue.setFlag(false);
                queue.notify();
            }
        }
    }

}

package cc.ivera.test.thread.communication;

/**
 * @author e2607
 * @version 1.0
 * @description: 队列
 * @date 11/27/2021 9:00 PM
 */
public class Queue {

    private boolean flag = false;
    //默认为无消息，如消费者获取到锁对象，进行判断，线程等待唤醒生产进行工作。


    public Queue() {
    }

    public Queue(Boolean flag) {
        this.flag = flag;
    }

    public boolean getFlag() {
        return flag;
    }

    public void setFlag(Boolean flag) {
        this.flag = flag;
    }
}

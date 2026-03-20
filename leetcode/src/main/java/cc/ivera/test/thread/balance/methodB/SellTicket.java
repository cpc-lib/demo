package cc.ivera.test.thread.balance.methodB;

/**
 * @author e2607
 * @version 1.0
 * @description: 模拟售票（使用lock锁传递参数为true来实现公平消费）
 * @date 11/27/2021 12:01 PM
 */
public class SellTicket implements Runnable {

    private volatile int ticket = 90;

    @Override
    public void run() {
        while (true) {
            /*指代的为TestThread,因为使用的是implements方式。若使用继承Thread类的方式，慎用this*/
            synchronized (this) {
                //等待唤醒机制来实现售票（必须时同一锁对象）
                /*唤醒另外一个线程，注意是this的方法，而不是Thread*/
                //notify与notifyAll的区别
                //notify唤醒由监视器管理的单个线程
                //notifyAll唤醒由监视器管理的所有线程
                //wait与notify方法必须在同步代码块或同步方法中使用
                notify();
                try {
                    /*使其休眠100毫秒，放大线程差异*/
                    Thread.currentThread();
                    //sleep不会释放锁资源
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (ticket > 0) {
                    System.out.println(Thread.currentThread().getName() + "正在售票,还剩下" + (--ticket) + "张票");
                    try {
                        /*放弃资源，等待*/
                        //释放锁
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
            }
        }
    }
}

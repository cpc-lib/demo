package cc.ivera.test.thread.security.methodA;

/**
 * @author e2607
 * @version 1.0
 * @description: 模拟售票（使用同步代码块来保证线程安全）
 * @date 11/27/2021 11:06 AM
 */
public class SellTicket implements Runnable {

    //共享资源-票数300张

    private volatile int ticketCount = 300;

    private Object object = new Object();


    @Override
    public void run() {
        while (true) {
            //锁对象必须唯一(锁对象为object)
            //synchronized 可以实现原子性，可见性，有序行。
            //volatile 只能实现可见性
            //一般同步的范围越大，性能就越差，一般需要加锁进行同步的时候，肯定是范围越小越好，这样性能更好
            synchronized (object) {
                if (ticketCount > 0) {
                    System.out.println(Thread.currentThread().getName() + "正在售票,还剩下" + (--ticketCount) + "张票");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("票已销售完毕！");
                    break;
                }
            }
        }
    }
}

package cc.ivera.test.thread.security.methodC;

/**
 * @author e2607
 * @version 1.0
 * @description: 模拟售票（使用同步方法来确保线程安全）
 * @date 11/27/2021 12:01 PM
 */
public class SellTicket implements Runnable {

    private volatile static int ticketCount = 300;

    private static synchronized int sellTicket() {
        return action();
    }

    /**
     * 静态同步方法 锁对象不能是this
     * 锁对象是本类的calss属性-->class文件
     * 静态优先与对象的创建
     */
    private static int sellTicket2() {
        synchronized (SellTicket.class) {
            return action();
        }
    }

    private static int action() {
        if (ticketCount > 0) {
            System.out.println(Thread.currentThread().getName() + "正在售票,还剩下" + (--ticketCount) + "张票");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("票已销售完毕！");
        }
        return ticketCount;
    }

    @Override
    public void run() {
        while (true) {
            int leftTicket = sellTicket2();
            if (leftTicket == 0) {
                //票出售完后，退出循环
                break;
            }
        }
    }
}

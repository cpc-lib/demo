package cc.ivera.test.thread.balance.methodA;

/**
 * @author e2607
 * @version 1.0
 * @description: 模拟售票
 * @date 11/27/2021 12:01 PM
 */
public class Main {
    public static void main(String[] args) {
        sellTicket();
    }

    private static void sellTicket() {
        SellTicket sellTicket = new SellTicket();
        //线程任务默认执行一次。如果需执行多次应使用循环及条件控制
        new Thread(sellTicket).start();
        new Thread(sellTicket).start();
        new Thread(sellTicket).start();
    }
}

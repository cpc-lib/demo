package cc.ivera.test.thread.balance.methodB;

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

    //启用两个线程时可以实现公平，但是线程数超过2时，无法实现公平锁

    private static void sellTicket() {
        SellTicket sellTicket = new SellTicket();
        new Thread(sellTicket).start();
        new Thread(sellTicket).start();
    }
}

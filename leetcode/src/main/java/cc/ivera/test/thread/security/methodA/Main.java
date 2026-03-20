package cc.ivera.test.thread.security.methodA;

/**
 * @author e2607
 * @version 1.0
 * @description: 模拟售票
 * @date 11/27/2021 11:12 AM
 */
public class Main {
    public static void main(String[] args) {
        sellTicket();
    }

    private static void sellTicket() {
        SellTicket sellTicket = new SellTicket();
        new Thread(sellTicket).start();
        new Thread(sellTicket).start();
        new Thread(sellTicket).start();
    }
}

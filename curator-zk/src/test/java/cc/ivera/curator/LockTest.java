package cc.ivera.curator;


public class LockTest {


    public static void main(String[] args) {
        Ticket12306 ticket12306 = new Ticket12306();

        //创建客户端
        Thread t1 = new Thread(ticket12306,"携程");
        Thread t2 = new Thread(ticket12306,"飞猪0");
        Thread t3 = new Thread(ticket12306,"飞猪1");
        Thread t4 = new Thread(ticket12306,"飞猪2");
        Thread t5 = new Thread(ticket12306,"飞猪4");

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
    }

}

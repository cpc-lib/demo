package cc.ivera.test.thread.Demo.test2;

public class ABCTest {
    static volatile Integer count = 1;

    public static void main(String[] args) {
        new Thread(() -> {
            try {
                for (int i = 0; i < 10; ) {
                    while (count % 3 != 1) {

                    }

                    synchronized (count) {
                        if (count % 3 == 1) {
                            System.out.println(Thread.currentThread().getName() + "-----" + "A");
                            count++;
                            i++;
                        }
                    }
                }
            } catch (Exception e) {

            }

        }, "A1").start();

        new Thread(() -> {
            try {
                for (int i = 0; i < 10; ) {
                    while (count % 3 != 2) {

                    }

                    synchronized (count) {
                        if (count % 3 == 2) {
                            System.out.println(Thread.currentThread().getName() + "-----" + "B");
                            count++;
                            i++;
                        }
                    }
                }
            } catch (Exception e) {

            }
        }, "B1").start();

        new Thread(() -> {
            try {
                for (int i = 0; i < 10; ) {
                    while (count % 3 != 0) {

                    }

                    synchronized (count) {
                        if (count % 3 == 0) {
                            System.out.println(Thread.currentThread().getName() + "-----" + "C");
                            count++;
                            i++;
                        }
                    }
                }
            } catch (Exception e) {

            }
        }, "C1").start();
    }

}

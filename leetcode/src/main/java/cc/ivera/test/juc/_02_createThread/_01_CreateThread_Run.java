package cc.ivera.test.juc._02_createThread;


public class _01_CreateThread_Run {
    public static void main(String[] args) {
        Thread thread = new MyThread();
        thread.start();
        System.out.println("main ends");
    }
}

class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("sub thread");
    }
}
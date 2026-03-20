package cc.ivera.test.base;

public class Demo93 {
    public static void main(String[] args) {
        Thread helloWorld = new Thread(new Runnable() {
            public void run() {
                System.out.println("Hello world");
            }
        });
        helloWorld.start();
        helloWorld.start();
    }
}

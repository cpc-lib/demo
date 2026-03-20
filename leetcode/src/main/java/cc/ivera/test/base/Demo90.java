package cc.ivera.test.base;

public class Demo90 {


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        System.out.println(fibonacciRecursive(10));
        long end =System.currentTimeMillis();
        System.out.println("耗时：" + (end - start));
    }



    public static int fibonacci(int n) {
        if (n <= 0) {
            return 0;
        }

        int a = 0; // F(0)
        int b = 1; // F(1)
        int c;

        for (int i = 2; i <= n; i++) {
            c = a + b;
            a = b;
            b = c;
        }

        return b;
    }

    public static int fibonacciRecursive(int n) {
        if (n <= 0) {
            return 0;
        } else if (n == 1 || n == 2) {
            return 1;
        } else {
            return fibonacciRecursive(n - 1) + fibonacciRecursive(n - 2);
        }
    }


}

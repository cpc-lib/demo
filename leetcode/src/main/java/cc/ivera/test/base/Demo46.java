package cc.ivera.test.base;

/**
 * return的运行时机
 */
public class Demo46 {
    public static void main(String[] args) {
        System.out.println(test());
    }

    static int test() {
        int x = 1;
        try {
            return x;
        } finally {
            ++x;
        }
    }

}

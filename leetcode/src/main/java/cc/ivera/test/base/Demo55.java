package cc.ivera.test.base;

public class Demo55 {
    private static int j;

    public static void main(String[] args) {
        int i = 0;
        while (i++ < 3 || j++ < 7) {
            String s = "I love you 3,000";
            System.out.println(s);
        }
    }
}

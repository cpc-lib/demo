package cc.ivera.test.base;

public class Demo32 {
    public static void main(String[] args) {
        String time = "2023-01-01";
        System.out.println(time.substring(0, 4));
        System.out.println(Integer.parseInt(time.substring(5, 7)));
        System.out.println(Integer.parseInt(time.substring(8, 10)));

    }
}

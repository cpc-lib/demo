package cc.ivera.test.base;

public class Demo51 {
    public static void main(String[] args) {
        String str = "";
        String[] split = str.split(";");
        for (int i = 0; i < split.length; i++) {
            System.out.println(split[i]);
        }
    }
}

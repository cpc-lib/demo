package cc.ivera.test.base;

public class Demo53 {
    public static void main(String[] args) {
        String fileId = "123456789";
        String[] split = fileId.split(";");
        System.out.println(split.length);
        System.out.println(split[0]);
    }
}

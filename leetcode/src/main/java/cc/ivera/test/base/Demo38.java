package cc.ivera.test.base;

import lombok.SneakyThrows;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Demo38 {

    //线程不安全 Demo39-Demo44 解决办法
    public static void main(String[] args) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    Date parse = simpleDateFormat.parse("2023-11-11 11:11:11");
                    System.out.println(parse);
                }
            });
            thread.start();
        }
    }
}

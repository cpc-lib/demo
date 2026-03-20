package cc.ivera.test.base;

import cc.ivera.util.MoneyUtil;

public class Demo33 {

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String number = "12.56";
                    System.out.println(Thread.currentThread().getName() + ":" + number + ": " + MoneyUtil.toChinese(number));

                    number = "1234567890563886.123";
                    System.out.println(Thread.currentThread().getName() + ":" + number + ": " + MoneyUtil.toChinese(number));

                    number = "1600";
                    System.out.println(Thread.currentThread().getName() + ":" + number + ": " + MoneyUtil.toChinese(number));

                    number = "156,0";
                    System.out.println(Thread.currentThread().getName() + ":" + number + ": " + MoneyUtil.toChinese(number));

                    number = "-156,0";
                    System.out.println(Thread.currentThread().getName() + ":" + number + ": " + MoneyUtil.toChinese(number));

                    number = "0.12";
                    System.out.println(Thread.currentThread().getName() + ":" + number + ": " + MoneyUtil.toChinese(number));

                    number = "0.0";
                    System.out.println(Thread.currentThread().getName() + ":" + number + ": " + MoneyUtil.toChinese(number));
                }
            });
            thread.start();
        }

    }
}

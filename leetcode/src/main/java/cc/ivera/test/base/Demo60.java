package cc.ivera.test.base;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Demo60 {
    private String sharedStr = "Hello world!";
    @Test
    public void test2() {
        String icon = "http://114.132.210.77:9000/mall/20240329/home-bg.png";
        String URL_PATTERN = "^http://114\\.132\\.210\\.77:9000/mall/(.*)$";
        Pattern pattern = Pattern.compile(URL_PATTERN);
        Matcher matcher = pattern.matcher(icon);
        boolean matches = matcher.matches();
        if (matches) {
            String objectName = removePrefix(icon);
            Assert.assertEquals("20240329/home-bg.png", objectName);
        }

    }

    private String removePrefix(String url) {
        String PREFIX_TO_REMOVE = "http://114.132.210.77:9000/mall/";
        if (url != null && url.startsWith(PREFIX_TO_REMOVE)) {
            return url.substring(PREFIX_TO_REMOVE.length());
        } else {
            // 如果URL不以指定的前缀开始，则原样返回或者抛出异常
            return url; // 或者你可以根据需要抛出异常
        }
    }

    //单例测试->双重判断加锁确保单例
    @Test
    public void test3() {
        for (int i = 0; i < 10000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Singleton instance = Singleton.getInstance();
                    System.out.println(instance);
                }
            }).start();
        }
    }


    //单例创建方式二
    @Test
    public void test4() {
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Singleton singleton = Singleton.SINGLETON;
                    System.out.println(singleton);
                }
            }).start();
        }
    }

    @Test
    public void test5() {
        for (int i = 0; i < 100; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //1.7以上做了substring创建了新的String 线程安全
                    String substring = sharedStr.substring(0, 5);
                    System.out.println(substring);
                }
            }).start();
        }
    }

    @Test
    public void test6() {
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //没有加入双重判断,测试出现多例
                    Singleton singleton = Singleton.getInstance2();
                    System.out.println(singleton);
                }
            }).start();
        }
    }

    @Test
    public void test7() {
        String value = "430422199705029796";
        for (int i = 0; i < 10_000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String hiddenStr = StrUtil.hide(value, 4, 14);
                    System.out.println(Thread.currentThread().getName() + "-->" + hiddenStr);
                }
            }).start();
        }
    }


    @Test
    public void test8() {
        String dateStr = "2000-12-19";
        String year = dateStr.substring(0, 4);
        String month = dateStr.substring(5, 7);
        String day = dateStr.substring(8, 10);
        System.out.println(year);
        System.out.println(month);
        System.out.println(day);
    }


    @Test
    public void test9() {
        StringJoiner stringJoiner = new StringJoiner(";");
        System.out.println(stringJoiner.toString());
    }


    /**
     * 图片url代理
     */
    @Test
    public void test10() {
        String file_path = "/20240617/18026378851545292801802643120296038400.png";
        String prefix = "http://120.78.132.79:8095/download";
        if (StringUtils.isNotEmpty(file_path)) {
            String substring = file_path.substring(0, 1);
            System.out.println(file_path);
            if ("/".equals(substring)) {
                prefix = prefix + file_path;
                System.out.println(prefix);
            } else {
                prefix = prefix + "/" + file_path;
                System.out.println(prefix);
            }
        }
    }

}
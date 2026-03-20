package cc.ivera.test.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Demo61 {
    public static void main(String[] args) {
        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(0, "2000年12月22日");
        map.put(1, "2000年12月23日");
        map.put(2, "2000年12月24日");
        map.put(3, "2000年12月25日");
        map.put(4, "2000年12月26日");
        map.put(5, "2000年12月27日");
        map.put(6, "2000年12月28日");
        map.put(7, "2000年12月29日");
        map.put(8, "2000年12月30日");
        map.put(9, "2000年12月31日");
        map.put(10, "2001年01月01日");
        map.put(11, "2001年01月02日");
        map.put(12, "2001年01月03日");
        map.put(13, "2001年01月04日");
        map.put(14, "2001年01月05日");
        map.put(15, "2001年01月06日");
        map.put(16, "2001年01月07日");
        map.put(17, "2001年01月08日");
        map.put(18, "2001年01月09日");
        map.put(19, "2001年01月10日");
        map.put(20, "2001年01月11日");
        map.put(21, "2001年01月12日");
        map.put(22, "2001年01月13日");
        map.put(23, "2001年01月14日");
        map.put(24, "2001年01月15日");
        map.put(25, "2001年01月16日");
        map.put(26, "2001年01月17日");
        map.put(27, "2001年01月18日");
        map.put(28, "2001年01月19日");

        System.out.println("如果数字0代表2000年12月22日");
        System.out.println("如果数字28代表2001年01月19日");
        boolean flag = true;
        while (flag) {
            System.out.println("请选择一个数字");
            Scanner scanner = new Scanner(System.in);
            int i = scanner.nextInt();
            if(i==7){
                flag=false;
                System.out.println("我的生日是"+map.get(i));
            }
        }


    }
}

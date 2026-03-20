package cc.ivera.test.base;

import java.util.regex.Pattern;

public class Demo50 {

    public static void main(String[] args) {
        String path1 = "d:\\target";
        String path2 = "d:\\test\\target\\test";

        System.out.println("Path 1 contains target: " + containsTarget(path1));
        System.out.println("Path 2 contains target: " + containsTarget(path2));
    }

    private static boolean containsTarget(String path) {
        // 使用正则表达式匹配路径是否包含 "target"
        String regex = ".*\\\\target.*";
        return Pattern.matches(regex, path);
    }
}

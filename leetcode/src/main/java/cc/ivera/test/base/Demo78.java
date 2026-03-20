package cc.ivera.test.base;

import cc.ivera.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Demo78 {
    public static void main(String[] args) {
        List<String> files = FileUtil.findFiles(new File("E:\\迅雷下载\\谈判专家"), new ArrayList<>());
        files.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println(s);
            }
        });
    }
}

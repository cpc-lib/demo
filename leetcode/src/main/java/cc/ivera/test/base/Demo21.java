package cc.ivera.test.base;


import cc.ivera.util.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Demo21 {
    public static void main(String[] args) {
        String filePath = "E:\\test.txt";
        File directory = new File("E:\\source\\Go语言全栈开发"); // 将"/path/to/your/directory"替换为你要遍历的文件夹的路径
        if (directory.exists() && directory.isDirectory()) {
            List<String> fileList = new ArrayList<>();
            List<String> strings = FileUtil.viewFilesRecursivly(directory, fileList);
            boolean flag = false;
            if (strings.size() > 0) {
                for (String string : strings) {
//                    flag = containsChar(string);
//                    if (flag) {
//                        writeToFile(filePath, string);
////                        File tempFile = new File(string);
////                        tempFile.delete();
//                    }
                    writeToFile(filePath,string);
                }
            }
            System.out.println("Content has been appended to the file.");
        } else {
            System.err.println("指定的路径不是一个有效的文件夹。");
        }
    }

    private static boolean containsTarget(String path) {
        // 使用正则表达式匹配路径是否包含 "target"
        String regex = ".*\\\\target.*";
        return Pattern.matches(regex, path);
    }


    private static boolean containsChar(String path) {
        // 使用正则表达式匹配路径是否包含 "target"
        String regex = ".*\\\\（.*";
        return Pattern.matches(regex, path);
    }

    private static void writeToFile(String filePath, String target) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            // 要追加的字符串
            writer.write(target);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

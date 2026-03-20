package cn.itcast.nio.c3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestFilesCopy {


    public static void main(String[] args) throws IOException {
        fileCopy("D:\\develop\\jar", "D:\\develop\\jar-backup");
    }

    /**
     * 文件拷贝
     *
     * @param source
     * @param target
     * @throws IOException
     */
    private static void fileCopy(String source, String target) throws IOException {
        long start = System.currentTimeMillis();

        Files.walk(Paths.get(source)).forEach(path -> {
            try {
                String targetName = path.toString().replace(source, target);
                // 是目录
                if (Files.isDirectory(path)) {
                    Files.createDirectory(Paths.get(targetName));
                }
                // 是普通文件
                else if (Files.isRegularFile(path)) {
                    Files.copy(path, Paths.get(targetName));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
}

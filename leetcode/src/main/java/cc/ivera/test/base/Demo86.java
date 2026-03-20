package cc.ivera.test.base;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Demo86 {

    public static void main(String[] args) {
        // 源目录
        String sourceDirectoryPath = "E:\\entertainment\\Drama\\Arcane\\S2";
        // 目标目录
        String targetDirectoryPath = "E:\\target";
        // 线程数量
        int numberOfThreads = 10;

        // 获取源目录下的所有文件
        File sourceDirectory = new File(sourceDirectoryPath);
        List<File> files = new ArrayList<>();
        if (sourceDirectory.exists() && sourceDirectory.isDirectory()) {
            File[] fileArray = sourceDirectory.listFiles((dir, name) -> !new File(dir, name).isDirectory());
            if (fileArray != null) {
                files.addAll(java.util.Arrays.asList(fileArray));
            }
        } else {
            System.out.println("源目录不存在或不是一个目录！");
            return;
        }

        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);


        // 提交任务到线程池
        for (File file : files) {
            executorService.submit(() -> {
                try {
                    copyFile(file, targetDirectoryPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        // 关闭线程池
        executorService.shutdown();

        //long end = System.currentTimeMillis();

        try {
            long start = System.currentTimeMillis();
            if (!executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS))
                    System.err.println("线程池没有在规定时间内关闭");
            }
            long end = System.currentTimeMillis();
            System.out.println((end - start) + "ms");

        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("所有文件移动完成！");
    }

    private static void copyFile(File file, String targetDirectoryPath) throws IOException {
        Path sourcePath = Paths.get(file.getAbsolutePath());
        Path targetPath = Paths.get(targetDirectoryPath, file.getName());
        Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("已移动文件: " + file.getName());
    }
}

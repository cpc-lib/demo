package cc.ivera;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FolderSingleThreadApplication {

    public static void main(String[] args) {
        if (args.length != 3) {
            printUsage();
            return;
        }

        String operation = args[0].trim().toLowerCase(Locale.ROOT);
        Path sourceDir = Paths.get(args[1]).toAbsolutePath().normalize();
        Path targetDir = Paths.get(args[2]).toAbsolutePath().normalize();

        long start = System.currentTimeMillis();

        try {
            switch (operation) {
                case "copy":
                    copyDirectory(sourceDir, targetDir);
                    break;
                case "move":
                    moveDirectory(sourceDir, targetDir);
                    break;
                default:
                    System.out.println("不支持的操作类型: " + operation);
                    printUsage();
                    return;
            }

            long cost = System.currentTimeMillis() - start;
            System.out.println();
            System.out.println("处理完成，总耗时: " + cost + " ms");
        } catch (Exception e) {
            System.err.println("处理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        validatePath(sourceDir, targetDir);
        Files.createDirectories(targetDir);

        Counter counter = new Counter();

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(dir);
                Path targetSubDir = targetDir.resolve(relative);
                Files.createDirectories(targetSubDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relative = sourceDir.relativize(file);
                Path targetFile = targetDir.resolve(relative);

                try {
                    Files.createDirectories(targetFile.getParent());
                    Files.copy(file, targetFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);

                    counter.success++;
                    System.out.println("[成功][copy] " + file + " -> " + targetFile + "，累计成功: " + counter.success);
                } catch (Exception e) {
                    counter.fail++;
                    System.err.println("[失败][copy] " + file + " -> " + targetFile + "，原因: " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        printSummary("copy", counter);
    }

    private static void moveDirectory(Path sourceDir, Path targetDir) throws IOException {
        validatePath(sourceDir, targetDir);
        Files.createDirectories(targetDir);

        Counter counter = new Counter();
        List<Path> directories = new ArrayList<>();

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                directories.add(dir);
                Path relative = sourceDir.relativize(dir);
                Path targetSubDir = targetDir.resolve(relative);
                Files.createDirectories(targetSubDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relative = sourceDir.relativize(file);
                Path targetFile = targetDir.resolve(relative);

                try {
                    Files.createDirectories(targetFile.getParent());
                    Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);

                    counter.success++;
                    System.out.println("[成功][move] " + file + " -> " + targetFile + "，累计成功: " + counter.success);
                } catch (Exception e) {
                    counter.fail++;
                    System.err.println("[失败][move] " + file + " -> " + targetFile + "，原因: " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        deleteEmptyDirectories(sourceDir, directories);
        printSummary("move", counter);
    }

    private static void deleteEmptyDirectories(Path sourceDir, List<Path> directories) throws IOException {
        for (int i = directories.size() - 1; i >= 0; i--) {
            Path dir = directories.get(i);
            if (Files.exists(dir) && !dir.equals(sourceDir)) {
                try {
                    Files.delete(dir);
                } catch (DirectoryNotEmptyException ignored) {
                    // 保留非空目录
                }
            }
        }

        if (Files.exists(sourceDir)) {
            try {
                Files.delete(sourceDir);
                System.out.println("已删除空源目录: " + sourceDir);
            } catch (DirectoryNotEmptyException ignored) {
                System.out.println("源目录仍有未移动内容，保留目录: " + sourceDir);
            }
        }
    }

    private static void validatePath(Path sourceDir, Path targetDir) {
        if (!Files.exists(sourceDir)) {
            throw new IllegalArgumentException("源目录不存在: " + sourceDir);
        }
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("源路径不是目录: " + sourceDir);
        }
        if (sourceDir.equals(targetDir)) {
            throw new IllegalArgumentException("源目录和目标目录不能相同");
        }
        if (targetDir.startsWith(sourceDir)) {
            throw new IllegalArgumentException("目标目录不能是源目录的子目录，否则会导致递归处理问题");
        }
    }

    private static void printSummary(String operation, Counter counter) {
        System.out.println();
        System.out.println("========== 执行结果 ==========");
        System.out.println("操作类型: " + operation);
        System.out.println("成功处理文件数: " + counter.success);
        System.out.println("失败文件数: " + counter.fail);
    }

    private static void printUsage() {
        System.out.println("用法:");
        System.out.println("  java -jar target/folder-tool-single-thread.jar <copy|move> <源目录> <目标目录>");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -jar target/folder-tool-single-thread.jar copy D:/a D:/b");
        System.out.println("  java -jar target/folder-tool-single-thread.jar move D:/a D:/b");
    }

    private static class Counter {
        private int success;
        private int fail;
    }
}

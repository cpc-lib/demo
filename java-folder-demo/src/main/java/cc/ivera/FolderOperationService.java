package cc.ivera;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FolderOperationService {

    private final ThreadPoolExecutor executor;
    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failCount = new AtomicInteger();

    public FolderOperationService(int corePoolSize, int maxPoolSize, int queueCapacity) {
        if (corePoolSize <= 0 || maxPoolSize <= 0 || queueCapacity <= 0) {
            throw new IllegalArgumentException("线程池参数必须大于 0");
        }
        if (corePoolSize > maxPoolSize) {
            throw new IllegalArgumentException("核心线程数不能大于最大线程数");
        }

        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("folder-operation-pool-" + thread.getId());
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        processDirectory(sourceDir, targetDir, OperationType.COPY);
    }

    public void moveDirectory(Path sourceDir, Path targetDir) throws IOException {
        processDirectory(sourceDir, targetDir, OperationType.MOVE);
    }

    private void processDirectory(Path sourceDir, Path targetDir, OperationType operationType) throws IOException {
        validatePath(sourceDir, targetDir);

        successCount.set(0);
        failCount.set(0);

        Files.createDirectories(targetDir);

        List<Path> directories = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
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
                futures.add(executor.submit(() -> processSingleFile(sourceDir, targetDir, file, operationType)));
                return FileVisitResult.CONTINUE;
            }
        });

        waitForAllTasks(futures);

        if (operationType == OperationType.MOVE) {
            deleteEmptyDirectories(sourceDir, directories);
        }

        System.out.println("操作类型: " + operationType.name().toLowerCase());
        System.out.println("成功处理文件数: " + successCount.get());
        System.out.println("失败文件数: " + failCount.get());
    }

    private void processSingleFile(Path sourceDir, Path targetDir, Path sourceFile, OperationType operationType) {
        Path relative = sourceDir.relativize(sourceFile);
        Path targetFile = targetDir.resolve(relative);

        try {
            Files.createDirectories(targetFile.getParent());

            if (operationType == OperationType.COPY) {
                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            } else {
                Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            int current = successCount.incrementAndGet();
            System.out.println("[成功][" + operationType.name().toLowerCase() + "] " +
                    sourceFile + " -> " + targetFile + "，累计成功: " + current);

        } catch (FileAlreadyExistsException e) {
            failCount.incrementAndGet();
            System.err.println("[失败] 目标文件已存在: " + targetFile);
        } catch (Exception e) {
            failCount.incrementAndGet();
            System.err.println("[失败][" + operationType.name().toLowerCase() + "] " +
                    sourceFile + " -> " + targetFile + "，原因: " + e.getMessage());
        }
    }

    private void waitForAllTasks(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("线程池任务执行失败", e);
            }
        }
    }

    private void deleteEmptyDirectories(Path sourceDir, List<Path> directories) throws IOException {
        for (int i = directories.size() - 1; i >= 0; i--) {
            Path dir = directories.get(i);
            if (Files.exists(dir) && !dir.equals(sourceDir)) {
                try {
                    Files.delete(dir);
                } catch (DirectoryNotEmptyException ignored) {
                }
            }
        }

        if (Files.exists(sourceDir)) {
            try {
                Files.delete(sourceDir);
                System.out.println("已删除空源目录: " + sourceDir);
            } catch (DirectoryNotEmptyException ignored) {
                System.out.println("源目录仍有未处理内容，保留目录: " + sourceDir);
            }
        }
    }

    private void validatePath(Path sourceDir, Path targetDir) {
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

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private enum OperationType {
        COPY,
        MOVE
    }
}
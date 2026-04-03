package cc.ivera;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderOperationApplication {

    public static void main(String[] args) {
        if (args.length < 3 || args.length > 6) {
            printUsage();
            return;
        }

        String operation = args[0].trim().toLowerCase();
        Path sourceDir = Paths.get(args[1]).toAbsolutePath().normalize();
        Path targetDir = Paths.get(args[2]).toAbsolutePath().normalize();

        int corePoolSize = args.length >= 4 ? Integer.parseInt(args[3]) : 4;
        int maxPoolSize = args.length >= 5 ? Integer.parseInt(args[4]) : 8;
        int queueCapacity = args.length >= 6 ? Integer.parseInt(args[5]) : 1000;

        FolderOperationService service = new FolderOperationService(corePoolSize, maxPoolSize, queueCapacity);

        long start = System.currentTimeMillis();
        try {
            if ("copy".equals(operation)) {
                service.copyDirectory(sourceDir, targetDir);
            } else if ("move".equals(operation)) {
                service.moveDirectory(sourceDir, targetDir);
            } else {
                System.out.println("不支持的操作类型: " + operation);
                printUsage();
                return;
            }

            long cost = System.currentTimeMillis() - start;
            System.out.println("\n处理完成，总耗时: " + cost + " ms");
        } catch (Exception e) {
            System.err.println("处理失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
    }

    private static void printUsage() {
        System.out.println("用法:");
        System.out.println("  java -jar folder-tool.jar <copy|move> <源目录> <目标目录> [核心线程数] [最大线程数] [队列容量]");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -jar folder-tool.jar copy D:/data/a D:/data/b 4 8 1000");
        System.out.println("  java -jar folder-tool.jar move D:/data/a D:/data/b 4 8 1000");
    }
}
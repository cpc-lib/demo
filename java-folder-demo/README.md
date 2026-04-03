# Java 自定义线程池移动文件夹项目

## 功能说明

使用 `ThreadPoolExecutor` 自定义线程池，将 A 目录中的所有文件并发移动到 B 目录，并保留原有目录结构。

## 特性

- 使用自定义线程池处理文件移动
- 保留源目录层级结构
- 支持大批量文件并发移动
- 自动创建目标目录
- 移动完成后尝试删除空源目录
- 支持自定义核心线程数、最大线程数、队列容量

## 项目结构

```text
java-folder-move-threadpool
├── pom.xml
├── README.md
└── src
    └── main
        └── java
            └── com
                └── example
                    └── move
                        ├── FolderMoveApplication.java
                        └── FolderMoveService.java
```

## 打包

```bash
mvn clean package
```

## 运行

```bash
java -jar target/folder-move-demo-1.0.0.jar <操作名> <源目录> <目标目录> [核心线程数] [最大线程数] [队列容量]
```

## 示例

```bash
复制：

java -jar java-folder-demo.jar copy D:/Telegram/1 D:/Telegram/2 32 64 1000

移动：

java -jar java-folder-demo.jar move D:/Telegram/1 D:/Telegram/2 32 64 1000
```

## 参数说明

- 源目录：要移动的原始目录
- 目标目录：移动后的目标目录
- 核心线程数：可选，默认 4
- 最大线程数：可选，默认 8
- 队列容量：可选，默认 1000

## 注意事项

1. 目标目录不能是源目录的子目录。
2. 若目标文件已存在，将被覆盖。
3. 移动使用 `Files.move(..., REPLACE_EXISTING)`。
4. 如果有文件被占用或权限不足，失败信息会打印到控制台。

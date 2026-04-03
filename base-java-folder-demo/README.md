# folder-single-thread-demo

Java 单线程目录复制 / 移动工具。

## 功能
- 支持 copy / move
- 保留原目录结构
- move 完成后尝试删除空源目录
- Maven 打包后可直接 `java -jar` 运行

## 编译
```bash
mvn clean package
```

## 运行
### 复制
```bash
java -jar target/base-java-folder-demo.jar copy D:/Telegram/1 D:/Telegram/2
```

### 移动
```bash
java -jar target/base-java-folder-demo.jar move D:/Telegram/1 D:/Telegram/2
```

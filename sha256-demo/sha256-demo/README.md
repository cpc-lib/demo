# Java SHA-256 Demo

一个纯 Java 实现的 SHA-256 计算 Demo，可用于：

- 计算 ZIP / JAR / EXE / PDF / 图片等任意文件的 SHA-256
- 计算字符串 SHA-256
- 大文件流式读取，不会一次性加载到内存
- Java 8+ 可运行

## 项目结构

```text
java-sha256-demo/
├── pom.xml
├── README.md
└── src/main/java/com/example/sha256/
    ├── Sha256Application.java
    └── Sha256Utils.java
```

## Maven 编译

```bash
mvn clean package
```

## 直接使用 javac 编译

Linux / macOS：

```bash
mkdir -p target/classes
javac -encoding UTF-8 -d target/classes src/main/java/com/example/sha256/*.java
```

Windows CMD：

```cmd
mkdir target\classes
javac -encoding UTF-8 -d target\classes src\main\java\com\example\sha256\*.java
```

## 计算文件 SHA-256

```bash
java -cp target/classes com.example.sha256.Sha256Application file demo.zip
```

也支持直接传文件路径：

```bash
java -cp target/classes com.example.sha256.Sha256Application demo.zip
```

Windows 示例：

```cmd
java -cp target\classes com.example.sha256.Sha256Application file "D:\download\demo.zip"
```

输出示例：

```text
文件: D:\download\demo.zip
大小: 123456 bytes
SHA-256: 4e1e7159d18b2e89947ecb5265a3841f743a012408ba940d0dbfb383469cf801
```

## 计算字符串 SHA-256

```bash
java -cp target/classes com.example.sha256.Sha256Application text hello
```

`hello` 的 SHA-256 应为：

```text
2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
```

## 核心调用

```java
String sha256 = Sha256Utils.calculateFileSha256("demo.zip");
System.out.println(sha256);
```

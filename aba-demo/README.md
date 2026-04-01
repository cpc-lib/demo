# ABA Java Demo

这个示例包含两个 Java 类：

1. `AbaProblemDemo`
   - 使用 `AtomicInteger` 演示 ABA 问题
   - 值从 `100 -> 101 -> 100` 后，另一个线程的 CAS 仍然会成功

2. `AbaSolutionDemo`
   - 使用 `AtomicStampedReference` 解决 ABA 问题
   - 通过“值 + 版本号”识别中间是否发生过变更

## 运行方式

在项目根目录执行：

```bash
mvn compile
java -cp target/classes com.example.aba.AbaProblemDemo
java -cp target/classes com.example.aba.AbaSolutionDemo
```

## 预期现象

- `AbaProblemDemo` 中，线程 t1 的 CAS 会成功，体现 ABA 问题
- `AbaSolutionDemo` 中，线程 t1 的 CAS 会失败，因为版本号已经变化

# Java 死锁案例与解决方案 Demo

这个示例包含 3 个类：

1. `DeadlockProblemDemo`
   - 演示经典死锁
   - 两个线程持有不同锁，并互相等待对方释放

2. `DeadlockSolutionOrderedLockDemo`
   - 解决方案一：固定加锁顺序
   - 所有线程都先获取 `LOCK_A`，再获取 `LOCK_B`

3. `DeadlockSolutionTryLockDemo`
   - 解决方案二：`ReentrantLock + tryLock + 超时重试`
   - 获取不到第二把锁时主动释放第一把锁并重试

## 运行方式

在项目根目录执行：

```bash
mvn compile
java -cp target/classes com.example.deadlock.DeadlockProblemDemo
java -cp target/classes com.example.deadlock.DeadlockSolutionOrderedLockDemo
java -cp target/classes com.example.deadlock.DeadlockSolutionTryLockDemo
```

## 预期现象

### 死锁案例
运行 `DeadlockProblemDemo` 后，通常会看到：

- `t1` 获取到 `LOCK_A`
- `t2` 获取到 `LOCK_B`
- 然后两个线程都在等待另一把锁
- 程序卡住不退出

### 解决方案一
运行 `DeadlockSolutionOrderedLockDemo`：

- 所有线程按统一顺序获取锁
- 不会形成循环等待
- 程序正常结束

### 解决方案二
运行 `DeadlockSolutionTryLockDemo`：

- 获取不到锁时不永久阻塞
- 主动释放已有锁并重试
- 程序正常结束

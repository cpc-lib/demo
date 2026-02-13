# YARN 常用任务与资源管理命令速查

本文整理了在 **Apache Hadoop YARN** 环境中常用的应用、日志、容器、节点及队列管理命令，适合日常运维与排障快速查阅。

---

## 一、查看任务（Application）

### 1. 查看当前任务列表
```bash
yarn application -list
```

### 2. 按状态查看任务列表
```bash
yarn application -list -appStates <STATE>
```

支持的状态包括：

- `ALL`
- `NEW`
- `NEW_SAVING`
- `SUBMITTED`
- `ACCEPTED`
- `RUNNING`
- `FINISHED`
- `FAILED`
- `KILLED`

示例：
```bash
yarn application -list -appStates RUNNING
```

### 3. 杀掉指定任务
```bash
yarn application -kill <ApplicationId>
```

示例：
```bash
yarn application -kill application_1612577921195_0001
```

---

## 二、查看任务日志

### 查看指定 Application 的日志
```bash
yarn logs -applicationId <ApplicationId>
```

示例：
```bash
yarn logs -applicationId application_1612577921195_0001
```

---

## 三、查看任务尝试（ApplicationAttempt）

### 1. 列出 Application 的所有 Attempt
```bash
yarn applicationattempt -list <ApplicationId>
```

### 2. 查看指定 ApplicationAttempt 状态
```bash
yarn applicationattempt -status <ApplicationAttemptId>
```

---

## 四、查看容器（Container）

### 1. 列出某次 ApplicationAttempt 下的所有 Container
```bash
yarn container -list <ApplicationAttemptId>
```

### 2. 查看指定 Container 状态
```bash
yarn container -status <ContainerId>
```

---

## 五、查看节点（NodeManager）

### 列出所有节点（包含非 RUNNING 状态）
```bash
yarn node -list -all
```

---

## 六、队列与资源管理

### 1. 刷新队列配置（管理员操作）
```bash
yarn rmadmin -refreshQueues
```

> 用于在不重启 ResourceManager 的情况下，重新加载 `capacity-scheduler.xml` 或 `fair-scheduler.xml`。

### 2. 查看指定队列状态
```bash
yarn queue -status <QueueName>
```

示例：
```bash
yarn queue -status root.default
```

---

## 七、使用建议

- **任务卡在 ACCEPTED**：通常是队列资源不足或队列容量限制。
- **频繁 FAILED**：优先查看 `yarn logs` 中的 AM 和 Container 日志。
- **Container 异常退出**：结合 `yarn container -status` 与 NodeManager 日志排查。
- **修改队列配置后**：记得执行 `yarn rmadmin -refreshQueues`。

---

📌 *本文可作为 YARN 运维与排障的命令速查手册使用。*

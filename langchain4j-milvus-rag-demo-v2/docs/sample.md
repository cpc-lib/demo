# Sample Knowledge

Java 线程池的核心参数：

- corePoolSize：核心线程数
- maximumPoolSize：最大线程数
- keepAliveTime：非核心线程空闲存活时间
- workQueue：任务队列（如 LinkedBlockingQueue、SynchronousQueue）
- threadFactory：线程工厂
- handler：拒绝策略（AbortPolicy、CallerRunsPolicy、DiscardPolicy、DiscardOldestPolicy）

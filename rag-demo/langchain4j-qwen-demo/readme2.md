一、Java 基础核心面试题（高频版）
1️⃣ JDK / JRE / JVM 区别

答：

JDK（Java Development Kit）：开发工具包，包含 JRE + 编译器 + 工具

JRE（Java Runtime Environment）：运行环境，包含 JVM + 核心类库

JVM（Java Virtual Machine）：Java 虚拟机，负责运行字节码

关系：

JDK > JRE > JVM


追问：

JVM 为什么能跨平台？

因为不同系统实现了不同 JVM，但字节码标准统一

2️⃣ == 和 equals() 区别

答：

比较	==	equals
基本类型	比较值	-
引用类型	比较地址	比较内容

例如：

String a = new String("abc");
String b = new String("abc");

System.out.println(a == b);      // false
System.out.println(a.equals(b)); // true


追问：

String 为什么重写 equals？

equals 为什么必须重写 hashCode？

3️⃣ hashCode 和 equals 的关系

规则：

equals 相等 → hashCode 必须相等

hashCode 相等 → equals 不一定相等

原因：
用于 HashMap、HashSet 定位桶

4️⃣ String 为什么不可变？

答：

final 修饰 char[]

没有提供修改方法

线程安全

可缓存 hashCode

可作为 HashMap key

private final char value[];


追问：

StringBuilder 和 StringBuffer 区别？

StringBuffer 线程安全

StringBuilder 性能更好

5️⃣ 重载 vs 重写
区别	重载	重写
发生位置	同类	子类
参数	必须不同	必须相同
返回值	可不同	必须兼容
访问权限	无限制	不能更严格
6️⃣ Java 线程有几种创建方式？

三种：

继承 Thread

实现 Runnable

实现 Callable（可返回值）

推荐：

ExecutorService pool = Executors.newFixedThreadPool(5);


追问：

Runnable 和 Callable 区别？

Future 是什么？

在 Java 里，Future 是一个“异步任务的结果占位符”。

简单说一句话：

👉 Future 表示一个“未来某个时间点才会得到的计算结果”。

它通常配合线程池（ExecutorService）使用，用来获取异步执行任务的结果。

一、为什么需要 Future？

在你现在这种 Java 后端开发场景（比如 Spring Boot + AI 调用 + IO 操作）中，经常会遇到：

调用远程接口（大模型）

查询数据库

执行耗时计算

文件上传下载

ClickHouse 分析任务

如果主线程等待这些操作完成，会阻塞。

于是就有了：

提交任务 → 立刻返回 → 以后再拿结果


这就是 Future 的作用。

二、Future 的基本使用
示例代码
ExecutorService executor = Executors.newSingleThreadExecutor();

Future<String> future = executor.submit(() -> {
Thread.sleep(3000);
return "任务完成";
});

System.out.println("主线程继续执行...");

// 阻塞等待结果
String result = future.get();

System.out.println(result);

executor.shutdown();

执行流程
1️⃣ 提交任务
2️⃣ 任务在子线程执行
3️⃣ 主线程继续运行
4️⃣ future.get() 时才等待结果

三、Future 核心方法
方法	作用
get()	获取结果（会阻塞）
get(timeout)	超时等待
isDone()	是否执行完成
cancel()	取消任务
isCancelled()	是否被取消
四、Future 的问题

Future 有几个致命缺点：

❌ 1. get() 会阻塞
future.get(); // 卡住

❌ 2. 无法做链式调用

不能这样：

A执行完 → 再执行B → 再执行C

❌ 3. 不支持回调
五、升级版：CompletableFuture（重点🔥）

Java 8 之后推荐使用：

CompletableFuture


它是 Future 的增强版。

示例
CompletableFuture.supplyAsync(() -> {
return "Hello";
}).thenApply(result -> {
return result + " World";
}).thenAccept(System.out::println);


输出：

Hello World

优点
能力	CompletableFuture
非阻塞	✅
链式调用	✅
异常处理	✅
组合多个任务	✅
六、实际开发中怎么选？
场景	推荐
简单线程池返回结果	Future
复杂异步编排	CompletableFuture
Spring WebFlux	Mono / Flux
AI 流式输出	SSE + Flux
七、面试回答模板（背诵版）

Future 是 Java 并发包中的一个接口，用来表示异步计算的结果。
通过 ExecutorService 提交任务时会返回 Future 对象，可以通过 get() 获取结果。
但 get() 是阻塞的，因此 Java 8 引入了 CompletableFuture，支持链式调用和非阻塞编排，是更推荐的异步编程方式。







7️⃣ Java 线程状态
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED


常考：

wait() 进入 WAITING

sleep() 进入 TIMED_WAITING

8️⃣ synchronized 底层原理

JDK 1.6 后优化：

偏向锁

轻量级锁

重量级锁

基于：

对象头 + Monitor

9️⃣ volatile 作用

保证：

可见性

禁止指令重排序

不保证：

原子性

🔟 ConcurrentHashMap 1.7 vs 1.8 区别
版本	实现
1.7	Segment 分段锁
1.8	CAS + synchronized

1.8 更细粒度锁

二、集合框架高频题
11️⃣ ArrayList 和 LinkedList 区别
特点	ArrayList	LinkedList
底层	数组	双向链表
查询	快	慢
插入删除	慢	快
12️⃣ HashMap 底层原理

JDK 1.8：

数组 + 链表 + 红黑树


当链表长度 ≥ 8 → 转红黑树

13️⃣ HashMap 为什么线程不安全？

扩容可能死循环（1.7）

数据覆盖

解决：

ConcurrentHashMap

一、JDK1.7 如何保证线程安全？
✅ 核心思想：分段锁（Segment）

结构：

ConcurrentHashMap
├── Segment[0]
├── Segment[1]
├── Segment[2]
└── ...


每个 Segment 本质是一个小 HashMap。

🔐 加锁粒度

不是锁整个 Map

而是锁某一个 Segment

不同 Segment 可以并发操作

示意：

线程A -> 锁 Segment[1]
线程B -> 锁 Segment[5]


互不影响。

原理

Segment 继承自 ReentrantLock：

static final class Segment<K,V> extends ReentrantLock


所以本质是：

分段锁 = 多把 ReentrantLock

二、JDK1.8 如何保证线程安全？（重点🔥）

JDK1.8 完全重写了实现。

✅ 不再使用 Segment
✅ 使用 CAS + synchronized
1️⃣ 数据结构

和 HashMap 类似：

Node[] table

2️⃣ 插入数据时的线程安全机制
情况一：桶为空

使用 CAS 操作

CAS(tab[i], null, newNode)


如果成功 → 插入成功
如果失败 → 说明有竞争 → 进入 synchronized

情况二：桶不为空

使用 synchronized 锁住当前桶头节点

synchronized (f) {
// 链表插入
}


⚠️ 注意：

不是锁整个 Map
只锁当前桶

三、为什么效率高？
锁粒度变小了

JDK1.7：

锁 Segment


JDK1.8：

锁 单个桶


锁的粒度更细 → 并发度更高

四、size() 如何保证准确？

这是面试追问🔥

因为高并发下统计 size 很难。

JDK1.8 使用：

baseCount + CounterCell[]


类似 LongAdder 的分段计数思想：

多线程更新不同的计数槽


最后汇总。

五、读操作为什么不用加锁？

关键点🔥

因为：

Node.value 是 volatile
volatile V val;
volatile Node<K,V> next;


保证：

可见性

有序性

读操作：

get() 不加锁


因为：

不会修改结构

volatile 保证可见

六、JDK1.7 vs 1.8 对比（面试必问）
对比项	JDK1.7	JDK1.8
数据结构	Segment + HashEntry	Node[] + 链表 + 红黑树
锁机制	ReentrantLock	CAS + synchronized
锁粒度	Segment级别	桶级别
并发度	默认16	理论上更高
性能	好	更好
七、为什么不用 ReentrantLock 了？

面试高级追问🔥

因为：

synchronized 在 JDK1.6 之后性能优化很多

锁对象粒度更小

代码更简洁

CAS 减少锁竞争

八、树化时如何保证线程安全？

当链表长度 > 8：

转红黑树


树化时也会加 synchronized 锁住当前桶。

九、底层核心保证总结

JDK1.8 线程安全靠：

1. volatile 保证可见性
2. CAS 保证无锁插入
3. synchronized 锁桶
4. 分段计数统计 size
5. 红黑树优化查询性能




三、JVM 高频题
14️⃣ JVM 内存结构
堆
方法区
虚拟机栈
本地方法栈
程序计数器

15️⃣ 堆和栈区别
堆	栈
存对象	存方法变量
线程共享	线程私有
16️⃣ 什么是 GC？

垃圾回收机制

判断对象是否存活：

引用计数（已废弃）

可达性分析

17️⃣ 常见垃圾回收器

Serial

ParNew

CMS

G1（默认）

ZGC

四、IO / NIO
18️⃣ BIO 和 NIO 区别
BIO	NIO
阻塞	非阻塞
一连接一线程	多路复用
五、面试必问综合题
19️⃣ 什么是双亲委派机制？

类加载时：

先让父类加载器加载
父加载不了才自己加载


好处：

防止类重复加载

防止核心类被篡改

20️⃣ 为什么重写 equals 必须重写 hashCode？

因为 HashMap 先比较 hashCode 再 equals

六、面试加分题（中高级）
21️⃣ 什么是 MVCC？

多版本并发控制
保证读写不阻塞

22️⃣ CAS 是什么？

Compare And Swap

底层：

Unsafe + CPU 原子指令

23️⃣ Java 内存模型 JMM

规定：

主内存

工作内存

保证：

原子性

可见性

有序性

七、面试回答模板（通用结构）

建议答题结构：

① 先定义
② 说底层原理
③ 说应用场景
④ 说优缺点
⑤ 举例


SpringBoot 中 @Async 底层

1) @Async 是怎么生效的

开启异步能力

@EnableAsync（Spring Boot 也可能通过自动配置/你显式加上）会注册一个 AsyncAnnotationBeanPostProcessor。

Bean 后置处理器扫描

它在容器创建 Bean 时，发现类或方法上有 @Async，就给这个 Bean 创建 AOP 代理（JDK 动态代理或 CGLIB）。

代理拦截调用

你调用 xxx.asyncMethod() 时，其实走的是代理的拦截器（AsyncExecutionInterceptor / AnnotationAsyncExecutionInterceptor）。

提交线程池

拦截器把这次方法调用封装成 Callable/Runnable，扔进 TaskExecutor（线程池）里执行。

返回值处理

void：直接返回（调用方不等结果）。

Future/CompletableFuture：立刻返回一个“占位结果”，真正计算在线程池里跑完再完成它。

2) 默认用哪个线程池？

Spring 会找一个 TaskExecutor 类型的 Bean（或名字叫 taskExecutor 的）。

如果你没配，Spring 可能退化到 SimpleAsyncTaskExecutor（不复用线程、像“每次新开线程”），不建议生产环境。

生产常见做法：自定义 ThreadPoolTaskExecutor 并暴露为 Bean。

3) 为什么你会遇到“没生效/还是同步”？

@Async 依赖 代理，所以有典型坑：

① 同类内部调用（self-invocation）不生效
@Service
class A {
@Async
public void f(){}

public void g(){
f(); // 这里是 this.f()，没经过代理 -> 不异步
}
}


解决：把异步方法放到另一个 Bean 里调用，或通过代理调用（不推荐硬取代理，容易绕晕）。

② 方法必须是 public

代理通常只拦截 public（尤其是基于接口的代理）；非 public 可能不触发。

③ 代理方式影响：final / private 等

CGLIB 不能覆盖 final 方法；private 本来也拦不到。

4) 返回值与异常怎么处理？

void 方法里的异常：不会抛回调用方，需要 AsyncUncaughtExceptionHandler 处理。

Future/CompletableFuture：异常会封装在 future 里（get()/join() 才会抛）。

5) 你在 Spring WebFlux / Reactor 场景要注意

你之前遇到过 block() 报错（reactor-http-nio 线程不允许阻塞）。@Async 的作用是把任务丢到线程池，但如果你在 响应式链路里乱用 block()，仍然会炸。

在 WebFlux 里更推荐：

用 Reactor 的 publishOn/subscribeOn 做线程切换

或把阻塞 IO 放到 Schedulers.boundedElastic() 上

6) 推荐的生产级线程池配置（示例）
   @EnableAsync
   @Configuration
   public class AsyncConfig {

@Bean("bizExecutor")
public ThreadPoolTaskExecutor bizExecutor() {
ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
ex.setCorePoolSize(8);
ex.setMaxPoolSize(32);
ex.setQueueCapacity(1000);
ex.setThreadNamePrefix("biz-async-");
ex.setWaitForTasksToCompleteOnShutdown(true);
ex.initialize();
return ex;
}
}


使用：

@Async("bizExecutor")
public CompletableFuture<String> work() { ... }


FutureTask原理

一、FutureTask 是什么？

一句话：

FutureTask = Runnable + Future + 状态机

它既可以被线程执行（Runnable），
又可以返回异步结果（Future）。

二、类结构
public class FutureTask<V> implements RunnableFuture<V>


而：

public interface RunnableFuture<V>
extends Runnable, Future<V>


所以：

FutureTask = 可执行任务 + 可获取结果

三、核心成员变量（重点🔥）

源码精简版：

private volatile int state;

private Callable<V> callable;

private Object outcome;

private volatile Thread runner;

private volatile WaitNode waiters;

四、核心原理 = 状态机 + CAS + 阻塞队列
1️⃣ 状态机设计（核心）

FutureTask 不是靠锁保证安全，

而是靠：

volatile + CAS + 状态流转


状态值：

NEW = 0
COMPLETING = 1
NORMAL = 2
EXCEPTIONAL = 3
CANCELLED = 4
INTERRUPTING = 5
INTERRUPTED = 6

状态流转图
NEW
├──> COMPLETING -> NORMAL
├──> COMPLETING -> EXCEPTIONAL
└──> CANCELLED


状态只能单向改变。

五、run() 执行流程
public void run() {
if (state != NEW) return;

    runner = Thread.currentThread();

    try {
        V result = callable.call();
        set(result);
    } catch (Throwable ex) {
        setException(ex);
    }
}

关键点

✔ 只允许一个线程执行
✔ CAS 设置 runner
✔ 执行 callable
✔ 保存结果
✔ 唤醒等待线程

六、get() 如何实现阻塞？

核心方法：

public V get() {
if (state <= COMPLETING)
awaitDone();
return report();
}

awaitDone()

核心逻辑：

创建一个 WaitNode

加入等待链表（CAS 插入）

调用 LockSupport.park() 挂起线程

执行完成后

在 set() 中：

finishCompletion();


会：

遍历等待链表
调用 LockSupport.unpark(thread)


唤醒所有等待线程。

七、线程安全靠什么？

FutureTask 保证线程安全靠：

1️⃣ volatile

state

runner

waiters

保证可见性。

2️⃣ CAS

使用 Unsafe.compareAndSwapInt

保证状态原子更新。

3️⃣ LockSupport

实现阻塞/唤醒。

4️⃣ 状态机设计

避免重复执行。

八、核心执行流程图
提交任务
↓
线程池执行 run()
↓
callable.call()
↓
保存结果到 outcome
↓
修改 state
↓
unpark 等待线程

九、为什么不用 synchronized？

因为：

CAS + volatile 性能更高


FutureTask 追求：

轻量级并发控制

ForkJoinPool原理

一、ForkJoinPool 是干什么的？

一句话：

ForkJoinPool 是一个基于“工作窃取（Work Stealing）”算法的并行线程池，专门用于处理可拆分的大任务。

典型场景：

并行计算

分治算法

递归任务

CompletableFuture 默认线程池

Java8 parallelStream

二、核心思想：分而治之（Divide & Conquer）

名字就说明了一切：

Fork → 拆分任务
Join → 合并结果


流程：

大任务
↓ fork
拆成小任务
↓ fork
再拆
↓
最小任务执行
↓ join
逐层合并

三、核心原理：工作窃取算法（Work Stealing）

这是重点🔥

每个线程都有：

一个自己的双端队列（Deque）

工作规则：

✔ 自己的任务 → 从队列尾部取
✔ 别人的任务 → 从队列头部偷

为什么这样设计？

本线程 LIFO（缓存命中高）

窃取线程 FIFO（减少冲突）

四、核心结构

ForkJoinPool 内部包含：

WorkQueue[]


每个工作线程对应一个 WorkQueue。

WorkQueue 是什么？

本质是：

双端数组队列


核心字段：

ForkJoinTask<?>[] array;
int base;  // 队列头
int top;   // 队列尾

五、执行流程
1️⃣ 提交任务
ForkJoinPool pool = new ForkJoinPool();
pool.invoke(new MyTask());

2️⃣ 任务拆分

任务继承：

RecursiveTask<V>


重写：

protected V compute()


示例：

protected Integer compute() {
if (任务很小) {
直接计算;
} else {
拆成两个子任务;
fork();
join();
}
}

3️⃣ fork()

本质：

把任务压入当前线程队列尾部

4️⃣ join()

本质：

等待子任务执行完成


如果子任务没完成：

当前线程可能去帮忙执行其他任务（避免阻塞）。

六、为什么效率高？
1️⃣ 无锁设计

大部分情况下：

线程只操作自己的队列


无锁。

2️⃣ CAS 控制窃取

偷任务时使用 CAS 控制 base。

3️⃣ 避免线程阻塞

join 时不会直接阻塞，

而是：

尝试执行其他任务


提高 CPU 利用率。

七、和普通线程池对比
对比	ThreadPoolExecutor	ForkJoinPool
队列	一个共享队列	每线程一个队列
适合场景	IO密集	CPU密集
任务拆分	不支持	原生支持
调度方式	抢占式	工作窃取
八、CompletableFuture 为什么用它？

默认线程池：

ForkJoinPool.commonPool()


原因：

适合大量小任务

支持任务拆分

高并发无锁设计


SpringIOC的原理
一、IOC 是什么？

IOC（Inversion of Control）控制反转。

一句话：

把对象的创建和依赖管理的控制权，从程序员手里交给容器。

传统写法：

UserService service = new UserService();


IOC 之后：

@Autowired
UserService service;


对象由 Spring 创建。

二、Spring 如何实现 IOC？

核心：

BeanFactory


Spring IOC 本质就是：

一个大工厂 + 一个对象缓存池

三、容器启动流程（核心流程）

以 Spring Boot 为例：

SpringApplication.run()
↓
创建 ApplicationContext
↓
refresh()
↓
完成 Bean 的创建和初始化

refresh() 是核心

内部步骤：

1. 加载 BeanDefinition
2. 注册 BeanDefinition
3. 创建 Bean
4. 依赖注入
5. 初始化

四、Bean 创建流程（源码级流程🔥）

当 getBean() 时：

AbstractBeanFactory#doGetBean()


核心调用链：

doGetBean()
↓
createBean()
↓
doCreateBean()

五、doCreateBean() 三步（最核心🔥）
1️⃣ 实例化（构造方法）
2️⃣ 属性填充（依赖注入）
3️⃣ 初始化（回调方法）

1️⃣ 实例化
createBeanInstance()


反射创建对象。

2️⃣ 属性填充
populateBean()


处理：

@Autowired

@Value

setter 注入

3️⃣ 初始化
initializeBean()


包括：

Aware 回调
BeanPostProcessor
@PostConstruct
afterPropertiesSet
init-method

六、IOC 的核心数据结构
1️⃣ BeanDefinition

存储：

类名

作用域

构造参数

依赖关系

初始化方法

2️⃣ 单例池（三级缓存🔥）

解决循环依赖关键结构：

singletonObjects        // 一级缓存
earlySingletonObjects   // 二级缓存
singletonFactories      // 三级缓存

七、循环依赖怎么解决？

A -> B
B -> A

解决靠：

提前暴露对象引用


流程：

1️⃣ 实例化 A
2️⃣ 放入三级缓存
3️⃣ 注入 B
4️⃣ B 需要 A
5️⃣ 从三级缓存拿到 A 的早期引用
6️⃣ 注入完成

八、Spring IOC 本质总结

Spring IOC 核心就是：

BeanDefinition + BeanFactory + 三级缓存 + 反射



Spring AOP原理
一、什么是 AOP？

AOP = Aspect Oriented Programming（面向切面编程）

核心作用：

👉 把“公共逻辑”从业务代码中剥离出来，统一管理。

比如：

日志记录

事务控制

权限校验

性能监控

接口限流

二、Spring AOP 底层原理

Spring AOP 只有一个核心：

动态代理


Spring 在运行时生成代理对象。

三、两种代理方式（重点🔥）
1️⃣ JDK 动态代理（默认）

目标类必须实现接口

基于 java.lang.reflect.Proxy

原理：

接口 → 生成代理类 → 方法调用被拦截


示意图：

Controller
↓
Proxy (代理对象)
↓
ServiceImpl

2️⃣ CGLIB 动态代理

目标类没有接口时使用

基于继承

通过字节码增强

原理：

生成一个子类
重写方法
在方法前后加增强逻辑

四、Spring AOP 执行流程

以 @Transactional 为例：

步骤 1️⃣ Bean 初始化

Spring 在创建 Bean 时会检查：

是否有 AOP 注解？


如果有：

创建代理对象

步骤 2️⃣ 方法调用

你调用：

userService.save();


实际上调用的是：

Proxy.save();

步骤 3️⃣ 代理内部执行

伪代码：

before();   // 开启事务

target.save();  // 执行真实方法

after();    // 提交事务


如果异常：

rollback();

五、Spring AOP 关键概念
名词	解释
Aspect	切面
Advice	通知（增强逻辑）
JoinPoint	连接点（方法）
Pointcut	切入点（哪些方法）
Weaving	织入
六、五种通知类型
@Before
@After
@AfterReturning
@AfterThrowing
@Around   // 最强

🔥 @Around 最重要
@Around("execution(* com.example.service.*.*(..))")
public Object around(ProceedingJoinPoint pjp) throws Throwable {

    System.out.println("前置");

    Object result = pjp.proceed();

    System.out.println("后置");

    return result;
}

七、底层源码关键类（面试加分🔥）
类	作用
ProxyFactory	创建代理
JdkDynamicAopProxy	JDK代理
CglibAopProxy	CGLIB代理
Advisor	封装切面
MethodInterceptor	方法拦截器
八、Spring AOP 和 AspectJ 区别
Spring AOP	AspectJ
原理	动态代理	字节码修改
织入时机	运行时	编译时
性能	较低	更高
功能	只支持方法级别	支持字段/构造器
九、常见面试追问
1️⃣ 为什么同类方法调用事务失效？
this.save(); // 失效


因为：

AOP 是代理对象生效，this 调用绕过代理。

2️⃣ 为什么 private 方法事务不生效？

因为：

CGLIB 通过重写方法实现增强，private 无法被重写。

3️⃣ Spring 默认用哪种代理？

有接口 → JDK

没接口 → CGLIB

Spring Boot 2+ 默认强制 CGLIB
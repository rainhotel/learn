# JMM、volatile 与 synchronized

## 1. 学习目标

完成本章后，你能够：

1. 解释 Java 内存模型解决的问题。
2. 识别 data race 和错误同步程序。
3. 使用 happens-before 判断一个线程的写是否对另一个线程可见。
4. 区分 volatile 的可见性/排序语义与复合操作原子性。
5. 解释 synchronized 的互斥、可见性、可重入和自动解锁。
6. 正确使用 wait/notify、sleep、start、join 和 final 字段语义。
7. 将并发状态设计应用到 NotifyFlow。

## 2. 为什么要学

NotifyFlow 中可能出现这些代码：

```java
boolean stopping;
int successCount;
ProviderConfig config;
```

多个线程读取和修改它们时，单线程直觉不够：

- 一个线程修改 `stopping=true`，其他线程何时必须看到？
- `volatile int successCount` 能否安全执行 `successCount++`？
- 替换配置对象时，其他线程会不会看到部分初始化状态？
- 给方法加 synchronized，究竟锁住了谁？
- `Thread.sleep(100)` 后重新读取变量，是否获得可见性？

JMM 给出的不是某台 CPU 的缓存图，而是：对一个多线程程序，哪些读取结果和执行行为是合法的。

## 3. 不要从“主内存/工作内存图”开始

很多教程把 JMM 简化成“线程从主内存复制变量到工作内存，再刷新回主内存”。这个图可以帮助建立直觉，但容易误导：

- JLS 用 action、program order、synchronization order 和 happens-before 定义行为。
- 编译器、JIT、CPU 和内存层次可以采用不同实现，只要产生的行为符合规范。
- 不能用“立即刷新缓存”替代对程序顺序关系的严谨推理。

本课程优先使用规范关系，再在性能章节讨论硬件缓存与屏障实现。

## 4. 三类常见并发问题

### 4.1 可见性

线程 A 写入共享变量后，线程 B 是否被保证观察到该写入。

### 4.2 原子性

一个操作是否不可被拆分和交错。例如 `count++` 包含读取、加一、写回，不是单一原子复合操作。

### 4.3 有序性

编译器与处理器可以进行不改变单线程语义的重排。错误同步程序可能观察到与源码直觉不同的跨线程顺序。

互斥是另一重要概念：同一时刻是否只允许一个线程进入临界区。synchronized 同时提供互斥与内存可见性关系，volatile 不提供互斥。

## 5. shared variables 与 data race

JLS 17.4.1 将实例字段、静态字段和数组元素视为可在线程间共享的变量；局部变量和方法参数本身不会被其他线程直接访问。

两个访问发生冲突的条件：

- 访问同一个变量。
- 至少一个访问是写。

当两个冲突访问来自不同线程，且没有 happens-before 顺序将它们排序时，程序存在 data race。

正确同步程序具有重要性质：如果程序不存在 data race，其执行通常可以按 sequential consistency 进行理解，即看起来像所有线程操作以某种保持各线程程序顺序的全局次序交错。

## 6. actions 与顺序

JMM 关心的动作包括：

- 普通变量读取和写入。
- volatile 读取和写入。
- monitor lock/unlock。
- 线程 start、终止检测和中断相关动作。

### program order

每个线程内部的动作按照该线程的程序语义形成顺序。它描述单线程看来如何执行，不等于跨线程自动可见。

### synchronization order

所有 synchronization actions 存在一个与各线程程序顺序一致的全序。

### synchronizes-with

特定同步动作之间建立关系，例如：

- monitor unlock -> 后续对同一 monitor 的 lock。
- volatile write -> 后续读到该变量的 volatile read。
- Thread.start -> 被启动线程中的动作。
- 线程中的全部动作 -> 其他线程检测到它终止（如 join 正常返回）。

### happens-before

happens-before 是 program order 与 synchronizes-with 等关系的传递闭包。若 A happens-before B，则 A 的结果对 B 可见，并且 A 在内存模型顺序上先于 B。

## 7. 核心 happens-before 规则

面试与工程中最常用：

1. 程序顺序：同一线程中，前面的动作 happens-before 后面的动作。
2. monitor：unlock 同一 monitor happens-before 后续 lock。
3. volatile：写某 volatile 变量 happens-before 后续读该变量。
4. start：调用 `thread.start()` happens-before 新线程中的动作。
5. termination/join：线程中的动作 happens-before 其他线程确认其终止。
6. 传递性：A hb B 且 B hb C，则 A hb C。
7. 默认初始化先于其他动作。

必须注意：源码时间上的“先发生”不自动等于 happens-before。

## 8. volatile 安全发布

```java
class ConfigHolder {
    int timeoutMillis;
    volatile boolean ready;
}

// writer
holder.timeoutMillis = 2000;
holder.ready = true;

// reader
if (holder.ready) {
    use(holder.timeoutMillis);
}
```

推理：

1. writer 中普通字段写 program-order 在 volatile 写之前。
2. volatile 写 synchronizes-with 读取到该写的 volatile 读。
3. reader 中 volatile 读 program-order 在普通字段读之前。
4. 通过传递性，普通字段写 happens-before reader 的普通字段读。

这就是实验 1 的依据。

更推荐发布不可变对象引用：

```java
volatile ProviderConfig current = ProviderConfig.initial();

void reload(ProviderConfig next) {
    current = next;
}
```

对象构造完成后一次替换引用，读线程获取一个内部一致的快照。

## 9. volatile 不保证复合操作原子性

```java
volatile int count;
count++;
```

可分为：

```text
read count
compute count + 1
write count
```

两个线程可能都读到 10，然后都写回 11，丢失一次更新。volatile 使单次读写具有对应内存语义，但不把多个动作合并为原子事务。

选择：

- 单变量原子更新：AtomicInteger/AtomicLong。
- 高竞争统计：LongAdder（接受 sum 的瞬时语义）。
- 多字段不变量：synchronized/Lock 或不可变快照。
- 跨进程状态：数据库、Redis 或消息协议，而非 JVM 字段。

## 10. volatile 的适用条件

典型适用：

- 停止标记。
- 配置/路由表不可变快照引用。
- 单写多读状态，且新值不依赖当前值。
- 双重检查单例中的引用（正确构造前提下）。

不适用：

- `count++`。
- check-then-act：`if (!started) started=true`。
- 多字段必须同时变化。
- 需要阻塞等待和条件队列。

## 11. synchronized 与 monitor

JLS 说明每个对象都关联一个 monitor。一次只有一个线程持有特定 monitor。

```java
synchronized (lock) {
    // critical section
}
```

进入块前执行 lock，离开时执行 unlock。无论正常返回还是异常退出，Java 都会自动 unlock。

### 实例 synchronized 方法

锁 `this`。

### static synchronized 方法

锁声明该方法的 Class 对象。

### 不同实例

两个实例方法分别锁各自 this，不能保护跨实例共享静态状态。

## 12. synchronized 提供什么

### 互斥

同一 monitor 同一时刻只有一个持有线程。

### 可见性/顺序

对 monitor 的 unlock happens-before 后续 lock，因此临界区内此前写入可被后续持锁线程观察。

### 原子保护复合动作

只要所有访问都遵守同一个锁协议，可以保护多个读写组成的不变量。

```java
synchronized (lock) {
    if (remaining > 0) {
        remaining--;
    }
}
```

## 13. 可重入

同一线程可以重复锁定同一个 monitor。每次 lock 都对应一次 unlock。

因此 synchronized 方法可以调用同一对象上的另一个 synchronized 方法，而不会自我死锁。

可重入不代表多个线程可以同时进入；它只对当前持锁线程生效。

## 14. 锁对象选择

锁保护的是一组状态与不变量，不是某行代码。

推荐：

```java
private final Object lock = new Object();
```

谨慎使用：

- 字符串常量：可能被 intern 后与无关代码共享。
- 可被外部访问的对象：外部代码可能意外持锁。
- 可变锁引用：不同线程可能锁不同对象。
- `this`：若对象暴露给外部，锁协议也暴露。

所有读写必须遵循同一锁。只给写方法加锁、读方法不加锁，仍可能存在可见性和一致性问题。

## 15. 临界区边界

锁范围过小：无法保护完整不变量。

锁范围过大：降低并发并可能把慢 I/O 放入锁内。

```java
synchronized (lock) {
    // 更新内存状态
    provider.send(); // 危险：外部 I/O 持锁
}
```

更好的方式通常是：

1. 锁内验证并提取不可变命令。
2. 锁外执行慢 I/O。
3. 使用状态机/幂等处理结果回写。

但锁外操作会引入状态变化窗口，需要明确业务协议，不能机械缩小锁范围。

## 16. wait、notify 与 wait set

每个对象除 monitor 外还有 wait set。

调用 `wait` 必须持有目标 monitor，否则抛 `IllegalMonitorStateException`。wait 会：

1. 将线程加入 wait set。
2. 释放该 monitor 的全部重入层级。
3. 等待通知、中断、超时或可能的 spurious wake-up。
4. 返回前重新获取 monitor。

因此必须使用 while：

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }
    // condition holds
}
```

不能使用 if，因为可能虚假唤醒，或被唤醒后条件已被其他线程改变。

现代业务代码通常优先使用 BlockingQueue、CountDownLatch、Semaphore、Condition 等更高层工具，但理解 monitor wait set 是面试和并发基础。

## 17. sleep 与 yield 没有同步语义

JLS 明确说明 `Thread.sleep` 和 `Thread.yield` 不提供 synchronization semantics：

```java
while (!done) {
    Thread.sleep(1000);
}
```

如果 done 非 volatile 且没有其他同步，sleep 不保证循环重新观察写入。

sleep 是调度/等待工具，不是内存可见性工具。

## 18. start 与 join

```java
task.value = 42;
thread.start();
```

start 之前的动作 happens-before 新线程中的动作，因此新线程可以安全观察此前初始化。

线程中的全部动作 happens-before 另一个线程成功从 join 返回后的动作，因此 join 后可以读取线程计算结果，即使结果字段本身不是 volatile（前提是访问协议正确）。

## 19. final 字段与安全构造

JLS 对 final 字段提供特殊语义：对象正确构造且 `this` 没有在构造期间逸出时，其他线程获得对象引用后对 final 字段拥有更强的初始化可见性保证。

这也是不可变对象适合并发共享的重要原因。

错误示例：构造函数中把 `this` 注册到全局列表、启动线程或调用可覆盖方法，可能在构造完成前泄露对象。

final 不意味着引用指向的对象深度不可变：

```java
final List<String> items = new ArrayList<>();
```

引用不能改，List 内容仍可改。

## 20. 双重检查锁

```java
private static volatile Service instance;

static Service getInstance() {
    Service result = instance;
    if (result == null) {
        synchronized (Service.class) {
            result = instance;
            if (result == null) {
                result = new Service();
                instance = result;
            }
        }
    }
    return result;
}
```

volatile 用于安全发布引用并阻止读线程观察到不正确发布的构造状态。实际项目优先使用枚举、静态初始化、依赖注入容器或显式生命周期，不要为了面试到处手写单例。

## 21. 死锁

JLS 不要求 Java 自动阻止或检测死锁。

常见形成条件：

- 互斥。
- 持有并等待。
- 不可抢占。
- 循环等待。

工程策略：

- 固定锁顺序。
- 避免持锁执行外部调用。
- 减少多锁嵌套。
- 使用带超时的显式锁（必要时）。
- 线程 dump 定位 BLOCKED 和锁拥有者。

## 22. NotifyFlow 设计

### 配置发布

```java
private volatile ProviderConfig currentConfig;

void reload(ProviderConfig next) {
    validate(next);
    currentConfig = next;
}
```

ProviderConfig 应不可变。读线程读取一次引用并在一次发送中使用该快照，避免同一任务前后读取两个版本。

### 统计

成功数、失败数用于指标时可使用 LongAdder；不能把其瞬时 sum 作为强一致业务完成条件。

### 任务状态

数据库任务状态不能靠 JVM synchronized 保护跨实例一致性。需要数据库条件更新、唯一约束、乐观锁或消息协议。

### 单实例内存注册表

构造时一次发布不可变 Map，运行时只读。动态更新时使用 volatile 不可变快照或 ConcurrentHashMap，根据一致性需求选择。

## 23. 常见错误

### 错误一：volatile 等于原子

`volatile++` 仍会丢更新。

### 错误二：sleep 后自然可见

sleep 无同步语义。

### 错误三：锁了写，没锁读

读写协议不一致。

### 错误四：锁不同对象

方法看起来都有 synchronized 块，但使用的 monitor 不同。

### 错误五：锁内远程调用

放大阻塞、死锁和长尾。

### 错误六：if 包围 wait

无法应对虚假唤醒与条件竞争。

### 错误七：用 synchronized 解决分布式问题

只保护单 JVM、同一 monitor 的线程。

### 错误八：把 final 当深度不可变

final 引用的可变对象仍可被修改。

## 24. 本章小结

- JMM 定义多线程程序允许出现的行为。
- data race 来源于未被 happens-before 排序的冲突访问。
- volatile 建立写-读发布关系，但不保护复合操作。
- synchronized 通过 monitor 提供互斥、可见性和复合原子保护。
- sleep/yield 不建立同步；start/join、unlock/lock、volatile write/read 会建立重要关系。
- 并发设计应优先不可变、清晰所有权和高层并发工具。


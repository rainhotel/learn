# 面试追问与表达

## 1. 什么是 JMM？

推荐回答：JMM 是 Java 语言规范定义的内存模型，用于判断多线程执行中每次共享变量读取允许观察哪些写入，并通过 synchronization order 与 happens-before 等关系约束合法行为。

不要只回答“主内存和工作内存”。

## 2. volatile 保证什么？

对同一 volatile 变量的写 synchronizes-with 后续读取，使此前写入通过 happens-before 对读线程可见；它还约束相关重排，但不提供互斥，也不把复合操作变成原子操作。

## 3. volatile 能保证原子性吗？

单次符合规范的 volatile 读写有相应原子/同步语义，但 `count++` 是复合动作，不能笼统回答“volatile 完全不保证原子性”而忽略单次读写，也不能说它能保证递增原子。

## 4. synchronized 的底层是什么？

先回答语言语义：monitor lock/unlock、互斥、可重入、异常自动释放以及 unlock/lock happens-before。字节码 `monitorenter/monitorexit` 和 JVM 锁优化属于后续实现层，不应替代语义回答。

## 5. synchronized 锁升级是什么？

这是 HotSpot 实现与版本相关问题，不是 JMM 规范。回答前注明 JDK 版本；不要背已经变化或移除的偏向锁流程作为永恒结论。

## 6. synchronized 和 volatile 如何选？

- 独立状态发布、单写多读：volatile/不可变快照。
- 复合操作、多字段不变量、互斥：synchronized/Lock。
- 高频计数：Atomic/LongAdder。
- 阻塞协调：高层并发工具。

## 7. happens-before 有哪些规则？

程序顺序、unlock/lock、volatile write/read、start、termination/join、传递性。

## 8. sleep 能保证可见性吗？

不能。JLS 明确说明 sleep/yield 没有同步语义。

## 9. wait 为什么必须在 while 中？

允许虚假唤醒；notify 后重新竞争锁期间条件可能再次变化。返回后必须重新检查条件。

## 10. synchronized 能解决分布式锁问题吗？

不能，只协调同 JVM、同 monitor。多实例需要数据库、Redis、协调服务或业务幂等协议，并理解各自边界。

## 11. 项目中如何使用 volatile？

> NotifyFlow 使用 volatile 引用发布经过完整校验的不可变 ProviderConfig。发送任务一次读取快照，避免逐字段更新造成版本混合。计数不用 volatile++，指标使用 LongAdder，业务完成状态落数据库。

## 12. 三层追问

### 配置发布

1. 为什么普通引用不够？
2. 为什么对象必须不可变？
3. 单次任务多次读取 current 有什么问题？

### 锁

1. 锁对象选 this 有什么风险？
2. 锁内调用 HTTP 会怎样？
3. 缩小锁范围后如何维持业务状态一致？

### 计数

1. AtomicLong 与 LongAdder 如何选？
2. LongAdder.sum 是否强一致快照？
3. 为什么不能用它判断批次完成？


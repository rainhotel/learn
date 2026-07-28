# 跨层系统设计哲学 Mastery Outline

## Dependency Map

```text
Phase 0 系统推理与测量
  ├─> Phase 1 并发、不变量与状态所有权
  ├─> Phase 2 局部性、缓存与副本
  └─> Phase 3 I/O、调度、排队与背压

Phase 1 + Phase 3
  └─> Phase 4 事务、日志与崩溃恢复

Phase 1 + Phase 3 + Phase 4
  └─> Phase 5 网络失败、时间、重试与幂等

Phase 2 + Phase 4 + Phase 5
  └─> Phase 6 复制、分片、一致性与共识

Phase 2 + Phase 3 + Phase 5 + Phase 6
  └─> Phase 7 高可用、过载控制与可观测性

全部阶段
  └─> Phase 8 综合设计与故障演练
```

建议按里程碑而不是日历推进。完整路线约 25–35 个学习单元，每个单元包含阅读、实验、数据和复盘。

## Phase 0: 系统推理、性能模型与测量

### Core Questions

- 正确性条件和性能目标如何分别定义？
- 延迟、吞吐、并发量和利用率有什么关系？
- 为什么平均延迟正常，用户仍会觉得系统很慢？
- 为什么提高并发有时反而降低吞吐？
- 基准测试和压测如何产生误导？

### Concepts

- 不变量、前置条件、后置条件、状态机。
- 安全性、活性、故障模型、故障域。
- 吞吐、服务时间、响应时间、排队时间、并发量。
- P50/P95/P99、长尾和 Fan-out。
- Little's Law、利用率与饱和点。
- 开环与闭环压测、Coordinated Omission。
- RED、USE、Golden Signals。

### Required Reading

- `resource-map.md`：OSTEP/CSAPP 的性能基础。
- The Tail at Scale。
- Google SRE Monitoring Distributed Systems。
- Gil Tene 关于延迟测量的材料。

### Experiments

1. 写一个可调服务时间、并发数和慢请求比例的模拟服务。
2. 分别使用固定并发和固定到达率压测。
3. 对比平均值、P95、P99 和最大值。
4. 注入 1% 慢请求，观察端到端长尾。
5. 绘制吞吐—并发—延迟—拒绝率曲线。

### Exit Criteria

- 能给贯穿项目定义至少 3 个正确性指标和 5 个性能/可靠性指标。
- 能解释 Little's Law 的适用范围。
- 能指出压测工具是否产生 Coordinated Omission。
- 能写出一份明确的负载模型与基准环境说明。

### Common Mistakes

- 只看平均延迟或最高 QPS。
- 把开发机微基准直接外推到生产。
- 先优化再测量。
- 把“没有报错”视为正确。

## Phase 1: 并发、不变量、锁与状态所有权

### Core Questions

- 竞态条件为什么发生？
- 原子性、可见性、有序性和隔离性分别是什么？
- 锁保护的是代码还是不变量？
- 如何通过单所有者或分区减少协调？
- 数据库锁和分布式锁为什么不是线程 Mutex 的放大版？

### Concepts

- 临界区、Mutex、Semaphore、Condition Variable。
- 死锁、活锁、饥饿、优先级反转、Convoy Effect。
- CAS、ABA、内存模型、happens-before、False Sharing。
- 粗粒度锁、分段锁、无锁结构、Actor/单写者。
- 数据库行锁、MVCC、OCC、条件更新和唯一约束。
- 租约、会话、Epoch、Fencing Token。

### Experiments

1. 实现不安全计数器、Mutex、Atomic 和分段计数器并比较。
2. 制造并修复死锁和 False Sharing。
3. 比较全局锁、按 Key 锁、单所有者队列的吞吐与尾延迟。
4. 用数据库行锁、版本号和原子 SQL 分别解决库存 Lost Update。
5. 模拟租约过期后旧持有者继续写入，再使用 Fencing Token 拒绝旧写。

### Exit Criteria

- 能先写不变量，再选择同步机制。
- 能区分互斥、原子性、事务隔离和幂等性。
- 能解释为什么 `SET NX PX` 不自动保证外部资源安全。
- 能为库存扣减给出至少三种正确方案及取舍。

### Common Mistakes

- 锁住代码就等于业务正确。
- Atomic 能让复合业务操作自动原子化。
- 无锁一定更快。
- 超时代表旧锁持有者已停止工作。

## Phase 2: 局部性、缓存与副本

### Core Questions

- 缓存为什么有效，什么时候无效？
- 命中率如何受工作集、容量和访问分布影响？
- 写穿、写回和 Cache Aside 分别转移了什么风险？
- 副本增加后，如何定义陈旧度和失效语义？
- 穿透、击穿和雪崩的共同本质是什么？

### Concepts

- 时间/空间局部性、工作集、Cache Line、TLB、Page Cache。
- Buffer Pool、B+ Tree、LRU/LFU/CLOCK。
- Cache Aside、Read Through、Write Through、Write Back。
- TTL、版本化 Key、主动失效、Stale-While-Revalidate。
- Singleflight、Negative Cache、Bloom Filter。
- 热点 Key、多级缓存、CDN 和回源放大。

### Experiments

1. 比较数组顺序访问与随机访问。
2. 实现 LRU/LFU，在均匀和 Zipf 分布下比较命中率。
3. 实现 Cache Aside，复现数据库与缓存短暂不一致。
4. 并发复现缓存击穿，比较互斥重建、Singleflight、逻辑过期和提前刷新。
5. 模拟缓存全部失效，验证后端退化容量。

### Exit Criteria

- 能用工作集和访问分布解释命中率。
- 能给接口定义允许的最大陈旧时间。
- 能设计缓存失效、重建、降级和全 miss 路径。
- 能解释为什么“先删缓存还是先写数据库”都不是普遍正确答案。

### Common Mistakes

- 缓存只影响性能，不影响正确性。
- TTL 等于一致性协议。
- 命中率高就一定收益高。
- Redis 快，所以无需做热点和容量分析。

## Phase 3: I/O、事件驱动、调度、队列与背压

### Core Questions

- 阻塞、非阻塞、同步和异步如何区分？
- 线程池、事件循环和协程分别适合什么负载？
- 为什么无界队列会转化为长尾和内存耗尽？
- 当生产者长期快于消费者时，系统必须做出什么选择？

### Concepts

- 系统调用、中断、DMA、缓冲、零拷贝、批处理。
- select/poll/epoll、Reactor、Event Loop。
- 线程池、连接池、Work Stealing。
- 有界队列、背压、Admission Control、Load Shedding。
- Head-of-Line Blocking、公平性、优先级和 Deadline。
- Round Robin、Least Outstanding、Power of Two Choices。

### Experiments

1. 实现阻塞线程池服务器和事件循环服务器。
2. 对 CPU 密集、I/O 密集和混合负载分别压测。
3. 把无界队列改成有界队列，比较内存、吞吐、P99 和拒绝率。
4. 实现 reject、block、drop-oldest、shed-low-priority 策略。
5. 改变批大小，观察吞吐与延迟交换。

### Exit Criteria

- 能估算线程池、连接池和队列的上限。
- 能解释为什么过载时尽早拒绝通常优于无限排队。
- 能画出项目的背压传播路径。
- 能区分异步执行和可靠交付。

### Common Mistakes

- 异步天然更快。
- 增加线程或连接一定提高吞吐。
- 消息队列意味着消费者最终一定追得上。
- 队列长度只影响容量，不影响延迟。

## Phase 4: 持久化、事务、日志与崩溃恢复

### Core Questions

- 崩溃发生在写入中间时，如何恢复到可解释状态？
- WAL 为什么必须先于数据页持久化？
- 原子性、隔离性和持久性分别由什么机制实现？
- MVCC 解决了什么，又留下哪些异常？
- 数据库与消息系统之间的双写窗口如何处理？

### Concepts

- Page Cache、fsync、写入顺序、Crash Consistency。
- WAL、Redo、Undo、Checkpoint、LSN。
- Steal/No-Steal、Force/No-Force。
- 2PL、MVCC、OCC 和隔离级别。
- Lost Update、Write Skew、Phantom。
- Transactional Outbox、Inbox、Saga、补偿和对账。

### Experiments

1. 实现简化 Append-Only WAL 与崩溃恢复。
2. 用数据库复现 Lost Update、Write Skew 或幻读。
3. 使用唯一约束和条件更新保护业务不变量。
4. 模拟“数据库提交成功但消息发送失败”。
5. 实现 Outbox 与幂等消费者，并在任意步骤 Kill 进程。

### Exit Criteria

- 能画出事务从内存、WAL 到数据页的路径。
- 能根据不变量选择隔离级别，而不是只背名称。
- 能解释 Exactly Once 的端到端边界。
- 能证明 Outbox 消息可以重复，但业务结果不重复。

### Common Mistakes

- 使用事务就没有并发异常。
- 所有数据库中的隔离级别语义相同。
- WAL 等于业务事件日志。
- 2PC 是所有跨服务一致性的默认答案。

## Phase 5: 网络失败、时间、重试与幂等

### Core Questions

- RPC 超时后，远端操作到底可能处于哪些状态？
- 延迟、丢包、重复、乱序和分区分别破坏什么假设？
- 重试如何从容错变成故障放大器？
- 幂等 Key 的身份、作用域和生命周期如何定义？
- 本地时钟能否用于全局顺序和锁安全？

### Concepts

- TCP 流语义、Timeout、Deadline、Retry。
- Exponential Backoff、Full Jitter、Retry Budget。
- Partial Failure、Failure Detector、Heartbeat。
- Wall Clock、Monotonic Clock、Logical Clock。
- At-Most-Once、At-Least-Once、去重和幂等。
- 租约、Epoch、Generation 和 Fencing。

### Experiments

1. 模拟延迟、丢包、断连和重复请求。
2. 制造“服务端已提交但响应丢失”。
3. 实现幂等表，并处理并发重复请求和请求体冲突。
4. 比较固定重试、指数退避和 Full Jitter。
5. 让调用链每层各自重试，再加入统一 Deadline 和 Retry Budget 比较。

### Exit Criteria

- 能列出一次 RPC 超时后的多种未知结果。
- 能为非天然幂等操作定义安全重复语义。
- 能解释每跳 Timeout 与整体 Deadline 的关系。
- 能解释心跳超时为什么只是怀疑。

### Common Mistakes

- TCP 可靠意味着业务请求不会重复。
- 超时表示请求没有执行。
- 所有错误都应该重试。
- UUID 自动等于幂等。

## Phase 6: 复制、分片、一致性与共识

### Core Questions

- 复制解决什么，又引入什么？
- 主从切换时如何阻止旧 Leader 写入？
- 一致性模型如何写成用户可观察契约？
- 分片如何改变查询、事务、热点和恢复？
- 共识解决什么，又明确不解决什么？

### Concepts

- Leader-Follower、Multi-Leader、Leaderless。
- 同步/异步复制、Quorum、Read Repair。
- 线性一致、顺序一致、因果一致、最终一致。
- Read-Your-Writes、Monotonic Reads。
- Split Brain、Term、Epoch、Leader Election。
- Raft 日志复制与提交。
- Range/Hash Sharding、一致性哈希、再平衡和热点。
- CAP、PACELC、2PC 与共识的区别。

### Experiments

1. 实现简化主从日志复制并注入复制延迟。
2. 观察 Read-After-Write 失败并为接口定义语义。
3. 实现一致性哈希，测量节点增减的数据迁移量。
4. 完成 MIT 6.5840 Raft Lab，或实现教学版 Raft。
5. 模拟网络分区和旧 Leader，用 Term/Epoch 拒绝旧写。

### Exit Criteria

- 能给接口写出客户端可观察的一致性保证。
- 能正确解释 CAP 的操作、失败和一致性定义。
- 能区分复制、故障检测、选主和共识。
- 能识别分片键引发的热点与跨分片操作。

### Common Mistakes

- 最终一致等于“过一会儿肯定正确”。
- CAP 是正常情况下的三选二标签。
- Raft 或 etcd 解决所有分布式状态问题。
- 多数派自动意味着端到端业务正确。

## Phase 7: 高可用、过载控制、故障隔离与可观测性

### Core Questions

- 高可用的对象是进程、接口，还是用户任务？
- 自动重试、扩容和故障转移为什么会形成反馈环？
- 如何限制依赖故障的爆炸半径？
- 什么时候应该拒绝、降级或返回陈旧数据？
- 如何证明 RTO/RPO 和恢复流程真的有效？

### Concepts

- SLI、SLO、Error Budget、RTO、RPO。
- Health Check、Failover、Graceful Degradation。
- Token Bucket、并发限制、Load Shedding。
- Circuit Breaker、Bulkhead、Cell Architecture。
- RED、USE、Trace、结构化日志和关联 ID。
- Capacity Headroom、退化容量、Chaos Engineering、Game Day。

### Experiments

1. 实现 Token Bucket 和并发限制。
2. 为依赖调用增加 Deadline、熔断、隔舱和降级。
3. 注入慢依赖，观察线程池、连接池和队列耗尽。
4. 随机终止实例、暂停消费者、制造缓存全 miss。
5. 恢复备份并实际测量 RTO/RPO。

### Exit Criteria

- 能为项目定义 SLI、SLO、RTO 和 RPO。
- 能说明熔断器的状态机、振荡与半开风险。
- 能设计不依赖故障组件本身的降级路径。
- 能从 Trace 还原一次长尾请求。
- 完成一次有假设、中止条件和复盘的 Game Day。

### Common Mistakes

- 多部署副本就等于高可用。
- 自动故障转移总比人工安全。
- 熔断器能修复依赖。
- 指标越多，可观测性越好。
- Chaos Engineering 等于随机杀进程。

## Phase 8: 综合系统设计与故障演练

### Final Task

针对一个未见过的高并发系统，完成：

- 需求、关键不变量和失败语义。
- 状态所有权与数据流。
- 工作负载和容量估算。
- 缓存、事务、消息、复制和分片选择。
- 超时、重试、幂等和一致性契约。
- 过载、拒绝和降级策略。
- SLO、RTO、RPO。
- 压测方案与故障注入矩阵。
- 正常路径图、失败传播图和恢复路径图。
- 当前不值得引入的复杂度清单。

### Final Exit Criteria

- 所有关键机制都能对应明确需求或故障模型。
- 不使用无定义的“高性能、强一致、最终一致、高可用”。
- 能指出至少五种失败方式及检测和恢复方法。
- 至少一次通过实验数据推翻原始设计判断。
- 能解释为什么没有选择另一种常见方案。

## Phase Completion Definition

每个阶段只有同时满足以下条件才算完成：

- 能用自己的话解释，而不是复述术语。
- 有一个可运行实验。
- 有正确性测试或不变量检查。
- 有性能或可靠性数据。
- 写明成立条件、失效条件和不能保证什么。
- 说明基础系统与 Web 系统的对应关系。
- 将机制加入贯穿项目，或说明为什么当前不应加入。
- 记录至少一个被纠正的原有误解。


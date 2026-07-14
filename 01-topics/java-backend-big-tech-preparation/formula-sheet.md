# Java 后端速查清单

## 线程池

- 任务属性：CPU 密集、I/O 密集、阻塞比例、突发程度。
- 参数：core、max、keepAlive、queue、factory、reject。
- 必查指标：active、pool size、queue depth、completed、reject、task latency。
- 原则：有界队列、业务隔离、明确拒绝、支持超时和关闭。

## MySQL

- 索引设计：选择性、最左前缀、覆盖索引、写放大。
- Explain：type、possible_keys、key、rows、filtered、Extra。
- 事务：边界要小，避免外部调用处于数据库事务内。
- 优化证据：执行计划、扫描行数、P95、锁等待。

## Redis

- 缓存：TTL 抖动、互斥重建、空值/布隆过滤、逻辑过期。
- 锁：唯一 token、原子释放、续期、租约、主从切换边界。
- 风险：热 Key、大 Key、阻塞命令、缓存一致性。

## MQ

- Producer：重试、ACK、幂等、Outbox。
- Broker：副本、持久化、分区/队列。
- Consumer：幂等、Offset、重试、死信、积压。
- 顺序：业务 key 路由到同一分区，并控制并发消费。

## RAG

- 检索质量：Recall@K、MRR、nDCG。
- 生成质量：正确性、忠实度、引用、拒答。
- 性能：索引时间、检索 P95、端到端延迟、Token 成本。
- 调优顺序：数据 -> 切分 -> 检索 -> 重排 -> 上下文 -> Prompt/模型。


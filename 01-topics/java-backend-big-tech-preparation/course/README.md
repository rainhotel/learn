# 《Java 后端与 Agent 工程实战》课程目录

## 使用方式

每章按以下顺序学习：讲义 -> 实验 -> 项目应用 -> 练习 -> 答案 -> 面试 -> 试讲 -> 复盘。

章节完成不等于发布。只有学习者完成作业与试讲并依据反馈修订后，才能进入发布状态。

## 已制作章节

| 编号 | 章节 | 当前状态 | 验证 |
|---|---|---|---|
| 01 | [线程池与异步通知任务](01-thread-pool-async-notification/README.md) | 完整初稿，待学习与试讲 | JDK 21 实验通过 |
| 02 | [HashMap 与集合选型](02-hashmap-collection-selection/README.md) | 完整初稿，待学习与试讲 | JDK 21 实验通过 |
| 03 | [JMM、volatile 与 synchronized](03-jmm-volatile-synchronized/README.md) | 完整初稿，待学习与试讲 | JLS 21 核对，JDK 21 实验通过 |
| 04 | [MySQL 索引、事务与任务表设计](04-mysql-index-transaction-task-table/README.md) | 完整初稿，待学习与试讲 | MySQL 8.0.40 SQL 实验通过 |
| 05 | [Spring 事务、AOP 代理与业务边界](05-spring-transaction-proxy/README.md) | 完整内容初稿，实验待验证 | Spring 7.0.8 官方资料已核对；Maven 执行权限待恢复 |
| 06 | [Redis 缓存、限流与短期幂等](06-redis-cache-rate-limit-idempotency/README.md) | 完整内容初稿，实验待验证 | Redis/Spring Data Redis 官方资料已核对；Docker 实验待运行 |
| 07 | [Kafka 可靠消息、消费恢复与 Transactional Outbox](07-kafka-reliable-messaging-outbox/README.md) | 完整内容初稿，实验待验证 | Kafka 4.3.1、Spring Kafka 4.1.0、RocketMQ 5.0 官方资料已核对；故障实验待运行 |
| 08 | [重试、DLT、对账与故障恢复控制面](08-retry-dlt-reconciliation-recovery/README.md) | 完整内容初稿，待补其余实验 | 重试放大与 Full Jitter Java 21 实验已通过；其余六组 Pending |

## 下一批章节

1. 完成 Spring 事务实验的 RED-GREEN 验证。
2. 完成 Redis 限流、击穿、eviction、fencing 和 Sentinel 实验。
3. 完成 Kafka offset、rebalance、lag、DLT 与 Outbox 故障实验。
4. 实现恢复控制面最小 API、对账任务与安全重放实验。
5. 指标、压测和故障注入。
6. 大模型原理与推理服务。
7. Docker、Kubernetes 与多机分布式。

## 状态定义

- Research：资料研究中。
- Draft：讲义初稿。
- Lab Verified：实验已验证。
- Teach Pending：等待学习者试讲。
- Released：通过学习、作业、试讲和修订，可以发布。

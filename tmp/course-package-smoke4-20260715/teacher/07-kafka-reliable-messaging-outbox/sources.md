# 来源与验证记录

## 使用原则

- 机制、配置默认值和版本能力优先引用官方文档。
- 博客、面经只能用于发现问题，不能单独支撑结论。
- 每条结论区分“Kafka 内部语义”和“端到端业务语义”。
- 当前仅完成文档核验；未运行的实验不得写成已验证事实。

## Apache Kafka 4.3.1

### Quickstart 与 Docker 镜像

- URL：<https://kafka.apache.org/43/getting-started/quickstart/>
- URL：<https://kafka.apache.org/43/getting-started/docker/>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照。
- 支撑结论：当前 4.3 文档 Quickstart 固定最新 patch 为 4.3.1；官方 JVM 镜像为 `apache/kafka:4.3.1`，默认端口示例为 9092。

### Introduction

- URL：<https://kafka.apache.org/43/getting-started/introduction/>
- 访问日期：2026-07-14
- 支撑结论：Topic、Partition、Producer、Consumer、Consumer Group 的基本模型；相同 key 可用于选择语义分区；顺序是分区内顺序。

### Design

- URL：<https://kafka.apache.org/43/design/design/>
- 访问日期：2026-07-14
- 支撑结论：Kafka 默认提供 at-least-once；at-most-once、at-least-once 和 exactly-once 的实现边界；Kafka 事务适合 consume-process-produce 闭环；外部系统需要额外协作或幂等。

### Producer configs

- URL：<https://kafka.apache.org/43/configuration/producer-configs/>
- 访问日期：2026-07-14
- 支撑结论：`acks=all`、`enable.idempotence`、`retries`、`max.in.flight.requests.per.connection`、`transactional.id`、`linger.ms`、`batch.size`、`delivery.timeout.ms` 的语义。
- 关键核验：启用幂等要求 `acks=all`、`retries > 0`、`max.in.flight.requests.per.connection <= 5`；`transactional.id` 启用事务并隐含幂等。

### Consumer configs

- URL：<https://kafka.apache.org/43/configuration/consumer-configs/>
- 访问日期：2026-07-14
- 支撑结论：`enable.auto.commit`、`max.poll.interval.ms`、`session.timeout.ms`、`group.instance.id`、`isolation.level=read_committed`。

### Topic configs

- URL：<https://kafka.apache.org/43/configuration/topic-configs/>
- 访问日期：2026-07-14
- 支撑结论：`min.insync.replicas`、`unclean.leader.election.enable`、`cleanup.policy`、`retention.ms`。

### Monitoring

- URL：<https://kafka.apache.org/43/operations/monitoring/>
- 访问日期：2026-07-14
- 支撑结论：Broker、Partition、ISR、请求延迟和 Consumer lag 的监控方向。
- 本章重点指标：`MessagesInPerSec`、`UnderReplicatedPartitions`、`UnderMinIsrPartitionCount`、ISR shrink/expand、`RequestQueueTimeMs`、`records-lag-max`。

## Spring for Apache Kafka 4.1.0

### Reference

- URL：<https://docs.spring.io/spring-kafka/reference/>
- 访问日期：2026-07-14
- 支撑结论：Spring Kafka 容器、监听器、事务和错误处理能力的版本基线。

### Transactions

- URL：<https://docs.spring.io/spring-kafka/reference/kafka/transactions.html>
- 访问日期：2026-07-14
- 支撑结论：`KafkaTransactionManager` 实现 `PlatformTransactionManager`；多实例 `transactionIdPrefix` 必须唯一；数据库与 Kafka 同步事务有提交顺序，第二个事务提交失败时需要补偿第一个已提交事务。
- 课程解释：这不是 XA 原子提交，因此 NotifyFlow 主线仍采用 Transactional Outbox。

### Error handling

- URL：<https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html>
- 访问日期：2026-07-14
- 支撑结论：`DefaultErrorHandler`、BackOff、`DeadLetterPublishingRecoverer` 的错误恢复模型。

### Non-blocking retry

- URL：<https://docs.spring.io/spring-kafka/reference/retrytopic.html>
- 访问日期：2026-07-14
- 支撑结论：retry topic 的能力和限制；non-blocking retry 不支持 batch listener，不能与 container transactions 组合。

## Apache RocketMQ 5.0 对照资料

### Transaction Message

- URL：<https://rocketmq.apache.org/docs/featureBehavior/04transactionmessage/>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照与文本查找。
- 支撑结论：Broker 先保存不可投递 half message；Producer 执行本地事务后提交 Commit/Rollback；第二次确认缺失或 Unknown 时 Broker 回查 Producer；只保证消息生产与本地事务的最终一致性，不保证下游消费结果。

### Ordered Message

- URL：<https://rocketmq.apache.org/docs/featureBehavior/03fifomessage/>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照与文本查找。
- 支撑结论：FIFO 以 message group 为范围；不同 group 不保证顺序；生产顺序要求单 Producer、串行发送；消费需要 receive-process-reply，批量或异步处理可能打乱顺序；过大的 group 会形成队列热点。

### Delay Message

- URL：<https://rocketmq.apache.org/docs/featureBehavior/02delaymessage/>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照与文本查找。
- 支撑结论：投递时间使用毫秒 Unix 时间戳；默认最大范围 24 小时且不可修改；超范围或早于当前时间会立即投递；默认时间粒度 1000 ms；大量消息同一时刻投递会造成负载与延迟。

## 待补来源

- Kafka 4.3 具体发行说明与兼容矩阵。
- Kafka 官方 Docker 镜像的固定 tag 与部署说明。
- Schema Registry 或替代方案的一手兼容规则。
- Spring Boot 与 Spring Kafka 的正式兼容版本组合。
- NotifyFlow 实验使用的 MySQL Connector、Testcontainers 或 Docker Compose 版本。

## 当前验证结论

| 项目 | 状态 | 证据 |
|---|---|---|
| Kafka 机制与配置文档 | 已核验 | Apache Kafka 4.3 官方文档 |
| Spring Kafka 事务与错误处理 | 已核验 | Spring Kafka 4.1.0 官方文档 |
| RocketMQ 事务/FIFO/延迟边界 | 已核验 | Playwright 官方页面快照 |
| Kafka 实验包静态验证 | 已通过 | `STATIC_CHECKS_PASSED`、Compose config exit 0、六个 PowerShell 脚本解析通过 |
| Kafka Docker 运行态实验 | Pending | Docker Engine 未运行，尚无 Broker/Topic 输出 |
| Spring Kafka 示例 | Pending | 尚无编译和测试结果 |
| MySQL Outbox 故障实验 | Pending | 尚无时间线和数据证据 |

# 来源与验证记录

## Spring for Apache Kafka 4.1.0

### Handling Exceptions

- URL：<https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照和文本查找。
- 支撑结论：
  - `DefaultErrorHandler` 使用 `BackOff` 控制重试和恢复。
  - 可配置固定次数后调用 recoverer；无限重试由特殊 BackOff 表达，但生产系统不应无边界使用。
  - 默认存在 fatal exception 分类，对这类异常跳过重试并在第一次失败调用 recoverer。
  - 可通过 `addNotRetryableExceptions` 或分类映射扩展不可重试异常。
  - `DeadLetterPublishingRecoverer` 默认发送到 `<originalTopic>-dlt` 的相同 Partition，因此 DLT 至少需要与原 Topic 相同的 Partition 数。
  - DLT 记录可以携带原 Topic、Partition、offset、异常类型、异常消息和堆栈等 Header。
  - 可排除异常堆栈 Header，避免 Header 膨胀或敏感信息泄露。

### Non-Blocking Retries

- URL：<https://docs.spring.io/spring-kafka/reference/retrytopic.html>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照和文本查找。
- 支撑结论：
  - non-blocking retry 不支持 batch listener。
  - non-blocking retry 不能与 container transaction 组合。
  - retry topic 是时间和吞吐换空间的恢复路径，会改变原始消费顺序。

### Transactions

- URL：<https://docs.spring.io/spring-kafka/reference/kafka/transactions.html>
- 访问日期：2026-07-14
- 支撑结论：
  - 多实例 `transactionIdPrefix` 必须唯一。
  - 同步事务中第二个事务提交失败时，应用需要补偿已经提交的第一个事务。
  - Spring 事务同步不是 MySQL 与 Kafka 的 XA 原子提交。

## AWS Builders' Library

### Timeouts, retries, and backoff with jitter

- URL：<https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照和文本查找。
- 支撑结论：
  - 所有远程调用都应有明确超时，连接、DNS、TLS 和请求阶段的覆盖范围要核对。
  - 重试会增加下游负载；过载故障中重试可能让恢复更慢。
  - 五层调用链每层重试三次，底层负载可放大到 243 倍。
  - 通常应在调用链的单一层负责重试。
  - 上限指数退避仍需限制总重试次数。
  - 可使用令牌桶限制本地重试速率。
  - 抖动将重试分散到不同时间，避免所有客户端同步重试。
  - 有副作用的 API 需要幂等机制才能安全重试。

## Apache Kafka 4.3.1

- URL：<https://kafka.apache.org/43/configuration/consumer-configs/>
- URL：<https://kafka.apache.org/43/operations/monitoring/>
- 访问日期：2026-07-14
- 支撑结论：Consumer poll、offset、lag 和 Group 运行状态是恢复控制面的底层证据。

## 本章事实边界

- Spring Kafka 提供错误处理和 DLT 机制，不会自动完成业务错误分类、操作权限、对账和人工重放审计。
- DLT 成功写入只表示消息被隔离，不表示业务问题已解决。
- Circuit breaker 和暂停消费会改变系统状态，必须有恢复条件、半开验证和操作手册。
- “补偿”是新的业务动作，不是对已经发生的外部副作用做数据库式回滚。

## 当前验证状态

| 项目 | 状态 | 证据 |
|---|---|---|
| Spring Kafka 错误处理边界 | 已核验 | 4.1.0 官方文档 |
| retry topic 限制 | 已核验 | 4.1.0 官方文档 |
| 重试放大、退避和抖动原则 | 已核验 | AWS Builders' Library |
| NotifyFlow 恢复控制面 | Draft | 设计文档，尚无实现 |
| 多层重试放大实验 | 已通过 | Java 21：243 次对 3 次，负载放大 81 倍 |
| Full Jitter 分布实验 | 已通过 | Java 21：峰值 10000 降到 1044，比例 10.44% |
| Spring Kafka DLT 实验 | Pending | 尚无真实输出 |
| 对账与人工重放实验 | Pending | 尚无真实输出 |

# 分层练习

## 使用方式

- 先独立完成，再查看 `answers.md`。
- 设计题必须画时间线或数据流，不能只写配置名。
- 总分 100，80 分合格；低于 80 分需重做错题并完成口述。

## 一、基础题（20 分）

### 1. Partition 与顺序（4 分）

Kafka 的顺序保证范围是什么？为什么相同 `taskId` 通常应使用相同 key？

### 2. ISR（4 分）

解释 Leader、Follower、ISR 和 `min.insync.replicas` 的关系。

### 3. `acks=all`（4 分）

为什么只配置 `acks=all` 仍不能单独证明消息“绝不丢失”？

### 4. offset（4 分）

offset 表示什么？为什么 offset 已提交不等于业务副作用已完成？

### 5. retention 与 compaction（4 分）

比较 `cleanup.policy=delete` 与 `compact`，说明各自适合什么 Topic。

## 二、机制题（25 分）

### 6. Producer 超时（5 分）

画出“Broker 已写入，但 ACK 丢失”的时间线，并说明无幂等与启用幂等时的差异。

### 7. 两种提交顺序（6 分）

分别画出：

- 业务处理完成后、offset 提交前崩溃。
- offset 提交后、业务处理前崩溃。

指出各自对应的投递语义和风险。

### 8. rebalance（5 分）

Consumer 在 Listener 中执行 90 秒 HTTP 调用，而 `max.poll.interval.ms` 为 60 秒。可能发生什么？列出三个治理办法。

### 9. Kafka exactly-once（5 分）

为什么 Kafka EOS 可以覆盖 consume-process-produce，却不能自动保证短信供应商只收费一次？

### 10. 顺序与异步线程池（4 分）

Kafka 已按 A、B 顺序交付，为什么业务完成顺序仍可能是 B、A？如何修复？

## 三、应用题（30 分）

### 11. Outbox DDL（8 分）

为 NotifyFlow 设计最小 Outbox 表，至少包含：

- 事件唯一标识。
- 聚合标识与分区 key。
- 事件类型和版本。
- payload。
- 发布状态、尝试次数、下次尝试时间。
- 多实例领取租约。
- 查询索引。

说明每个唯一约束和索引解决什么问题。

### 12. Outbox 崩溃窗口（6 分）

Publisher 发送 Kafka 成功，但标记 Outbox 为 `PUBLISHED` 前崩溃。系统如何恢复？为什么会重复？如何保证业务结果不重复？

### 13. 消费幂等（6 分）

设计 `consumed_event` 表和处理事务。说明唯一键冲突时怎么做，offset 在何时确认。

### 14. 外部供应商 Unknown（5 分）

短信请求客户端超时，但供应商可能已接受。为什么不能直接重试？设计一个带 `attemptId`、结果查询和对账的恢复流程。

### 15. retry/DLT（5 分）

将以下异常分类并给出策略：

- HTTP 429。
- 手机号非法。
- eventVersion 不支持。
- 数据库整体不可用。
- 供应商返回 500。

## 四、系统设计题（15 分）

### 16. 先进制造设备命令流（8 分）

系统向 10 万台设备下发命令。同一设备命令必须有序，不同设备可并发。少量大客户拥有 40% 设备。设计：

- Topic 和 key。
- Partition 规划。
- 消费模型。
- 热点治理。
- 幂等和设备回执。
- 故障恢复与 DLT。

### 17. 容量与恢复（7 分）

Producer 峰值 5000 条/秒，Consumer 正常容量 7000 条/秒。Consumer 停机 20 分钟：

1. 理论积压多少？
2. 恢复后净消化速率多少？
3. 理论多久追平？
4. 哪些现实因素会使结果更差？

## 五、反例与表达题（10 分）

### 18. 纠正简历表述（5 分）

修改下面的表述，使其技术边界准确：

> 使用 Kafka 事务和 ACK 机制彻底保证消息绝不丢失、绝不重复，实现端到端 exactly-once。

### 19. 选型（5 分）

以下场景分别优先考虑 Kafka、RocketMQ 或数据库任务表，并说明原因：

1. 小团队、每天 5 万条定时通知、已有 MySQL、无专职运维。
2. Agent 平台需要保存工具调用事件、支持回放和流式分析。
3. Java 制造业务大量使用事务消息、同设备 FIFO 和 30 分钟内延迟消息，团队已有 RocketMQ 经验。

## 必做 Teach-back

不看讲义，用 5 分钟解释：

> 为什么 NotifyFlow 选择“task + outbox 同事务、至少一次发布、消费幂等”，而不是宣称数据库和 Kafka exactly-once？


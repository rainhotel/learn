# Teach-back 讲解稿

## 5 分钟版本

### 0:00-0:40 真实问题

NotifyFlow 创建任务时既要写 MySQL，又要通知 Worker。如果直接“提交数据库后发送 Kafka”，数据库成功、Kafka 失败会让任务永远没有消费者；如果先发 Kafka，消费者可能看到一个最终回滚的任务。这是双写问题。

### 0:40-1:40 Outbox

解决办法是在一个 MySQL 本地事务里同时写 `notification_task` 和 `event_outbox`。这样 API 成功时，任务和“需要发布的事件事实”一定同时存在。独立 Publisher 扫描 Outbox 并发送 Kafka。

必须强调：Outbox 不是 exactly-once。Publisher 可能已经发送成功，但在标记 `PUBLISHED` 前崩溃，重启后会再次发送。

### 1:40-2:40 至少一次与消费幂等

因此系统接受至少一次：消息可以重复，但业务效果必须收敛。Consumer 在数据库事务中插入 `(consumerName, eventId)` 唯一键，并用任务状态机做条件更新。若调用供应商，还需要稳定的 attemptId、供应商幂等 key、结果查询和对账。

### 2:40-3:30 Kafka 可靠配置

Producer 使用 `acks=all` 和幂等重试；Topic 配置副本与最小 ISR，例如 RF=3、min ISR=2。ISR 不足时写入失败，这是用一部分可用性换耐久性。顺序只在 Partition 内，因此同一 task 使用同 key。

### 3:30-4:20 offset 与失败窗口

业务完成后、offset 提交前崩溃会重复；offset 先提交、业务前崩溃会丢处理。NotifyFlow 选择前者，再用幂等处理重复。Kafka 的 exactly-once 主要覆盖 Kafka 输入到 Kafka 输出，不能自动把 MySQL 和短信供应商纳入事务。

### 4:20-5:00 恢复闭环

瞬时错误有界退避，永久错误直接失败，Schema 毒消息进入 DLT。DLT 必须有负责人、告警、修复、限速重放和审计。最终用 lag、Outbox 最老待发布时间、重复事件数、DLT 率和供应商 Unknown 结果验证系统。

## 15 分钟版本

### 第一部分：定义问题（2 分钟）

画出：

```text
API -> MySQL task
API -> Kafka event
Kafka -> Consumer -> Provider
```

指出四种不同可靠性：Producer 写入、Broker 保存、Consumer 进度、业务副作用。解释不能用一个“消息不丢”概括全部。

### 第二部分：Kafka 核心机制（3 分钟）

画出一个 Topic 的三个 Partition，每个 Partition 有 Leader、Follower 和 ISR。说明：

- key 决定 Partition 和业务顺序范围。
- `acks=all + RF=3 + min ISR=2` 的含义。
- ISR 不足时写失败。
- Producer 超时不代表 Broker 未写入。
- 幂等 Producer 只去除协议重试重复。

### 第三部分：Consumer 与 offset（3 分钟）

画两条崩溃时间线：

```text
处理 -> crash -> 未提交 offset -> 重复
提交 offset -> crash -> 未处理 -> 丢失
```

说明为什么选择 at-least-once，以及 rebalance、慢 poll 和异步线程池如何放大重复与乱序。

### 第四部分：Outbox 与业务幂等（4 分钟）

画出 task + outbox 同事务，Publisher 至少一次发布，Consumer 的 consumed_event 和状态机。

必须讲清：

- Outbox 解决双写丢失。
- 发布成功、标记失败会重复。
- 外部供应商不能放在长数据库事务里。
- attemptId、结果查询、Unknown 状态和对账如何恢复。

### 第五部分：重试、DLT 和监控（2 分钟）

用表格区分瞬时、永久、毒消息和系统性故障。说明 retry topic 会改变顺序，DLT 需要人工闭环。列出：

- Consumer lag。
- UnderMinISR。
- Outbox oldest pending。
- retry/DLT rate。
- duplicate event。

### 第六部分：选型与边界（1 分钟）

Kafka 作为 Agent 平台、事件流和回放主线；RocketMQ 作为中国 Java/制造业的事务、FIFO 和延迟消息对照；小规模系统可能优先数据库任务表。最后强调当前项目是独立工程化重构，不虚构为实习线上架构。

## 必须画出的三张图

### 图一：副本确认

```text
Producer -> Leader
             |-> Follower A (ISR)
             |-> Follower B (ISR)
          acks=all + min ISR
```

### 图二：offset 崩溃窗口

```text
at-least-once: process -> DB commit -> crash -> offset commit
at-most-once : offset commit -> crash -> process
```

### 图三：Outbox 恢复链

```text
MySQL TX(task + outbox)
 -> Publisher
 -> Kafka
 -> idempotent Consumer
 -> Provider attempt/query
 -> retry/DLT/replay
```

## 最容易被追问的点

1. `acks=all` 为什么还需要最小 ISR？
2. Producer 幂等与业务幂等差别是什么？
3. Outbox 为什么还会重复？
4. 外部 HTTP 超时后为什么不能直接重试？
5. Kafka EOS 的边界在哪里？
6. retry topic 如何破坏顺序？
7. DLT 重放如何避免二次副作用？
8. Partition 数量如何估算？

## 自测标准

- 5 分钟版本控制在 4:30-5:30。
- 15 分钟版本控制在 13:30-16:30。
- 不看讲义能画三张图。
- 每个“保证”后都能补充范围和失败窗口。
- 能回答一个反例：为什么小规模 NotifyFlow 可能暂时不需要 Kafka。


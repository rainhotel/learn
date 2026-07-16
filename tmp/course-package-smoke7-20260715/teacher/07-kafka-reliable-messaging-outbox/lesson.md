# Kafka 可靠消息、消费恢复与 Transactional Outbox

## 课程信息

- 所属模块：可靠通知主链路
- 难度：深入
- 建议时长：22-30 小时
- 先修章节：MySQL 事务、Spring 事务、Redis 幂等
- 对应项目里程碑：NotifyFlow 从数据库任务表升级为可恢复的事件驱动通知链路
- 对应岗位能力：Java 消息中间件、分布式一致性、故障恢复、系统设计、生产排障

## 学习目标

完成本章后，学习者能够：

1. 解释 Kafka 的分区、副本、ISR、Producer、Consumer Group 和 offset。
2. 从配置与失败时间线推导重复、丢失、乱序和积压的来源。
3. 说明 Producer 幂等、Kafka 事务、消费幂等和端到端 exactly-once 的不同边界。
4. 为 NotifyFlow 设计 Transactional Outbox、至少一次发布、消费幂等、重试、DLT 和人工重放。
5. 用指标、日志和实验验证可靠性，而不是只背配置名。
6. 根据场景比较 Kafka、RocketMQ 和数据库任务表。

## 为什么要学

消息队列最容易制造“架构看起来高级，故障时无法解释”的系统。真实生产事故通常不是因为不知道如何调用 `send()`，而是没有回答以下问题：

- Broker 返回成功前，消息究竟复制到哪里？
- Producer 超时后重试，第一次写入到底成功没有？
- Consumer 已经调用短信供应商，但 offset 尚未提交，此时进程崩溃会怎样？
- 一条永远无法反序列化的消息，会不会阻塞整个分区？
- task 已写 MySQL，但 Kafka 发送失败，如何恢复？
- DLT 中的消息由谁负责，修复后如何避免二次副作用？

大厂面试中的“如何保证消息不丢”“如何保证顺序”“如何实现 exactly-once”，都必须落到范围、失败窗口和证据上。

## 问题场景

NotifyFlow 接收企业通知请求：

```text
POST /tasks
  -> 写入通知任务
  -> 发送 task-created 事件
  -> Worker 调用短信/邮件/企业微信供应商
  -> 写回结果并发出 task-completed 或 task-failed
```

约束：

- API 成功后任务不能静默消失。
- 同一任务允许消息重复，但不允许产生无法解释的重复收费。
- 单个租户或供应商故障不能拖垮全部消费者。
- 需要保留失败原因、重试次数和人工重放记录。
- 同一任务的状态变化必须按合法状态机推进。
- 系统扩容、重启和 rebalance 时仍可恢复。

## 一、先定义“可靠”的范围

### 1.1 四个不同问题

| 问题 | 典型负责方 | 不能混淆的点 |
|---|---|---|
| 消息是否写入 Broker | Producer + Kafka | 成功 ACK 的条件由 `acks` 和 ISR 决定 |
| Broker 是否保留消息 | Kafka 副本与保留策略 | 副本不是备份，retention 到期仍会删除 |
| Consumer 是否处理消息 | Consumer Group + offset | offset 只记录消费进度，不代表业务副作用一定完成 |
| 业务结果是否唯一 | 数据库/供应商/业务协议 | Kafka Producer 幂等不能阻止短信发两次 |

### 1.2 可靠性不是一个开关

必须明确：

- 允许丢失吗？允许重复吗？允许延迟多久？
- 顺序范围是全局、租户、任务，还是同一设备？
- 故障恢复目标 RTO 是多少？可接受的数据缺口 RPO 是多少？
- 下游是否支持幂等 key 或结果查询？
- 人工介入的 SLA 是多少？

没有这些边界，“保证不丢”只是不可验证的口号。

## 二、Kafka 数据模型

### 2.1 Topic 与 Partition

Topic 是事件类别，Partition 是实际有序日志。Producer 把记录追加到某个 Partition，Consumer 按 offset 读取。

```text
topic: notify-task-events

partition-0: [offset 0][1][2][3]...
partition-1: [offset 0][1][2]...
partition-2: [offset 0][1][2][3][4]...
```

Kafka 只保证单个 Partition 内的记录顺序，不提供跨 Partition 的全局顺序。

### 2.2 key 决定业务顺序范围

NotifyFlow 推荐以 `taskId` 作为任务状态事件的 key：

```text
key = taskId
value = TaskCreated / TaskClaimed / TaskSucceeded / TaskFailed
```

同一 task 的事件稳定进入同一 Partition，便于维护状态顺序。若用 `tenantId`：

- 优点：同一租户内可形成顺序。
- 风险：大租户成为热点 key，限制并行度。

key 是业务与吞吐的共同设计，不是随便填一个字符串。

### 2.3 Leader、Follower 与 ISR

每个 Partition 有一个 Leader 和若干副本。读写通常由 Leader 负责，Follower 复制日志。ISR 是当前与 Leader 保持足够同步的副本集合。

常见耐久组合：

```text
replication.factor = 3
min.insync.replicas = 2
producer acks = all
```

含义不是“三副本永不丢”：

- `acks=all` 等待当前 ISR 满足确认条件。
- ISR 少于 `min.insync.replicas` 时，Producer 写入失败，以降低可用性保护耐久性。
- 若允许 unclean leader election，可能用数据丢失换取恢复可用性。
- 多副本仍可能受机房、磁盘、误操作和保留策略影响。

## 三、Producer：成功、超时与重复

### 3.1 `acks` 的含义

- `acks=0`：不等待 Broker 确认，延迟低但无法可靠感知写入失败。
- `acks=1`：Leader 写入后确认；Leader 在 Follower 复制前故障可能丢失已确认记录。
- `acks=all`：等待 ISR 条件满足，耐久性更强，但延迟与失败概率受副本健康影响。

选择 `acks=all` 后，还必须结合 `min.insync.replicas`，否则“all”只表示当前 ISR，而当前 ISR 可能只剩 Leader。

### 3.2 batching 与吞吐

Producer 会批量发送同一 Partition 的记录：

- `batch.size` 控制批次容量上限。
- `linger.ms` 允许等待短时间聚合更多记录。
- 压缩降低网络和存储成本，但增加 CPU。

调参不能只看 QPS，还要观察：

- P95/P99 发送延迟。
- batch 利用率。
- 压缩比和 CPU。
- 请求队列时间。
- 超时与重试次数。

### 3.3 超时的不确定性

Producer 超时只说明“客户端在期限内没有拿到确定结果”，不等于 Broker 一定没写入。

```text
Producer -> Broker 写入成功
Broker   -> ACK 在网络中丢失
Producer -> 超时重试
```

若没有幂等生产者，重试可能在 Kafka 日志中形成重复记录。

### 3.4 幂等生产者

Kafka 幂等生产者使用 Producer 身份与序列号，避免同一 Producer 会话的重试在日志中产生重复。官方配置关系包括：

- `acks=all`
- `retries > 0`
- `max.in.flight.requests.per.connection <= 5`

边界：

- 它解决 Producer 到 Kafka 日志的重试重复。
- 它不识别两个独立业务请求是否属于同一个任务。
- 它不阻止 Consumer 重复调用外部供应商。
- Producer 重建、业务重放或 Outbox 重复发布仍需要事件 ID 和消费幂等。

### 3.5 Producer 事务

配置 `transactional.id` 后可在一个 Kafka 事务中原子写入多个 Partition，并与消费 offset 组合形成 consume-process-produce 闭环。

消费者使用 `isolation.level=read_committed` 时，只读取已提交事务记录。

不要扩大结论：Kafka 事务不能自动把 MySQL、Redis、HTTP 短信供应商纳入同一原子事务。

## 四、Consumer Group、offset 与 rebalance

### 4.1 Consumer Group

同一 Group 中，一个 Partition 同时分配给一个 Consumer。Consumer 数量超过 Partition 数量时，多出的 Consumer 空闲。

```text
3 partitions + 2 consumers -> 一个消费者处理两个分区
3 partitions + 3 consumers -> 每个消费者一个分区
3 partitions + 5 consumers -> 两个消费者空闲
```

因此扩容上限受 Partition 数量约束。

### 4.2 offset 是“下一次从哪里读”

offset 提交不是业务事务提交。需要分别记录：

- Kafka 已投递到什么位置。
- 应用完成了什么业务副作用。
- 数据库状态是否成功提交。
- 外部供应商是否实际接受请求。

### 4.3 自动提交的风险

`enable.auto.commit=true` 会周期性后台提交进度。若业务处理时间较长，提交进度可能领先于真正完成的副作用；进程崩溃后，消息可能不会重新处理。

可靠业务通常使用明确的容器 ACK/手动提交策略，并用实验验证提交发生在何处。

### 4.4 两条崩溃时间线

#### at-least-once：先处理，再提交 offset

```text
读取消息
-> 调用供应商成功
-> 进程崩溃
-> offset 未提交
-> 重启后再次读取
-> 供应商可能被再次调用
```

结果：不易丢处理，但可能重复，所以必须业务幂等。

#### at-most-once：先提交 offset，再处理

```text
读取消息
-> 提交 offset
-> 进程崩溃
-> 业务尚未执行
-> 重启后从下一条开始
```

结果：不重复，但可能永久丢失业务处理。

### 4.5 poll、心跳与 rebalance

- `session.timeout.ms`：Consumer 心跳故障检测窗口。
- `max.poll.interval.ms`：两次 `poll()` 的最大允许间隔；处理过慢可能被认为失效。
- `group.instance.id`：提供 static membership，可减少短暂重启导致的分区重新分配。

常见事故：Listener 内执行慢 HTTP 调用，超过 `max.poll.interval.ms`，Group 触发 rebalance；原 Consumer 仍在执行，下一个 Consumer 又拿到分区，重复窗口被放大。

治理方向：

- 限制单批记录数与单条处理时间。
- 把慢调用拆成明确任务状态，不无限阻塞 poll 线程。
- 设置超时、并发隔离和暂停策略。
- 监控 rebalance 次数、处理耗时和 lag。

## 五、顺序的真实边界

### 5.1 分区内顺序不等于处理完成顺序

Kafka 按顺序交付记录，但应用若把记录提交到异步线程池并发处理，完成顺序仍可能变化。

```text
Kafka: A -> B
线程池: A 调用耗时 3 秒，B 耗时 100 ms
结果: B 先完成
```

若业务要求同一 task 状态顺序：

- 同一 task 使用相同 key。
- 同一 Partition 内避免无约束异步并发。
- 数据库状态机使用条件更新兜底。
- 每个事件携带版本号或 sequence。

### 5.2 不追求不必要的全局顺序

全局单 Partition 可以提供全局顺序，但会牺牲吞吐和扩展性。NotifyFlow 通常只需要：

- 同一任务内顺序。
- 同一设备命令流顺序。
- 同一订单或工作流实例内顺序。

不同任务之间不需要互相阻塞。

## 六、投递语义与 exactly-once

### 6.1 三种语义

| 语义 | 典型顺序 | 主要风险 |
|---|---|---|
| at-most-once | 先提交进度，再处理 | 处理丢失 |
| at-least-once | 先处理，再提交进度 | 重复处理 |
| exactly-once | 原子协调处理结果和进度 | 只在明确系统边界内成立 |

Kafka 默认更接近 at-least-once。

### 6.2 Kafka EOS 的适用边界

Kafka 事务很适合：

```text
消费 Kafka A
-> 处理
-> 生产 Kafka B
-> 同事务提交 A 的 offset
```

但下面不是 Kafka 单独能解决的：

```text
消费 Kafka
-> INSERT MySQL
-> 调用短信供应商
-> 提交 Kafka offset
```

要实现业务可证明的“一次效果”，需要组合：

- 数据库唯一约束。
- 幂等消费表或业务状态机。
- 供应商幂等 key。
- 供应商结果查询与对账。
- Transactional Outbox/Inbox。
- 补偿和人工处置。

更准确的面试表达是：“消息链路采用至少一次，业务效果通过幂等与状态机收敛为一次可见结果。”

## 七、双写问题与 Transactional Outbox

### 7.1 错误做法：数据库和 Kafka 直接双写

```text
BEGIN
INSERT task
COMMIT
send Kafka
```

如果数据库提交后进程崩溃，任务存在但事件缺失。

反过来先发 Kafka 再提交数据库，可能出现消费者收到事件，但 task 不存在。

### 7.2 Outbox 核心模型

在同一个 MySQL 本地事务中写入业务表和 Outbox：

```sql
BEGIN;

INSERT INTO notification_task (...);

INSERT INTO event_outbox (
  event_id, aggregate_type, aggregate_id,
  event_type, event_version, payload,
  status, next_attempt_at, created_at
) VALUES (..., 'PENDING', NOW(), NOW());

COMMIT;
```

Publisher 独立扫描或通过 CDC 发布 Outbox，再标记发布状态。

### 7.3 Outbox 仍是至少一次

关键失败窗口：

```text
Publisher 发送 Kafka 成功
-> 在标记 outbox=PUBLISHED 前崩溃
-> 重启后再次发送
```

所以 Outbox 消除的是“业务状态与待发布事实之间的丢失”，并不消除重复发布。Consumer 仍需用 `eventId` 或业务唯一键幂等。

### 7.4 轮询 Outbox 与 CDC

| 方案 | 优点 | 缺点 | 适用阶段 |
|---|---|---|---|
| 数据库轮询 | 简单、可控、易教学 | 扫描压力、延迟、并发领取设计 | NotifyFlow 初版 |
| CDC | 低侵入、延迟较低、吞吐高 | 基础设施复杂、Schema 与运维要求高 | 规模化事件平台 |
| Broker 事务消息 | 部分产品原生协调 | 绑定中间件、仍不保证下游副作用 | 已深度采用对应 MQ |

初版优先数据库轮询，并通过索引、批量领取、租约和重试控制数据库压力。

## 八、消费幂等与状态机

### 8.1 幂等消费表

```sql
CREATE TABLE consumed_event (
  consumer_name VARCHAR(100) NOT NULL,
  event_id      VARCHAR(64)  NOT NULL,
  processed_at  DATETIME(6)  NOT NULL,
  PRIMARY KEY (consumer_name, event_id)
);
```

在业务数据库事务中：

1. 尝试插入 `(consumer_name, event_id)`。
2. 唯一键冲突表示已处理，安全跳过。
3. 插入成功后执行业务状态更新。
4. 事务提交后再确认 Kafka 消费进度。

若副作用是外部 HTTP，不能长时间持有数据库事务。应把“准备调用、调用中、结果待确认、成功/失败”设计为显式状态，并使用供应商幂等键与查询接口。

### 8.2 条件状态更新

```sql
UPDATE notification_task
SET status = 'SENDING', version = version + 1
WHERE id = ?
  AND status = 'PENDING'
  AND version = ?;
```

受影响行数为 0 时，说明事件过期、重复或状态已变化。不要直接覆盖为新状态。

### 8.3 幂等键选择

- 事件处理：`consumerName + eventId`。
- 创建任务：`tenantId + requestId`。
- 供应商调用：稳定的 `deliveryAttemptId` 或供应商支持的幂等 token。
- 状态推进：`taskId + expectedVersion`。

一个 key 不应承担所有层次的幂等。

## 九、重试、毒消息、DLT 与人工重放

### 9.1 先分类，再重试

| 错误 | 示例 | 策略 |
|---|---|---|
| 瞬时错误 | 网络抖动、429、短暂 5xx | 指数退避 + 抖动 + 有上限重试 |
| 永久业务错误 | 手机号非法、模板已禁用 | 不重试，记录明确失败 |
| 数据兼容错误 | Schema 不兼容、反序列化失败 | 隔离到 DLT，修复后重放 |
| 系统性故障 | 供应商全站故障、数据库不可用 | 熔断/暂停消费/降速，避免重试风暴 |

### 9.2 Blocking retry 与 retry topic

- Blocking retry：当前消费线程等待并重试，简单但占用分区处理能力。
- Retry topic：把失败消息发送到延迟级别不同的 Topic，原分区可继续推进，但顺序语义被改变，系统复杂度更高。

Spring Kafka 的 non-blocking retry 有明确限制：不支持 batch listener，也不能与 container transactions 组合。设计前必须核对当前版本文档。

### 9.3 DLT 不是终点

DLT 必须配套：

- 原 Topic、Partition、offset。
- eventId、key、Schema 版本。
- 异常类型与堆栈摘要。
- 首次失败时间、最后失败时间、重试次数。
- 业务实体和租户。
- 处置人、修复说明、重放批次和最终结果。

人工重放流程：

```text
筛选 DLT
-> 判断是否永久错误
-> 修复数据/代码/配置
-> 小批量 dry-run 或影子验证
-> 使用原 eventId 或建立 replayId
-> 限速重放
-> 观察幂等冲突、错误率和 lag
-> 记录审计结果
```

盲目把 DLT 全量重新发送，可能再次压垮下游。

## 十、Schema 与版本演进

事件是跨服务契约，不能只保存一个随意 JSON。

推荐事件信封：

```json
{
  "eventId": "01J...",
  "eventType": "notify.task.created",
  "eventVersion": 1,
  "occurredAt": "2026-07-14T10:00:00Z",
  "producer": "notify-api",
  "tenantId": "t-1001",
  "aggregateId": "task-9001",
  "traceId": "trace-...",
  "payload": {}
}
```

演进原则：

- 新增可选字段通常比删除/改名安全。
- Consumer 对未知字段保持容忍。
- 必填语义变化应升级事件版本。
- 不把数据库内部实体直接序列化为公共事件。
- PII 和密钥不能无审查进入消息体。
- 记录 Schema 兼容检查和回滚策略。

## 十一、保留、压缩与回放

### 11.1 `retention.ms`

消息超过保留时间可被删除。它也是恢复 SLA 边界：若 Consumer 停机时间超过 retention，不能保证仍能从 Kafka 回放全部消息。

### 11.2 `cleanup.policy`

- `delete`：按时间或大小删除旧日志段。
- `compact`：按 key 保留较新的值，适合状态主题。
- `delete,compact`：组合策略。

Compaction 不等于只保留一条，也不是立即完成。事件审计 Topic 和状态快照 Topic 应分别设计。

## 十二、监控、容量与故障判断

### 12.1 Broker 和副本

- `UnderReplicatedPartitions`
- `UnderMinIsrPartitionCount`
- ISR shrink/expand
- 请求错误率
- `RequestQueueTimeMs`
- 磁盘利用率与磁盘延迟

### 12.2 Producer

- 发送速率和字节速率
- batch 大小与压缩率
- record error/retry
- request latency
- buffer 等待和超时

### 12.3 Consumer

- `records-lag-max`
- 当前 offset 与 log end offset
- 消费速率、处理成功率
- 单条/P95/P99 处理耗时
- rebalance 次数和持续时间
- retry/DLT 速率

### 12.4 lag 不是单一结论

lag 上升可能来自：

- Producer 突发流量。
- Consumer 实例不足。
- 热 Partition。
- 下游 HTTP 或数据库变慢。
- poison message 阻塞。
- rebalance 频繁发生。
- GC、CPU 或磁盘问题。

排障顺序应从“是否全局、是否单 Partition、是否伴随错误、处理耗时在哪里增加”逐层缩小。

## 十三、Kafka、RocketMQ 与数据库任务表

| 维度 | Kafka | RocketMQ | 数据库任务表 |
|---|---|---|---|
| 强项 | 事件流、日志、生态、回放、Agent/数据链路 | Java 业务消息、事务/FIFO/延迟能力集中 | 简单、强事务、低基础设施成本 |
| 顺序范围 | Partition | Message group | SQL 领取与业务状态控制 |
| 延迟任务 | 通常需 retry topic/调度设计 | 原生延迟消息，5.0 文档默认最大 24h | `next_run_at` 索引扫描灵活 |
| 本地事务协调 | Outbox 或 Kafka 事务边界内 | 事务半消息与回查 | 同库本地事务天然直接 |
| 回放与流处理 | 强 | 业务消息能力突出 | 需自行实现历史与扫描 |
| 运维复杂度 | 高 | 高 | 初期较低，规模后压力明显 |

### RocketMQ 官方边界

- 事务消息：Broker 保存 half message，本地事务后 Commit/Rollback；状态未知时回查 Producer。它只保证消息生产与本地事务最终一致，不保证下游消费成功。
- FIFO：只在 message group 内成立；单 Producer 串行发送和 receive-process-reply 才能维护生产与消费顺序；过大的 group 会成为热点。
- 延迟消息：使用投递时间戳；官方 5.0 文档默认最大 24 小时，超范围或早于当前时间会立即投递；默认粒度 1000 ms。

### NotifyFlow 选择

主线选 Kafka，因为目标岗位同时覆盖 Agent 平台、事件流、日志、数据链路和 Java 后端。RocketMQ 保留为中国 Java/制造业岗位的重点对照。若团队规模小、任务量有限，MySQL 任务表可能是更负责任的第一版。

## 十四、常见错误

### 错误一：`acks=all` 就绝不会丢

- 表现：忽略副本数、最小 ISR、unclean election 和 retention。
- 修复：写出故障矩阵，并验证 ISR 不足时 Producer 是否失败。

### 错误二：Kafka 幂等生产者等于业务幂等

- 表现：Consumer 重复调用短信供应商并重复收费。
- 修复：事件 ID、数据库唯一约束、状态机、供应商幂等 key 与对账共同兜底。

### 错误三：自动提交 offset，同时执行慢业务

- 表现：进程崩溃后业务未完成，但消息不再出现。
- 修复：显式确认策略，确保业务提交和 offset 顺序可解释。

### 错误四：无限重试

- 表现：一个永久错误占满线程、阻塞分区、形成重试风暴。
- 修复：错误分类、有上限退避、DLT、熔断和人工处置。

### 错误五：为了顺序把所有消息放一个 Partition

- 表现：吞吐无法扩展，单点热点。
- 修复：按 task/order/device 等最小业务实体分区。

### 错误六：Outbox 发布成功就直接删除

- 表现：无法审计、难以判断发布状态和重放历史。
- 修复：状态化保存、归档策略、事件 ID、发布时间和尝试记录。

### 错误七：DLT 等于问题已解决

- 表现：DLT 长期堆积，无负责人、无重放流程。
- 修复：告警、所有权、SLA、修复、限速重放和审计闭环。

## 十五、面试回答框架

面对“如何保证消息不丢”，按四段回答：

1. 先界定 Producer、Broker、Consumer 还是业务副作用。
2. Producer 使用 `acks=all`、幂等重试；Broker 配置副本和最小 ISR，并监控副本健康。
3. Consumer 采用至少一次，业务事务成功后提交 offset，重复由唯一约束、状态机和供应商幂等处理。
4. 数据库与 Kafka 双写使用 Transactional Outbox，配套重试、DLT、人工重放、对账和故障实验。

不要只说“开启事务”或“设置 ACK”。

## 十六、章节作业

- 作业目标：设计并验证 NotifyFlow 可靠事件链路。
- 提交物：架构图、事件 Schema、DDL、配置、八组故障时间线、指标截图、事故复盘和 15 分钟讲解。
- 验收标准：每个可靠性结论都有对应失败路径和证据；不使用无边界的 exactly-once 表述。
- 加分项：完成 Outbox 轮询和 CDC 对比；实现 DLT 管理接口；形成容量模型。

## 本章小结

- Kafka 的顺序是 Partition 内顺序，业务 key 决定顺序范围和热点。
- `acks=all` 必须与副本和 `min.insync.replicas` 一起解释。
- Producer 幂等只处理写 Kafka 的重试重复，不负责外部副作用。
- offset 提交位置决定丢失或重复窗口；可靠业务通常选择至少一次并实现幂等。
- Kafka exactly-once 主要适用于 Kafka 闭环，不能直接覆盖 MySQL 和供应商。
- Transactional Outbox 解决数据库与待发布事件的双写丢失，但仍会重复发布。
- 重试、DLT 和人工重放必须形成带指标、所有权和审计的恢复系统。

## 版本记录

- v0.1，2026-07-14：完成基于 Kafka 4.3、Spring Kafka 4.1.0 和 RocketMQ 5.0 官方资料的完整初稿；实验均为 Pending。


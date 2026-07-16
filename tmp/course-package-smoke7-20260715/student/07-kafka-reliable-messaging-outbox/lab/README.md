# 第 7 章实验：Kafka 可靠性与 Outbox 故障时间线

## 当前状态

- 状态：基础实验包静态验证通过，Kafka 运行态 Pending
- 已完成：实验问题、Compose、Kafka 配置、PowerShell 运行脚本、两个基础实验脚本和静态 RED/GREEN 证据
- 未完成：Docker Engine 启动、Kafka 容器运行、Java/Spring Kafka 代码、真实 Kafka 日志、指标截图和结论修订

本文件中的所有“预期”都不是已运行结果。

## 实验环境基线

- Java 21
- Apache Kafka 4.3.1
- Spring for Apache Kafka 4.1.0
- MySQL 8.x
- Docker 28.3.0
- Windows PowerShell，Maven 使用 `mvn.cmd`

Kafka 官方镜像已固定为 `apache/kafka:4.3.1`。Spring Boot 与 Spring Kafka 兼容矩阵仍需在 Java 实验开始前固定。

## 已落盘的可运行文件

- `compose.yaml`：单节点 KRaft Kafka 4.3.1 基础环境。
- `config/producer.properties`：`acks=all`、幂等生产和超时基线。
- `config/consumer.properties`：关闭自动提交、`read_committed` 基线。
- `scripts/verify-lab.ps1`：静态与运行态验收。
- `scripts/start-lab.ps1`：拉取镜像、启动 Broker 并按条件等待就绪。
- `scripts/stop-lab.ps1`：停止环境，默认保留数据卷。
- `scripts/kafka-command.ps1`：统一调用容器内 Kafka CLI。
- `experiments/01-partition-order.ps1`：相同 key 分区内顺序断言。
- `experiments/02-offset-and-lag.ps1`：构造 committed offset、lag 和恢复后的 lag=0。

静态验证证据见 `evidence/static-verification-2026-07-14.md`。

## 当前运行阻塞

Docker Engine 当前未运行，自动启动服务的权限审批未通过。因此：

- `STATIC_CHECKS_PASSED` 已获得真实输出。
- Compose 解析和六个 PowerShell 脚本语法检查已通过。
- Kafka 运行态、顺序和 lag 实验仍为 Pending。

启动 Docker Desktop 后，从 `lab/` 目录依次运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-lab.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-lab.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\experiments\01-partition-order.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\experiments\02-offset-and-lag.ps1
```

## 证据目录约定

每次实验应保存：

```text
lab/evidence/<experiment-id>/
  environment.md
  commands.md
  producer.log
  consumer.log
  broker.log
  metrics.md
  database.sql
  timeline.md
  conclusion.md
```

不要只保存“成功截图”；失败日志和时间线更重要。

## 实验 1：分区内顺序与跨分区无全局顺序

### 假设

相同 key 的记录进入同一 Partition 并保持日志顺序；不同 key 跨 Partition 后没有全局消费完成顺序。

### 步骤

1. 创建至少 3 Partition 的 Topic。
2. 连续发送 `task-A:1..20`，key 固定为 `task-A`。
3. 发送 `task-A`、`task-B`、`task-C` 交错事件。
4. Consumer 打印 key、Partition、offset、sequence 和完成时间。
5. 在 Listener 内增加不同处理延迟，再对比串行处理和异步线程池处理。

### 预期

- 同一 Partition 的 offset 单调递增。
- 同 key 的日志顺序稳定。
- 不同 Partition 之间无全局顺序。
- 异步处理可能让完成顺序与交付顺序不同。

### 证据

- Producer 发送清单。
- Consumer 带 Partition/offset 的日志。
- 串行与异步完成顺序对照表。

## 实验 2：Producer 超时与幂等重试

### 假设

ACK 丢失或延迟时，Producer 重试可能产生重复；启用幂等生产者可避免同一会话的写入重试重复，但不能替代业务 eventId。

### 变量

- `enable.idempotence=false/true`
- `acks=1/all`
- `max.in.flight.requests.per.connection`
- 网络延迟或代理注入

### 步骤

1. 每条消息携带业务 `eventId` 和自增序号。
2. 注入发送超时或 ACK 延迟。
3. 对比关闭与开启幂等时 Kafka 中相同 eventId 的记录数。
4. 重启 Producer 并主动重发同一个业务 eventId，观察幂等生产者是否识别业务重复。

### 预期

- 幂等生产者减少同一 Producer 会话的协议级重复。
- 应用主动重发相同 eventId 仍可能形成两条消息。
- Consumer 业务幂等仍然必要。

## 实验 3：处理后、提交 offset 前崩溃

### 假设

先处理业务再提交 offset 提供 at-least-once；崩溃窗口会导致重复处理。

### 步骤

1. Consumer 写入 `delivery_attempt`。
2. 在数据库提交后、offset 确认前强制终止 Consumer。
3. 重启 Consumer。
4. 对比无唯一约束与有 `consumed_event` 唯一约束的结果。

### 预期

- 消息被重新投递。
- 无幂等时业务记录重复或状态被重复更新。
- 有唯一约束时第二次处理安全跳过。

### 必须回答

- 重复发生在哪个时间点？
- 哪条数据库约束阻止了什么副作用？
- offset 最终在何时提交？

## 实验 4：先提交 offset、处理前崩溃

### 假设

先提交 offset 再处理形成 at-most-once，崩溃会造成业务处理丢失。

### 步骤

1. 配置或实现先确认 offset。
2. 确认后、数据库写入前终止进程。
3. 重启并检查该消息是否再次出现。
4. 对比 Kafka offset 与业务表。

### 预期

- Consumer 从下一 offset 继续。
- 目标业务记录缺失。
- 证明“没有重复”并不等于可靠。

## 实验 5：Consumer lag 与恢复速度

### 假设

停止 Consumer 会使 lag 增长；恢复后的追赶时间取决于积压量和稳定消费余量。

### 步骤

1. 固定 Producer 速率，例如每秒 500 条。
2. 记录正常消费速率。
3. 停止 Consumer 5 分钟。
4. 恢复 Consumer，记录 lag 峰值、下降曲线和归零时间。
5. 分别测试增加 Consumer 和遇到单热点 Partition。

### 容量计算

```text
backlog = producerRate * outageSeconds
netDrainRate = consumerCapacity - producerRate
recoverySeconds = backlog / netDrainRate
```

若 `consumerCapacity <= producerRate`，系统不会追平积压。

## 实验 6：rebalance 与慢处理

### 假设

Consumer 增减、心跳失败或超过 `max.poll.interval.ms` 会触发 rebalance；慢处理会放大暂停和重复窗口。

### 步骤

1. 启动两个 Consumer。
2. 记录 Partition 分配。
3. 增加第三个 Consumer，再关闭一个 Consumer。
4. 在 Listener 注入超过 `max.poll.interval.ms` 的阻塞。
5. 记录 revoke/assign、重复 eventId、处理暂停时间和 lag。
6. 对比 static membership 或调整处理模型后的结果。

## 实验 7：poison message、重试与 DLT

### 假设

永久反序列化或 Schema 错误不能通过无限重试修复；有界重试与 DLT 可以隔离故障，但需要人工闭环。

### 步骤

1. 发送一个不支持的 `eventVersion`。
2. 配置固定或指数 BackOff。
3. 使用 `DefaultErrorHandler` 和 `DeadLetterPublishingRecoverer`。
4. 记录每次失败时间和最终 DLT Header。
5. 修复 Consumer 或转换数据。
6. 以每秒 1 条限速重放。

### 验收

- 重试次数有上限。
- 原 Topic/Partition/offset 和异常类别可追踪。
- 重放有审计记录。
- 重放不会绕过业务幂等。

## 实验 8：Outbox 发布成功但标记失败

### 假设

Outbox 消除双写丢失，但 Publisher 在发送成功后崩溃会造成重复发布。

### 步骤

1. 同一事务写 task 和 outbox。
2. Publisher 发送 Kafka 成功。
3. 在 `UPDATE event_outbox SET status='PUBLISHED'` 前终止进程。
4. 等待 lease 过期并重启 Publisher。
5. 检查 Kafka 相同 eventId 记录数。
6. 对比无消费幂等和有消费幂等结果。

### 预期

- Outbox 记录再次发布。
- Kafka 中可能存在重复 eventId。
- 幂等 Consumer 只产生一次业务可见结果。

## 实验 9：最小 ISR 故障矩阵

### 配置

```text
replication.factor=3
min.insync.replicas=2
acks=all
```

### 步骤

1. 健康三副本时写入。
2. 停止一个 Follower 后写入。
3. 再停止一个副本，使 ISR 小于 2。
4. 记录 Producer 错误和 Broker 指标。
5. 恢复副本并观察 ISR 恢复。

### 预期

- ISR 满足最小值时仍可写。
- ISR 小于最小值时写失败。
- 证明可靠配置会主动牺牲部分可用性。

## 实验报告评分

| 维度 | 分值 | 标准 |
|---|---:|---|
| 环境可复现 | 15 | 版本、配置、命令完整 |
| 失败时间线 | 20 | 精确到处理、提交、崩溃和恢复点 |
| 原始证据 | 20 | 日志、SQL、offset、指标均保留 |
| 机制解释 | 20 | 能从机制解释现象，不只描述结果 |
| NotifyFlow 修复 | 15 | 修复映射到 Outbox、幂等、状态机或恢复流程 |
| 边界与反例 | 10 | 明确未覆盖场景和残余风险 |

80 分以上且完成真实 Teach-back，才进入下一次课程修订。

# 练习答案与评分标准

## 一、基础题

### 1. Partition 与顺序（4 分）

- Kafka 保证单 Partition 内日志顺序，不保证跨 Partition 全局顺序。（2 分）
- 相同 `taskId` 用相同 key，使同一任务生命周期事件稳定进入同一 Partition。（1 分）
- 仍需避免异步乱序并用状态机/版本号兜底。（1 分）

### 2. ISR（4 分）

- Leader 负责 Partition 的主要读写，Follower 复制日志。（1 分）
- ISR 是与 Leader 保持足够同步、可参与确认或选举的副本集合。（1 分）
- `min.insync.replicas` 指定在配合 `acks=all` 时允许成功写入所需的最小 ISR 数量。（1 分）
- ISR 不足时写入失败，是用可用性换耐久性。（1 分）

### 3. `acks=all`（4 分）

完整答案应提到至少四项中的三项：

- 当前 ISR 可能退化。
- 需要配合 `min.insync.replicas` 和副本数。
- unclean leader election 可能造成数据缺口。
- retention 会删除到期数据。
- 跨机房故障、误操作和磁盘问题仍需备份/灾备。
- ACK 成功不代表 Consumer 业务已完成。

### 4. offset（4 分）

- offset 标识 Partition 中记录的位置以及 Consumer Group 的读取进度。（2 分）
- offset 与 MySQL/HTTP 副作用不是同一个原子事务；先提交 offset 后崩溃会丢处理。（2 分）

### 5. retention 与 compaction（4 分）

- `delete` 按时间或大小删除旧日志，适合审计事件但保留期必须满足恢复 SLA。（2 分）
- `compact` 按 key 保留较新值，适合状态快照或 changelog；不是立即只剩一条。（2 分）

## 二、机制题

### 6. Producer 超时（5 分）

```text
Producer send eventId=E1
-> Broker 写入 E1
-> ACK 丢失
-> Producer timeout
-> retry E1
```

- 无幂等时可能追加第二条 E1。（2 分）
- 启用幂等后，同一 Producer 会话的协议重试可被序列号去重。（2 分）
- 应用主动重发或新 Producer 发送同一 eventId 仍需业务幂等。（1 分）

### 7. 两种提交顺序（6 分）

```text
处理成功 -> 崩溃 -> offset 未提交 -> 重读 -> 重复
```

这是 at-least-once，需要幂等。（3 分）

```text
offset 提交 -> 崩溃 -> 未处理 -> 重启从下一条开始 -> 丢失
```

这是 at-most-once。（3 分）

### 8. rebalance（5 分）

- 处理超过 `max.poll.interval.ms`，Consumer 可能被认为失效并触发 rebalance。（2 分）
- 旧 Consumer 仍可能完成外部调用，新 Consumer 又处理相同分区，放大重复窗口。（1 分）
- 治理任选三项，每项 0.67 分：缩短单条处理、调整批量大小、把慢调用状态化、设置超时、暂停/恢复容器、合理提高 poll interval、使用 static membership、业务幂等、隔离供应商并发。

### 9. Kafka exactly-once（5 分）

- EOS 可原子协调 Kafka 输入 offset 与 Kafka 输出记录。（2 分）
- 短信供应商不参与 Kafka 事务，HTTP 成功/超时也不能由 Kafka 回滚。（2 分）
- 需要供应商幂等 key、结果查询、业务状态机和对账。（1 分）

### 10. 顺序与异步线程池（4 分）

- A、B 进入线程池后耗时不同，完成顺序可能反转。（2 分）
- 可按 key 串行、使用分区内单线程、分片执行器或 sequence + 条件状态更新。（2 分）

## 三、应用题

### 11. Outbox DDL（8 分）

评分点：

- `event_id` 唯一。（1 分）
- `aggregate_id`、`partition_key`。（1 分）
- `event_type`、`event_version`、`payload`。（1 分）
- `status`、`attempt_count`、`next_attempt_at`。（1 分）
- `lease_owner`、`lease_until`。（1 分）
- `created_at`、`updated_at`、`published_at`。（1 分）
- `(status, next_attempt_at, id)` 索引支持待发布扫描。（1 分）
- 能说明唯一键防止同一业务事务重复创建事件，但不能防止 Publisher 重复发送。（1 分）

### 12. Outbox 崩溃窗口（6 分）

- lease 到期后记录被再次领取。（1 分）
- 因第一次 Kafka 发送已成功，再次发送形成重复 eventId。（2 分）
- Consumer 通过 `(consumerName, eventId)` 唯一键、业务状态机和供应商幂等处理。（2 分）
- 保留 duplicate 指标和审计。（1 分）

### 13. 消费幂等（6 分）

```sql
PRIMARY KEY (consumer_name, event_id)
```

- 在同一数据库事务中插入 consumed_event 并更新业务状态。（3 分）
- 唯一键冲突表示已成功处理或已占有，按设计安全跳过。（1 分）
- 数据库事务成功后确认 offset。（1 分）
- 外部调用需另设 attempt 状态，不能放在长事务中。（1 分）

### 14. 外部供应商 Unknown（5 分）

- 客户端超时无法证明供应商未接受，直接重试可能重复收费。（2 分）
- 发送稳定 attemptId/幂等 key。（1 分）
- 超时后先查询结果；不能查询则进入 Unknown/待对账。（1 分）
- 对账后状态化收敛，重试需要明确授权和成本边界。（1 分）

### 15. retry/DLT（5 分）

- 429：瞬时/限流，读取 Retry-After 或指数退避。（1 分）
- 手机号非法：永久业务错误，不重试。（1 分）
- eventVersion 不支持：poison/schema，有限重试或直接 DLT。（1 分）
- 数据库整体不可用：系统性故障，暂停/降速消费并告警。（1 分）
- 供应商 500：瞬时错误，有界退避、熔断，必要时查询结果。（1 分）

## 四、系统设计题

### 16. 先进制造设备命令流（8 分）

高分答案包含：

- Topic 可按命令类型/安全等级拆分，key=`deviceId`，保证同设备分区内顺序。（1.5 分）
- Partition 由峰值、单分区安全吞吐、恢复时间和未来并发估算。（1 分）
- Consumer 按设备串行或 sequence 执行，不同设备并发。（1 分）
- 大客户热点通过租户内二级分片、独立 Topic/配额、设备分布分析治理，不能破坏单设备顺序。（1.5 分）
- `commandId` 唯一，设备端保存已执行序号/幂等结果。（1 分）
- 回执事件包含 commandId、device sequence、结果和时间。（1 分）
- 超时进入 Unknown，查询/对账；永久格式错误进入 DLT；重放限速。（1 分）

### 17. 容量与恢复（7 分）

1. 积压：`5000 * 20 * 60 = 6,000,000` 条。（2 分）
2. 净消化速率：`7000 - 5000 = 2000` 条/秒。（1 分）
3. 理论追平：`6,000,000 / 2000 = 3000 秒 = 50 分钟`。（2 分）
4. 现实因素：热 Partition、重试、下游变慢、rebalance、GC、批次效率、磁盘/网络、限流、poison message。（任四项 2 分）

## 五、反例与表达题

### 18. 纠正简历表述（5 分）

参考答案：

> 为 NotifyFlow 设计可靠消息链路：Producer 使用 `acks=all`、副本与最小 ISR 配置并启用幂等重试；数据库与 Kafka 之间采用 Transactional Outbox，Consumer 采用至少一次投递，通过事件唯一键、条件状态更新和供应商幂等键处理重复；配套有界重试、DLT、人工重放和对账。Kafka exactly-once 仅用于 Kafka 事务边界内，不扩展到外部供应商。

评分：有范围（2）、有 Outbox（1）、有消费幂等（1）、不承诺绝对 exactly-once（1）。

### 19. 选型（5 分）

1. 数据库任务表：规模小、已有 MySQL、运维成本优先；做好索引、领取、重试和归档。（1.5 分）
2. Kafka：事件保留、回放、流式分析和 Agent 事件生态更匹配。（1.5 分）
3. RocketMQ：团队已有经验，事务、message group FIFO 和 24 小时内延迟消息需求与产品能力匹配；仍需消费幂等和监控。（2 分）

## Teach-back 评分

| 维度 | 分值 |
|---|---:|
| 说清双写失败窗口 | 20 |
| 说清 Outbox 同事务 | 20 |
| 承认重复发布窗口 | 20 |
| 说清消费幂等和外部副作用 | 20 |
| 说清监控、DLT 和人工恢复 | 20 |


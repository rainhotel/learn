# NotifyFlow 项目应用：可靠任务事件链路

## 1. 目标与非目标

### 目标

- API 返回成功后，任务和待发布事件同时存在。
- Publisher 可在崩溃、超时和重启后恢复。
- Kafka 消息允许重复，业务结果必须可收敛和审计。
- 单条毒消息、单个租户或单个供应商故障不能拖垮全局。
- DLT 消息可定位、修复、限速重放和追踪结果。

### 非目标

- 不宣称 MySQL、Kafka 和外部供应商组成全局 exactly-once 事务。
- 不在数据库长事务中调用外部 HTTP。
- 不用 Redis 作为最终幂等真相。
- 不为了简历而强制所有任务都经过 Kafka；低规模阶段可保留数据库调度路径。

## 2. 总体架构

```text
Client
  -> Notify API
       -> MySQL transaction
            notification_task
            event_outbox

Outbox Publisher
  -> claim PENDING rows
  -> publish notify-task-events
  -> mark PUBLISHED or schedule retry

Kafka
  -> Delivery Consumer Group
       -> consumed_event / task state machine
       -> Provider Adapter
       -> result query / reconciliation
       -> task-result event + outbox

Failure Recovery
  -> retry topics or database delayed task
  -> DLT
  -> operator console
  -> rate-limited replay
```

## 3. 数据模型

### 3.1 通知任务表

```sql
CREATE TABLE notification_task (
  id                  BIGINT       NOT NULL,
  tenant_id           BIGINT       NOT NULL,
  request_id          VARCHAR(64)  NOT NULL,
  channel             VARCHAR(32)  NOT NULL,
  recipient_hash      VARCHAR(128) NOT NULL,
  template_id         BIGINT       NOT NULL,
  status              VARCHAR(32)  NOT NULL,
  version             BIGINT       NOT NULL DEFAULT 0,
  current_attempt_id  VARCHAR(64)  NULL,
  next_attempt_at     DATETIME(6)  NULL,
  last_error_code     VARCHAR(64)  NULL,
  created_at          DATETIME(6)  NOT NULL,
  updated_at          DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_request (tenant_id, request_id),
  KEY idx_task_schedule (status, next_attempt_at, id)
);
```

`recipient_hash` 用于减少敏感数据进入消息和日志；实际收件人应按安全设计加密保存或通过受控数据源读取。

### 3.2 Outbox 表

```sql
CREATE TABLE event_outbox (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  event_id         VARCHAR(64)  NOT NULL,
  aggregate_type   VARCHAR(64)  NOT NULL,
  aggregate_id     VARCHAR(64)  NOT NULL,
  event_type       VARCHAR(128) NOT NULL,
  event_version    INT          NOT NULL,
  partition_key    VARCHAR(128) NOT NULL,
  payload          JSON         NOT NULL,
  status           VARCHAR(16)  NOT NULL,
  attempt_count    INT          NOT NULL DEFAULT 0,
  next_attempt_at  DATETIME(6)  NOT NULL,
  lease_owner      VARCHAR(128) NULL,
  lease_until      DATETIME(6)  NULL,
  published_at     DATETIME(6)  NULL,
  last_error       VARCHAR(512) NULL,
  created_at       DATETIME(6)  NOT NULL,
  updated_at       DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_outbox_event (event_id),
  KEY idx_outbox_publish (status, next_attempt_at, id),
  KEY idx_outbox_lease (lease_until, id)
);
```

### 3.3 消费幂等表

```sql
CREATE TABLE consumed_event (
  consumer_name VARCHAR(100) NOT NULL,
  event_id      VARCHAR(64)  NOT NULL,
  aggregate_id  VARCHAR(64)  NOT NULL,
  processed_at  DATETIME(6)  NOT NULL,
  PRIMARY KEY (consumer_name, event_id),
  KEY idx_consumed_aggregate (aggregate_id, processed_at)
);
```

### 3.4 投递尝试表

```sql
CREATE TABLE delivery_attempt (
  attempt_id          VARCHAR(64)  NOT NULL,
  task_id             BIGINT       NOT NULL,
  attempt_no          INT          NOT NULL,
  provider            VARCHAR(64)  NOT NULL,
  provider_request_id VARCHAR(128) NULL,
  status              VARCHAR(32)  NOT NULL,
  error_code          VARCHAR(64)  NULL,
  started_at          DATETIME(6)  NOT NULL,
  finished_at         DATETIME(6)  NULL,
  PRIMARY KEY (attempt_id),
  UNIQUE KEY uk_task_attempt (task_id, attempt_no)
);
```

## 4. 创建任务：task 与 outbox 同事务

```text
1. 校验 tenant、模板、渠道和 requestId。
2. 开启 MySQL 事务。
3. 插入 notification_task，唯一约束拦截重复 requestId。
4. 插入 event_outbox，eventId 全局唯一，partitionKey=taskId。
5. 提交事务。
6. 返回 taskId；不在请求事务中等待 Kafka。
```

失败语义：

- 两条 INSERT 任一失败，整个事务回滚。
- Kafka 故障不影响任务创建事务，但会形成 Outbox backlog。
- API 成功只承诺“任务已持久化并等待异步处理”，不承诺已经送达供应商。

## 5. Outbox Publisher

### 5.1 领取

多实例 Publisher 使用短事务领取一批记录：

```sql
SELECT id
FROM event_outbox
WHERE status IN ('PENDING', 'RETRY')
  AND next_attempt_at <= NOW(6)
  AND (lease_until IS NULL OR lease_until < NOW(6))
ORDER BY id
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

随后更新：

```text
status=IN_FLIGHT
lease_owner=<instance-id>
lease_until=now+30s
attempt_count=attempt_count+1
```

提交领取事务后再发送 Kafka，避免持有数据库锁等待网络。

### 5.2 发布

- Kafka key 使用 `partition_key`。
- Header 携带 eventId、eventType、eventVersion、traceId 和 producer。
- Producer 启用幂等，使用 `acks=all`。
- 单次发送受 `delivery.timeout.ms` 约束。
- 回调成功后以新短事务标记 `PUBLISHED`。

### 5.3 发布成功、标记失败

这是必须接受的重复窗口。记录再次被领取并发布时：

- Kafka 可能出现相同 `eventId` 的两条记录。
- Consumer 的 `consumed_event` 唯一键使第二次处理变成 no-op。
- 监控应统计 duplicate event，而不是把重复完全隐藏。

### 5.4 退避

建议：

```text
delay = min(base * 2^(attempt-1), maxDelay) + randomJitter
```

到达最大尝试次数后不直接删除：

- 标记 `FAILED`。
- 触发告警。
- 保留 last_error。
- 提供人工恢复或批量重置入口。

## 6. Topic 与 key 设计

### 6.1 Topic

- `notify-task-events-v1`：任务生命周期事件。
- `notify-delivery-retry-1m-v1`、`notify-delivery-retry-10m-v1`：可选重试级别。
- `notify-delivery-dlt-v1`：不可自动恢复事件。

初版也可把延迟重试保留在 MySQL `next_attempt_at`，避免同时引入多级 retry topic。选择必须记录原因。

### 6.2 key

- 任务生命周期：`taskId`。
- 设备命令：`deviceId`。
- 工作流：`workflowInstanceId`。

不要使用常量 key；不要默认用 tenantId 导致大租户热点。

### 6.3 Partition 数量

依据：

- 峰值消息速率。
- 单 Consumer 单 Partition 的稳定处理能力。
- 未来 Consumer 并发上限。
- 热 key 分布。
- 故障恢复追赶速度。

粗略估算：

```text
requiredPartitions >= peakMessageRate / safeRatePerPartition
```

再预留增长和故障降级空间。Partition 增加后 key 到 Partition 的映射可能变化，因此不能把 Partition 编号当业务标识。

## 7. Consumer 事务边界

### 7.1 接收并记录业务意图

```text
读取 TaskCreated
-> 开启数据库短事务
-> 插入 consumed_event
-> 条件更新 task: PENDING -> READY/SENDING
-> 创建 delivery_attempt
-> 提交数据库事务
```

如果唯一键冲突，说明 event 已处理，可直接确认消息。

### 7.2 外部调用不放入长事务

```text
准备调用事务提交
-> 调用供应商，设置 connect/read/total timeout
-> 使用 attemptId 作为供应商幂等键（若支持）
-> 新事务写入调用结果
-> 同事务写结果 outbox
```

若客户端超时，结果是 Unknown，不应立即假设失败并重新发送。优先：

1. 用 attemptId 查询供应商结果。
2. 若供应商不支持查询，进入待对账状态。
3. 只有在重复成本可控或有幂等保证时自动重试。

### 7.3 offset 确认

只有数据库记录已提交且当前消息可以安全重放时，才确认 Kafka 进度。外部调用流程必须依靠 `delivery_attempt` 状态恢复，不能依赖 Listener 栈内存。

## 8. 重试与 DLT

### 8.1 分类函数

```text
InvalidRecipientException -> PERMANENT -> task FAILED
TemplateDisabledException -> PERMANENT -> task FAILED
Provider429Exception      -> TRANSIENT -> delayed retry
Provider5xxException      -> TRANSIENT -> retry/circuit breaker
SchemaException           -> POISON -> DLT
DatabaseUnavailable       -> SYSTEMIC -> pause/slow consumer
```

### 8.2 DLT 信封

```json
{
  "failedEvent": {},
  "source": {
    "topic": "notify-task-events-v1",
    "partition": 3,
    "offset": 9281
  },
  "failure": {
    "category": "SCHEMA",
    "exception": "UnknownEventVersionException",
    "message": "eventVersion=3 is unsupported",
    "attempts": 4,
    "firstFailedAt": "2026-07-14T10:00:00Z",
    "lastFailedAt": "2026-07-14T10:10:00Z"
  }
}
```

生产中避免直接暴露完整堆栈、手机号、token 等敏感数据。

### 8.3 人工重放控制面

最小接口：

- 按事件类型、租户、异常类别、时间筛选。
- 查看脱敏 payload 和失败历史。
- 单条/小批量重放。
- 设置 QPS 和并发上限。
- 指定 dry-run。
- 强制填写处置原因。
- 查看 replay batch 的成功、重复、再次失败数量。

重放消息保留原 `eventId` 可触发幂等跳过；若要在修复后重新执行业务，需要显式 `replayId` 和授权策略，不能偷偷修改 ID 绕过幂等。

## 9. 监控与 SLO

### 9.1 业务指标

- task 创建成功率。
- 从创建到首次尝试的 P95/P99。
- 最终送达率。
- duplicate event 数量。
- Unknown 供应商结果数量。
- DLT 数量和最长未处理时长。
- 人工重放成功率。

### 9.2 Outbox 指标

- `outbox_pending_total`
- `outbox_oldest_pending_seconds`
- `outbox_publish_rate`
- `outbox_publish_error_rate`
- `outbox_duplicate_publish_total`
- lease 过期数量

### 9.3 Kafka 指标

- Consumer lag 和 lag 增长速度。
- Under-replicated/under-min-ISR partition。
- Producer retry/error/latency。
- rebalance 次数和持续时间。
- retry topic 和 DLT 流量。

### 9.4 示例告警

```text
outbox_oldest_pending_seconds > 60 for 5m
records_lag_max 持续增长 10m
UnderMinIsrPartitionCount > 0
DLT rate > 1% of consumed rate
Provider UNKNOWN result > 0.1% for 10m
```

阈值需要通过压测和真实流量修订。

## 10. 故障场景验收

### 场景 A：Kafka 不可用

- 任务创建继续成功。
- Outbox backlog 增长并告警。
- Publisher 有界退避，不压垮数据库。
- Kafka 恢复后能追赶，且 Consumer 幂等处理重复。

### 场景 B：Consumer 在供应商成功后崩溃

- 重启后消息可能重复。
- 相同 attemptId 不应产生不可解释的二次收费。
- 若供应商结果 Unknown，进入查询/对账，不盲目重试。

### 场景 C：poison message

- 有界重试后进入 DLT。
- 后续消息是否继续取决于顺序需求和策略。
- 告警包含 eventId、Schema 版本和来源 offset。

### 场景 D：大租户热点

- 单 Partition lag 明显高于其他 Partition。
- 通过 key 设计、租户分片、配额或独立 Topic 治理。
- 不通过无限增加 Consumer 掩盖单 Partition 瓶颈。

## 11. 简历与面试表达边界

可表达：

> 独立设计 NotifyFlow 可靠事件链路，使用 MySQL Transactional Outbox 消除任务表与待发布事件的双写丢失窗口；Publisher 采用至少一次发布，Consumer 通过事件唯一键、条件状态更新和供应商幂等键处理重复；设计重试、DLT、人工限速重放与 lag/Outbox backlog 告警，并通过崩溃注入验证重复与恢复路径。

必须在真实实现和实验后再写“完成”“验证”“降低多少”。当前课程阶段只能写“设计中”或“独立重构项目计划”，不能伪装成大烨实习的线上技术栈。


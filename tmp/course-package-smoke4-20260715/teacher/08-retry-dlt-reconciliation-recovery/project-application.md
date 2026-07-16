# NotifyFlow 项目应用：故障恢复控制面

## 1. 建设目标

NotifyFlow 的恢复控制面负责回答：

- 当前失败属于哪一类？
- 自动恢复还剩多少预算？
- 是否需要暂停某个渠道或供应商？
- 哪些结果是 Unknown，需要对账？
- DLT 中的消息由谁处理？
- 哪个操作员批准并重放了哪些任务？
- 重放是否产生重复副作用？

它不是 Kafka 管理页面的替代品，而是面向业务失败的操作系统。

## 2. 模块边界

```text
Delivery Worker
  -> Failure Classifier
  -> Retry Policy Engine
  -> Provider Health / Circuit State
  -> Retry Scheduler

Kafka Error Path
  -> DLT Ingestor
  -> Failure Case

Provider Callback / Query / Bill
  -> Reconciliation Engine
  -> Attempt State Repair

Operator Console
  -> Search / Diagnose / Dry-run
  -> Approval
  -> Replay Orchestrator
  -> Audit Log

RAG/Agent Assistant
  -> Read-only evidence retrieval
  -> Diagnosis suggestion
  -> No direct high-risk execution
```

## 3. 核心数据模型

### 3.1 投递尝试

```sql
CREATE TABLE delivery_attempt (
  attempt_id            VARCHAR(64)  NOT NULL,
  task_id               BIGINT       NOT NULL,
  attempt_no            INT          NOT NULL,
  provider_code         VARCHAR(64)  NOT NULL,
  idempotency_key       VARCHAR(128) NOT NULL,
  provider_request_id   VARCHAR(128) NULL,
  status                VARCHAR(32)  NOT NULL,
  failure_category      VARCHAR(32)  NULL,
  error_code            VARCHAR(64)  NULL,
  error_summary         VARCHAR(512) NULL,
  started_at            DATETIME(6)  NOT NULL,
  timeout_at            DATETIME(6)  NOT NULL,
  finished_at           DATETIME(6)  NULL,
  version               BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (attempt_id),
  UNIQUE KEY uk_attempt_task_no (task_id, attempt_no),
  UNIQUE KEY uk_attempt_idempotency (provider_code, idempotency_key),
  KEY idx_attempt_unknown (status, provider_code, started_at)
);
```

### 3.2 重试计划

```sql
CREATE TABLE retry_schedule (
  id                    BIGINT       NOT NULL AUTO_INCREMENT,
  task_id               BIGINT       NOT NULL,
  source_attempt_id     VARCHAR(64)  NOT NULL,
  policy_version        INT          NOT NULL,
  retry_no              INT          NOT NULL,
  scheduled_at          DATETIME(6)  NOT NULL,
  deadline_at           DATETIME(6)  NOT NULL,
  status                VARCHAR(24)  NOT NULL,
  lease_owner           VARCHAR(128) NULL,
  lease_until           DATETIME(6)  NULL,
  reason                VARCHAR(256) NOT NULL,
  created_at            DATETIME(6)  NOT NULL,
  updated_at            DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_retry_attempt_no (source_attempt_id, retry_no),
  KEY idx_retry_claim (status, scheduled_at, id)
);
```

### 3.3 失败 case

```sql
CREATE TABLE failure_case (
  case_id               VARCHAR(64)  NOT NULL,
  source_type           VARCHAR(32)  NOT NULL,
  source_id             VARCHAR(128) NOT NULL,
  tenant_id             BIGINT       NOT NULL,
  failure_category      VARCHAR(32)  NOT NULL,
  status                VARCHAR(24)  NOT NULL,
  severity              VARCHAR(16)  NOT NULL,
  owner                  VARCHAR(128) NULL,
  diagnosis             TEXT         NULL,
  resolution            TEXT         NULL,
  first_failed_at       DATETIME(6)  NOT NULL,
  last_failed_at        DATETIME(6)  NOT NULL,
  resolved_at           DATETIME(6)  NULL,
  version               BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (case_id),
  UNIQUE KEY uk_failure_source (source_type, source_id),
  KEY idx_failure_queue (status, severity, first_failed_at)
);
```

### 3.4 对账 case

```sql
CREATE TABLE reconciliation_case (
  reconciliation_id    VARCHAR(64)  NOT NULL,
  attempt_id            VARCHAR(64)  NOT NULL,
  provider_code         VARCHAR(64)  NOT NULL,
  status                VARCHAR(24)  NOT NULL,
  query_count           INT          NOT NULL DEFAULT 0,
  next_query_at         DATETIME(6)  NOT NULL,
  provider_status       VARCHAR(64)  NULL,
  evidence_json         JSON         NULL,
  deadline_at           DATETIME(6)  NOT NULL,
  resolved_at           DATETIME(6)  NULL,
  created_at            DATETIME(6)  NOT NULL,
  updated_at            DATETIME(6)  NOT NULL,
  PRIMARY KEY (reconciliation_id),
  UNIQUE KEY uk_reconciliation_attempt (attempt_id),
  KEY idx_reconciliation_due (status, next_query_at, reconciliation_id)
);
```

### 3.5 重放批次

```sql
CREATE TABLE replay_batch (
  replay_id             VARCHAR(64)  NOT NULL,
  tenant_scope          VARCHAR(512) NOT NULL,
  source_filter         JSON         NOT NULL,
  mode                  VARCHAR(24)  NOT NULL,
  status                VARCHAR(24)  NOT NULL,
  max_qps               INT          NOT NULL,
  max_concurrency       INT          NOT NULL,
  requested_by          VARCHAR(128) NOT NULL,
  approved_by           VARCHAR(128) NULL,
  approval_reason       VARCHAR(512) NULL,
  requested_at          DATETIME(6)  NOT NULL,
  approved_at           DATETIME(6)  NULL,
  started_at            DATETIME(6)  NULL,
  finished_at           DATETIME(6)  NULL,
  version               BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (replay_id),
  KEY idx_replay_status (status, requested_at)
);
```

```sql
CREATE TABLE replay_item (
  replay_id             VARCHAR(64)  NOT NULL,
  source_type           VARCHAR(32)  NOT NULL,
  source_id             VARCHAR(128) NOT NULL,
  original_event_id     VARCHAR(64)  NULL,
  replay_event_id       VARCHAR(64)  NULL,
  status                VARCHAR(24)  NOT NULL,
  result_summary        VARCHAR(512) NULL,
  processed_at          DATETIME(6)  NULL,
  PRIMARY KEY (replay_id, source_type, source_id)
);
```

### 3.6 操作审计

```sql
CREATE TABLE recovery_audit_log (
  audit_id              BIGINT       NOT NULL AUTO_INCREMENT,
  actor                 VARCHAR(128) NOT NULL,
  action                VARCHAR(64)  NOT NULL,
  target_type           VARCHAR(64)  NOT NULL,
  target_id             VARCHAR(128) NOT NULL,
  tenant_id             BIGINT       NULL,
  request_id            VARCHAR(64)  NOT NULL,
  reason                VARCHAR(512) NOT NULL,
  before_snapshot       JSON         NULL,
  after_snapshot        JSON         NULL,
  occurred_at           DATETIME(6)  NOT NULL,
  PRIMARY KEY (audit_id),
  UNIQUE KEY uk_audit_request (request_id),
  KEY idx_audit_target (target_type, target_id, occurred_at)
);
```

## 4. 错误分类器

输入：

```text
providerCode
exception type
HTTP status
provider error code
hasProviderRequestId
request timeout phase
current circuit state
task deadline
```

输出：

```json
{
  "category": "THROTTLED",
  "retryable": true,
  "nextAction": "SCHEDULE_RETRY",
  "minimumDelaySeconds": 60,
  "requiresReconciliation": false,
  "policyVersion": 3
}
```

分类规则要版本化。事故复盘后修改规则时，历史 attempt 保留当时的 `policyVersion`。

## 5. 重试策略

示例渠道策略：

```json
{
  "provider": "sms-a",
  "maxAttempts": 4,
  "totalDeadlineSeconds": 900,
  "attemptTimeoutMillis": 3000,
  "baseBackoffMillis": 1000,
  "maxBackoffMillis": 120000,
  "jitter": "FULL",
  "retryTokenRate": 20,
  "retryTokenBurst": 40,
  "retryableCodes": ["429", "502", "503", "504"]
}
```

规则：

- `maxAttempts` 包含首次调用。
- 到达任务 deadline 后停止自动重试。
- 429 优先使用合法的 Retry-After。
- `UNKNOWN` 不进入普通 retry schedule。
- 熔断 OPEN 时不创建大量立即到期的重试记录。

## 6. 外部调用流程

```text
创建 attempt(SENDING)
-> 调用供应商，携带 idempotencyKey
-> 明确成功：attempt=SUCCEEDED
-> 明确永久失败：attempt=PERMANENT_FAILED
-> 明确瞬时失败：attempt=RETRY_WAIT + retry_schedule
-> timeout/连接中断且结果不确定：attempt=UNKNOWN + reconciliation_case
```

结果确认和结果事件使用新的短事务，并写 Outbox。

## 7. 对账引擎

### 7.1 数据源优先级

1. 供应商按 idempotencyKey/providerRequestId 查询。
2. 供应商回调。
3. 批量账单或投递报告。
4. 人工供应商工单。

### 7.2 收敛规则

- 查询到成功：`UNKNOWN -> SUCCEEDED`。
- 查询到明确失败：根据错误类别进入重试或永久失败。
- 查询仍未知：增加 query_count，使用长退避。
- 超过 reconciliation deadline：进入 `MANUAL`。

### 7.3 并发与乱序

更新 attempt 时使用：

```sql
UPDATE delivery_attempt
SET status = ?, version = version + 1
WHERE attempt_id = ?
  AND status IN ('UNKNOWN', 'SENDING')
  AND version = ?;
```

对已确认 `SUCCEEDED` 的 attempt，晚到的失败回调不能直接覆盖，必须进入异常 case。

## 8. DLT 接入

DLT Ingestor 不直接重放，先创建或合并 `failure_case`：

```text
Kafka DLT record
-> 验证 Header 和 payload 大小
-> 脱敏
-> 按 source topic + partition + offset 唯一入库
-> 聚类相同 eventType / exception / version
-> 分配 owner 和 SLA
```

对于同一代码缺陷造成的 10 万条 DLT，不需要创建 10 万个操作工单。可以一条 incident 关联多个 failure item，但每个原始事件仍要可追踪。

## 9. 恢复控制面 API

### 查询

```text
GET /recovery/cases?status=OPEN&category=POISON&tenantId=...
GET /recovery/cases/{caseId}
GET /recovery/reconciliation?status=MANUAL
GET /recovery/replays/{replayId}
```

### 操作

```text
POST /recovery/cases/{caseId}/assign
POST /recovery/cases/{caseId}/diagnose
POST /recovery/replays/preview
POST /recovery/replays
POST /recovery/replays/{replayId}/approve
POST /recovery/replays/{replayId}/start
POST /recovery/replays/{replayId}/pause
POST /recovery/channels/{provider}/open-circuit
POST /recovery/channels/{provider}/half-open
```

所有写操作要求：

- requestId 幂等。
- actor 身份和租户范围。
- reason 非空。
- expectedVersion。
- before/after 审计快照。

## 10. Replay preview

预览必须返回：

- 目标数量。
- 租户分布。
- 事件类型和版本分布。
- 原因分布。
- 已成功、已处理或幂等冲突数量。
- 预计 QPS 和完成时间。
- 最大潜在外部副作用。
- 是否需要新 eventId。
- 是否需要审批。

示例：

```json
{
  "candidateCount": 1200,
  "alreadySucceeded": 830,
  "safeNoop": 830,
  "willInvokeProvider": 370,
  "estimatedSeconds": 185,
  "approvalRequired": true,
  "risk": "MEDIUM"
}
```

## 11. Replay 执行

- 每个 replay batch 独立令牌桶。
- 受供应商全局配额和租户配额双重限制。
- 支持暂停，不依赖进程内存保存进度。
- 每个 item 使用条件状态更新领取。
- 原事件和 replayId 同时进入 Header。
- 再次失败时关联原 case，不无限创建新 case。

## 12. 渠道熔断和恢复

渠道状态表：

```sql
CREATE TABLE provider_health_state (
  provider_code         VARCHAR(64) NOT NULL,
  state                 VARCHAR(16) NOT NULL,
  reason                VARCHAR(512) NOT NULL,
  opened_at             DATETIME(6) NULL,
  next_probe_at          DATETIME(6) NULL,
  probe_budget           INT NOT NULL,
  version                BIGINT NOT NULL DEFAULT 0,
  updated_by             VARCHAR(128) NOT NULL,
  updated_at             DATETIME(6) NOT NULL,
  PRIMARY KEY (provider_code)
);
```

多实例必须读取同一权威状态或通过事件同步，不能每个实例独立判断后同时 HALF_OPEN。

## 13. Agent/RAG 辅助

输入：

- failure case 摘要。
- 脱敏异常。
- 指标时间线。
- 当前策略版本。
- Runbook 和历史复盘。

输出：

- 可能根因。
- 支持证据引用。
- 建议检查项。
- 重放风险清单。
- 建议的 dry-run 过滤条件。

执行仍由恢复 API 的确定性权限和状态机控制。Agent 不持有直接修改数据库或全量重放权限。

## 14. 指标

```text
notifyflow_retry_attempt_total{provider,category}
notifyflow_retry_budget_exhausted_total{provider}
notifyflow_provider_circuit_state{provider,state}
notifyflow_unknown_attempts{provider}
notifyflow_reconciliation_age_seconds
notifyflow_dlt_open_cases{category,severity}
notifyflow_dlt_oldest_case_seconds
notifyflow_replay_items_total{replayId,status}
notifyflow_replay_rate{replayId}
```

## 15. 权限验收

- Viewer 无法创建重放。
- Operator 只能重放授权租户的小批量任务。
- 大于阈值的 replay 必须由另一用户批准。
- 请求者不能批准自己的高风险批次。
- 修改 max QPS 和创建新 eventId 都进入高风险审计。
- Agent service account 只有读取和生成建议权限。

## 16. 简历表达

真实实现和实验完成后可表达：

> 为 NotifyFlow 设计故障恢复控制面，将失败分类为瞬时、限流、永久、毒消息、Unknown 和系统性故障；通过单点重试预算、上限指数退避与抖动避免重试放大，使用 DLT 隔离、供应商查询/回调对账和带审批限速的人工重放完成恢复闭环，并为所有操作保留租户权限和审计证据。

当前阶段只完成课程与设计初稿，不能声称控制面已经上线或完成压测。


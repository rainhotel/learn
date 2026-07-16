# NotifyFlow + Agent 事故助手系统设计包

## 1. 项目命题

设计并实现一个多租户可靠通知平台。业务系统异步创建短信、邮件和 Webhook 通知，平台负责去重、调度、投递、回调、重试、对账和受控重放；事故助手读取授权证据，回答“为什么延迟、失败或积压”，但不直接执行高风险动作。

## 2. 需求基线

### 功能范围

- 创建、查询、条件取消通知任务。
- 单任务多投递扇出，支持定时和模板版本。
- Provider 调用、回调、UNKNOWN 对账、DLT 和 replay preview。
- 租户、渠道、Provider 三级配额。
- 任务时间线、指标、日志、Trace 和审计。
- 事故助手只读查询、证据引用、不确定性和后续建议。

### 待验证目标

| 项目 | 设计目标 | 说明 |
|---|---:|---|
| 日任务量 | 10,000,000 | 教学规模假设 |
| 峰值任务 QPS | 约 928/s | 日均乘 8 倍峰值 |
| 峰值投递 QPS | 约 1,207/s | 平均扇出 1.3 |
| 创建 API P99 | 200 ms 内 | 不等待 Provider |
| 任务最终状态 | 99.9% 在 5 分钟内 | 需排除长期 Provider 故障并定义口径 |
| RPO | 已提交任务不因单实例故障丢失 | 依赖数据库与备份验证 |
| Agent 高风险动作 | 0 次自动执行 | 架构约束，不是模型承诺 |

所有数字在压测和故障演练前只能称为目标或假设。

## 3. 组件图

```text
                         +----------------------+
Client -> Gateway ------>| Notify API           |
                         | Query API             |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | MySQL                |
                         | task/delivery/outbox |
                         +----+-------------+---+
                              |             |
                      outbox scan       query/state
                              v             |
                         +---------+        |
                         | Kafka   |<-------+
                         +----+----+
                              |
                    +---------+----------+
                    | Channel Workers    |
                    | quota + adapters   |
                    +---+-------------+--+
                        |             |
                     Redis         Providers
                                      |
                            callback/query result
                                      v
                          Reconciliation Service

Telemetry/Runbook/Task Timeline -> Evidence Gateway -> Agent Assistant
Replay request -> Preview -> Human Approval -> Replay Executor -> Kafka
```

## 4. 核心写入时序

```text
Client
  -> Gateway: auth + tenant + request limit
  -> Notify API: validate + hash request
  -> MySQL transaction:
       insert notification_task
       insert notification_delivery(s)
       insert outbox_event(s)
  <- 202 taskId

Outbox Publisher
  -> claim unpublished event
  -> Kafka send(eventId, deliveryId)
  -> mark published

Worker
  -> load latest delivery
  -> quota + state check
  -> Provider(idempotent request id)
  -> conditional state update
  -> commit offset
```

关键不变量：API 返回成功必须对应已提交的数据库事实；任何重复事件不得绕过状态机制造新的逻辑投递；timeout 不得直接覆盖为 FAILED。

## 5. API 合同草案

### 创建任务

```json
{
  "businessRef": "order-9001-shipped",
  "deliveries": [
    {
      "channel": "SMS",
      "recipientRef": "customer_123_phone",
      "templateId": "order-shipped",
      "templateVersion": 4,
      "variables": {"orderNo": "9001"}
    }
  ],
  "scheduledAt": "2026-07-16T01:00:00Z",
  "priority": "NORMAL"
}
```

服务端校验：

- `tenant_id + Idempotency-Key` 唯一。
- 相同 key 的 `request_hash` 必须相同。
- 模板版本、接收者引用和渠道权限属于当前租户。
- `scheduledAt`、变量大小、投递数量和 payload 大小有上限。

### Agent 查询

```json
{
  "incidentId": "inc_20260716_001",
  "question": "过去 15 分钟邮件渠道为什么积压？",
  "timeRange": {"from": "2026-07-16T00:45:00Z", "to": "2026-07-16T01:00:00Z"},
  "scope": {"tenantId": "tenant_42", "channel": "EMAIL"}
}
```

回答必须包含 `claims[]`、每条 claim 的 `evidenceIds[]`、`uncertainties[]`、`recommendedReadOnlyChecks[]` 和 `prohibitedActions[]`。没有证据时返回“不足以判断”。

## 6. 数据模型与索引草案

```sql
CREATE TABLE notification_task (
  task_id           BINARY(16) PRIMARY KEY,
  tenant_id         BIGINT NOT NULL,
  idempotency_key   VARCHAR(128) NOT NULL,
  request_hash      BINARY(32) NOT NULL,
  aggregate_state   VARCHAR(32) NOT NULL,
  scheduled_at      DATETIME(3) NULL,
  version            BIGINT NOT NULL DEFAULT 0,
  created_at         DATETIME(3) NOT NULL,
  updated_at         DATETIME(3) NOT NULL,
  UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key),
  KEY idx_tenant_created (tenant_id, created_at, task_id)
);

CREATE TABLE notification_delivery (
  delivery_id        BINARY(16) PRIMARY KEY,
  task_id             BINARY(16) NOT NULL,
  tenant_id           BIGINT NOT NULL,
  channel             VARCHAR(32) NOT NULL,
  provider            VARCHAR(64) NOT NULL,
  state               VARCHAR(32) NOT NULL,
  attempt_count       INT NOT NULL DEFAULT 0,
  next_attempt_at     DATETIME(3) NULL,
  provider_request_id VARCHAR(128) NOT NULL,
  provider_message_id VARCHAR(128) NULL,
  lease_until         DATETIME(3) NULL,
  fencing_token       BIGINT NOT NULL DEFAULT 0,
  version             BIGINT NOT NULL DEFAULT 0,
  created_at          DATETIME(3) NOT NULL,
  updated_at          DATETIME(3) NOT NULL,
  UNIQUE KEY uk_provider_request (provider, provider_request_id),
  KEY idx_recovery (state, next_attempt_at, delivery_id),
  KEY idx_task (task_id, delivery_id)
);

CREATE TABLE outbox_event (
  event_id       BINARY(16) PRIMARY KEY,
  aggregate_id   BINARY(16) NOT NULL,
  event_type     VARCHAR(64) NOT NULL,
  payload_ref    VARCHAR(512) NOT NULL,
  status         VARCHAR(32) NOT NULL,
  available_at   DATETIME(3) NOT NULL,
  published_at   DATETIME(3) NULL,
  created_at     DATETIME(3) NOT NULL,
  KEY idx_publish (status, available_at, event_id)
);
```

DDL 是设计草案；字段长度、分区、外键、归档和索引必须通过真实查询计划与负载验证。

## 7. 分区、路由与顺序

- Kafka key 默认使用 `tenantId + channel + recipientShard`，保留局部顺序并避免单租户单分区热点。
- 不承诺跨渠道或跨接收者全局顺序；需要业务版本号拒绝陈旧事件。
- recovery scanner 按 `state + next_attempt_at` 小批领取，并使用 lease/fencing 控制多实例。
- 大租户可进入独立 quota pool 或物理 topic，但只有热点证据出现后再隔离。

## 8. 故障决策表

| 故障 | 立即表现 | 状态策略 | 止血 | 恢复与证明 |
|---|---|---|---|---|
| MySQL 不可用 | 创建失败、状态不可写 | 不返回假成功 | 入口快速失败/限流 | 恢复后核对已提交任务与 Outbox |
| Redis 不可用 | 配额/缓存失效 | 任务事实不丢 | 高风险渠道 fail-closed，低风险小配额降级 | 比较限流拒绝和 Provider 实际调用 |
| Kafka 不可用 | Outbox oldest age 上升 | DB 接收可继续到安全水位 | 达水位后背压入口 | Outbox 全量发布、无越界重复 |
| Worker 崩溃 | lag 上升、in-flight 中断 | 重复消费可吸收 | consumer group 接管 | 按 delivery/provider id 对账 |
| Provider timeout | 调用结果未知 | 进入 UNKNOWN | 停止盲目重试 | 查询/回调/人工确认后推进 |
| Provider 503 | retry 增长 | 有界重试 | circuit breaker + 渠道配额 | 分阶段恢复，计算 drain time |
| 回调重复/乱序 | 终态被陈旧事件冲击 | 条件状态转换 | 去重并记录拒绝原因 | 检查终态单调性 |
| Agent Tool 超时 | 回答证据不完整 | 不推断事实 | 返回不确定 | 展示缺失来源和后续只读查询 |
| prompt injection | 文档要求越权 | 内容不改变权限 | 拒绝动作与敏感输出 | 审计 policy decision 和引用 |

## 9. 配额和恢复预算

建议将总安全 Provider 容量拆分：

```text
70% normal traffic
15% controlled retry
10% reconciliation
5% approved replay
```

比例只是起始设计，必须按渠道和供应商压测校准。单租户还要有独立上限，避免大租户使用全部恢复预算。

积压恢复前计算：

```text
net_drain_rate = safe_capacity - current_incoming_rate
drain_time = backlog / net_drain_rate
```

若 `safe_capacity <= incoming_rate`，仅恢复消费者不会清空积压，必须降入口、扩安全容量或延后低优先级流量。

## 10. 观测与告警

### Dashboard 分层

1. 用户结果：接受率、最终成功率、完成延迟、重复/投诉。
2. 流水线：Outbox age、Kafka lag、retry/DLT/UNKNOWN/replay backlog。
3. 依赖：DB/Redis/Kafka/Provider 延迟、错误、池 pending。
4. 资源：CPU、heap/RSS、GC、线程、连接、容器 restart。
5. Agent：Tool 错误、引用覆盖、权限拒绝、token 和成本。

### 告警原则

- 以用户 SLO、oldest age 和 drain risk 为主，避免只看 CPU。
- 告警附带时间窗、租户/渠道范围、Runbook 和数据正确性查询。
- taskId 进入日志/Trace，不进入 Metrics label。
- 变更、发布、配额调整和 replay 审批进入统一事件时间线。

## 11. 安全矩阵

| 角色 | 读任务 | 读脱敏遥测 | 创建 replay preview | 审批 replay | 执行 replay | 读取 Secret |
|---|---:|---:|---:|---:|---:|---:|
| 租户用户 | 本租户 | 本租户摘要 | 否 | 否 | 否 | 否 |
| 值班工程师 | 授权范围 | 授权范围 | 是 | 否 | 否 | 否 |
| 事故负责人 | 授权范围 | 授权范围 | 是 | 是 | 否 | 否 |
| Replay Executor | 最小字段 | 否 | 否 | 验证审批 | 是 | 仅运行时凭证 |
| Agent Assistant | 授权只读 | 授权脱敏 | 仅建议结构 | 否 | 否 | 否 |

审批者与执行者分离；所有 Tool 和 replay 都要有 correlation id、输入摘要、策略结果和最终结果。

## 12. ADR 清单

至少维护以下 ADR：

1. MySQL 为任务真相源，Redis 只保存可重建状态。
2. 使用 Transactional Outbox，不采用业务库与 Kafka 的假原子双写。
3. 使用至少一次处理 + 幂等/UNKNOWN/对账，不承诺外部副作用 exactly-once。
4. 按渠道隔离 Worker 和配额，避免慢 Provider 全局扩散。
5. Agent 只读，所有高风险操作进入确定性控制面和人工审批。
6. 首版模块化单体 + 独立 Worker，不提前拆分大量微服务。

每条 ADR 写清背景、选择、备选、代价、回退条件和复审触发器。

## 13. 项目答辩证据包

```text
defense-pack/
  one-page-brief.md
  assumptions-and-capacity.xlsx
  architecture.png
  api-and-state-machine.md
  data-model.sql
  adr/
  load-test-report.md
  fault-timeline.md
  correctness-checks.sql
  dashboard-screenshots/
  agent-safety-evaluation.md
  resume-fact-ledger.md
  known-limitations.md
```

目录是交付约定，不代表这些证据已经生成。答辩只展示实际存在、可复核且属于个人完成范围的材料。

## 14. 三轮演进追问

### 流量增长十倍

先验证瓶颈属于 API、数据库写、Outbox、Kafka、Worker、Provider 还是网络。可选动作包括批量写、表/索引优化、分区增加、渠道隔离、异步归档和热点租户拆分。不能只回答“加机器”。

### 扩展到多地域

先澄清数据主权、延迟和容灾目标。首选单写地域 + 灾备或按租户归属地域；跨地域主动主动会引入幂等键冲突、顺序、回调路由、复制延迟和故障切换脑裂问题。

### Agent 能否自动恢复

先按副作用和可逆性分级。只读查询可以自动；低风险、确定性、可回滚动作可在严格策略下逐步试点；批量 replay、扩容消费者、offset reset、删除资源和权限变更仍需要 preview、审批、配额、审计和 kill switch。

## 15. 完成定义

项目设计只有在以下证据齐全后才可进入答辩候选：

- API、状态机、DDL 和 ADR 版本一致。
- 容量假设被真实负载校准并注明环境。
- 至少覆盖重复、崩溃、timeout、积压恢复和越权五类实验。
- 每次故障有注入点、停止条件、时间线、正确性查询和限制。
- 答辩描述与事实台账一致，不把独立项目包装成实习经历。

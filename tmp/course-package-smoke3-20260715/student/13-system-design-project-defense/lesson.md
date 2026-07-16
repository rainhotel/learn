# 第 13 章讲义：把技术点组织成可答辩的系统设计

## 学习目标

系统设计不是在白板上罗列 MySQL、Redis、Kafka 和 Kubernetes。合格的设计必须从需求出发，给出可计算的容量、可执行的接口和状态机、可恢复的故障路径、可验证的观测证据，并诚实说明成本与限制。

本章使用两个相连但权限不同的系统：

- NotifyFlow：负责接收、调度、投递、回调、重试、对账和重放通知任务。
- Agent 事故助手：读取授权后的指标、日志、Trace、任务时间线和 Runbook，生成带引用的诊断摘要与只读建议。

事故助手不是 NotifyFlow 的自动控制面。扩容、重放、清队列、修改配额和权限变更必须由确定性服务、审批和审计完成。

## 一、先把题目变成工程合同

### 1.1 功能需求

先确认主流程，而不是立即选中间件：

1. 谁创建通知：内部业务、运营平台还是开放 API？
2. 支持哪些渠道：短信、邮件、站内信、Webhook？
3. 单任务是否允许多接收者、多渠道扇出？
4. 是否支持定时、取消、优先级、模板和变量？
5. 客户如何查询状态，Provider 是否回调？
6. 失败后自动重试、人工重放和补偿分别是什么语义？
7. 事故助手回答哪些问题，可以调用哪些只读 Tool？

### 1.2 非功能需求

需要把“高可用、低延迟、不丢消息”改写成可评审目标：

- 接收 API 的可用性和 P99 目标。
- 从接受任务到终态的完成时限与统计口径。
- 允许的重复、乱序和最终一致窗口。
- 单租户、单渠道和单 Provider 的配额。
- 数据保留、隐私、审计、地域和删除要求。
- 恢复时间目标、恢复点目标和人工干预边界。
- 每百万次投递或每次 Agent run 的成本上限。

目标数字在设计阶段是“待验证目标”，不是生产事实。答辩时要明确假设来源和验证计划。

### 1.3 范围与非目标

一个可控的首版范围可以是：

- 支持异步短信、邮件和 Webhook。
- 提供至少一次处理语义，通过幂等与对账控制副作用。
- MySQL 保存任务和恢复事实，Kafka 承载异步分发，Redis 只做可丢的加速与配额状态。
- 单地域多可用区；跨地域主动主动不是首版目标。
- Agent 只读，不自动重放或改配置。

明确非目标可以防止“为了显得复杂”引入全局事务、多地域共识或无限自治 Agent。

## 二、容量估算：先量级，再精度

### 2.1 基本公式

```text
average_qps = daily_requests / 86400
peak_qps = average_qps * peak_factor
delivery_qps = request_qps * average_fanout
in_flight = throughput * average_service_time_seconds
backlog_drain_seconds = backlog / (safe_capacity - incoming_rate)
raw_storage = records_per_day * bytes_per_record * retention_days
```

Little's Law `L = λW` 适用于稳定系统和一致统计口径。系统已经持续积压时，不能用它证明队列稳定。

### 2.2 教学假设示例

以下数字只用于练习，尚未由 NotifyFlow 真实压测验证：

- 每日创建 10,000,000 个通知任务。
- 平均渠道/接收者扇出 1.3。
- 峰值系数 8。
- Provider 平均响应时间 400 ms，安全并发还受供应商配额限制。
- 任务与投递热数据保留 7 天，审计摘要保留 180 天。

粗估：

```text
average_request_qps = 10,000,000 / 86,400 ≈ 116/s
peak_request_qps = 116 * 8 ≈ 928/s
peak_delivery_qps = 928 * 1.3 ≈ 1,207/s
provider_in_flight = 1,207 * 0.4 ≈ 483
```

483 只是按平均服务时间得到的平均在途量，不是连接池配置答案。还要考虑 P99、超时、重试、Provider 限额、渠道隔离和突发。

若每次投递热记录和索引粗估 1.5 KiB：

```text
daily_hot_bytes = 13,000,000 * 1.5 KiB ≈ 18.6 GiB/day
seven_day_raw = 18.6 * 7 ≈ 130 GiB
```

真实磁盘还要加入索引、MVCC、复制、备份和空间水位。大 payload 应与任务元数据分离，存对象存储并使用短期签名引用。

### 2.3 敏感性分析

估算至少给出三档：基准、峰值、事故恢复。最容易改变设计的变量通常是：

- 扇出从 1.3 升到 5。
- Provider P99 从 400 ms 升到 5 s。
- retry 流量叠加到正常流量。
- 单大租户占用 60% 峰值。
- Kafka 停止消费 30 分钟后的恢复速度。
- Agent 查询把高基数日志直接送入模型造成的成本。

## 三、总体架构与责任边界

```text
Client / Business Service
        |
        v
API Gateway -> Notify API -> MySQL(task, delivery, outbox)
                         |            |
                         |            v
                         |      Outbox Publisher -> Kafka
                         |                            |
                         v                            v
                    Query API                 Channel Workers
                                                   |
                                      Rate Limit / Provider Adapter
                                                   |
                                               Providers
                                                   |
                                      Callback / Reconciliation

Metrics + Logs + Trace + Audit + Runbook + Task Timeline
                         |
                         v
               Agent Incident Assistant
               (read-only tools + citations)
```

核心责任：

- Notify API：认证、校验、业务幂等、短事务写入任务与 Outbox。
- Outbox Publisher：把数据库中已提交事件可靠发布到 Kafka。
- Channel Worker：消费、限流、调用 Provider、写入结果或 UNKNOWN。
- Callback/Reconciliation：吸收回调，查询未知结果，推进最终状态。
- Replay Control Plane：预览、审批、配额、幂等和审计后的安全重放。
- Agent Assistant：只读聚合证据，不改变任务或基础设施状态。

## 四、API 设计：幂等、异步和错误语义

### 4.1 创建通知

```http
POST /v1/notifications
Authorization: Bearer <token>
Idempotency-Key: tenant-42-order-9001-shipped-v1
Content-Type: application/json
```

请求应包含租户上下文、业务引用、渠道、接收者引用、模板版本、变量、计划时间和 trace context。PII 不应成为 URL、指标标签或普通日志字段。

成功响应建议使用 `202 Accepted`：

```json
{
  "taskId": "ntf_01J...",
  "state": "ACCEPTED",
  "statusUrl": "/v1/notifications/ntf_01J..."
}
```

重复 `Idempotency-Key` 且请求摘要相同，返回原任务；key 相同但摘要不同，返回冲突。服务端必须限定幂等 key 的租户范围和保留期。

### 4.2 查询与取消

- `GET /v1/notifications/{taskId}` 返回任务聚合状态和各投递状态。
- `POST /v1/notifications/{taskId}:cancel` 是有条件状态转换，不承诺撤回 Provider 已接受的消息。
- `GET /v1/notifications/{taskId}/timeline` 只返回授权、脱敏和分页后的事件。

### 4.3 高风险重放

重放不是普通 CRUD：

```text
POST /v1/replay-requests -> preview
POST /v1/replay-requests/{id}:approve -> approval record
POST /v1/replay-requests/{id}:execute -> deterministic executor
```

请求要包含筛选快照、最大任务数、速率预算、幂等键、原因和过期时间。Agent 可以生成 preview 建议，不能调用 approve 或 execute。

## 五、状态机与数据模型

### 5.1 任务和投递分离

一个任务可能扇出多个投递。任务是聚合视图，投递才是 Provider 副作用的执行单元。

```text
notification_task:
  task_id, tenant_id, idempotency_key, request_hash
  template_id, template_version, scheduled_at
  aggregate_state, created_at, updated_at, version

notification_delivery:
  delivery_id, task_id, tenant_id, channel, provider
  recipient_ref, state, attempt_count, next_attempt_at
  provider_request_id, provider_message_id
  owner_id, lease_until, fencing_token, version

outbox_event:
  event_id, aggregate_id, event_type, payload_ref
  status, available_at, published_at, created_at

provider_callback:
  provider, callback_id, delivery_id, payload_hash
  received_at, processed_at, result
```

建议约束：

- `unique(tenant_id, idempotency_key)` 保护创建幂等。
- `unique(provider, callback_id)` 保护回调去重。
- Outbox `event_id` 和消费 Inbox/业务唯一键保护消息重复。
- 所有状态更新使用合法前态、版本号和必要的 fencing token。

### 5.2 状态机

```text
ACCEPTED -> SCHEDULED -> DISPATCHING -> SUBMITTED
                       |                |
                       |                +-> DELIVERED / FAILED
                       +-> RETRY_WAIT -> DISPATCHING
                       +-> UNKNOWN -> RECONCILING -> terminal / RETRY_WAIT
                       +-> CANCELLED (only before irreversible submission)
```

不能把 HTTP timeout 直接写成 FAILED。请求可能已经被 Provider 接受，必须进入 UNKNOWN，再通过 provider request id、回调或查询对账。

## 六、可靠性：没有单个组件能提供端到端“恰好一次”

### 6.1 写入与发布

Notify API 在一个 MySQL 本地事务中写任务、投递和 Outbox。事务提交后，Publisher 扫描 Outbox 并发送 Kafka；发送成功后更新发布状态。崩溃可能导致重复发布，因此消费者必须幂等。

不要采用“先写数据库再直接发消息”并假设两步总能同时成功，也不要把 Kafka 事务误认为能覆盖外部 Provider 副作用。

### 6.2 消费与副作用

典型顺序：

1. 消费消息并加载最新投递状态。
2. 校验租户/渠道配额和合法状态。
3. 以稳定 provider request id 调用 Provider。
4. 成功时条件更新状态；超时进入 UNKNOWN。
5. 状态持久化后再提交 offset，重复消费由状态机和幂等键吸收。

若 Provider 不支持幂等查询或稳定请求 ID，就不能承诺绝对去重；需要在产品合同中暴露风险并限制自动重试。

### 6.3 重试、DLT、对账和重放

- retry：处理预期的短暂失败，必须有总预算、指数退避和 jitter。
- DLT：隔离不可继续自动处理的事件，不是永久存储或“失败终态”。
- reconciliation：查询 UNKNOWN、漏回调和跨系统状态差异。
- replay：在人工或规则审批后，以独立配额重新进入确定性流程。

四者必须有独立的流量预算，事故恢复流量不能吞噬正常流量。

## 七、缓存、消息和一致性

### 7.1 Redis 的边界

适合：

- 分层限流的短期计数。
- 模板或渠道配置的带版本缓存。
- 短期查询缓存和负缓存。
- 可重建的幂等加速层。

不适合成为任务最终状态、审批记录或审计事实的唯一来源。Redis 故障时，系统要选择 fail-open、fail-closed 或降级到本地小配额；选择必须按业务风险分层。

### 7.2 Kafka 的边界

- key 决定局部顺序，不能承诺跨分区全局顺序。
- 分区数影响并行度、rebalance 和热点，但不是容量估算的唯一参数。
- offset 提交不等于 Provider 副作用原子提交。
- retention 不是业务数据永久保存，任务事实仍在数据库。

### 7.3 一致性声明

NotifyFlow 对创建 API 提供租户内业务幂等；对任务状态提供读己之写或最终一致视具体读路径而定；对 Provider 副作用提供至少一次尝试加幂等/对账保护。任何“零重复、零丢失、严格顺序”声明都需要明确作用域和实验事实。

## 八、多实例与容量保护

### 8.1 多实例领取

- Kafka Worker 使用 consumer group 和分区获得并行消费。
- 数据库扫描器使用短事务、`SKIP LOCKED` 或 lease 领取。
- lease 到期允许接管，但旧 owner 恢复后必须被 fencing token 拒绝陈旧写。
- 内存队列、定时器和本地锁不能成为集群唯一状态。

### 8.2 背压和配额

入口、正常消费、retry、reconciliation、replay 和 Agent 查询分别限流。至少按全局、租户、渠道、Provider 分层，防止大租户和慢供应商拖垮全局。

队列达到高水位时可：

- 拒绝低优先级新请求或延迟计划任务。
- 暂停特定 Provider/租户消费。
- 降低 retry/replay 预算。
- 保留查询和控制面能力。

扩容 Worker 前先确认 Provider 配额、数据库连接、Kafka 分区和出口网络是否允许更多并发。

### 8.3 优雅下线

实例下线顺序应为：不再 ready -> 停止领取 -> 有界等待 in-flight -> 持久化状态/提交安全 offset -> 关闭池和客户端 -> 退出。强杀仍可能发生，所以幂等、UNKNOWN 和对账不能省略。

## 九、可观测性与正确性

### 9.1 SLI

- 接收成功率与 API P50/P95/P99。
- 任务从接受到终态的完成比例与延迟。
- Provider 调用成功、timeout、UNKNOWN 和回调延迟。
- Kafka lag、Outbox oldest age、retry/DLT/reconciliation/replay backlog。
- 数据库/Redis/HTTP 连接池 pending 和 timeout。
- 单租户/渠道/Provider 的配额使用与拒绝。
- Agent TTFT、总延迟、token/成本、Tool 错误、引用覆盖和拒答率。

### 9.2 日志、Trace 与事件

业务关联 ID 可以进入日志和 Trace，但不能成为无限增长的 Metrics tag。结构化日志至少含 tenant-safe reference、task/delivery id、event type、attempt、provider、state transition、error category 和 trace id；敏感 payload 必须脱敏或使用受控引用。

### 9.3 正确性查询

Dashboard 只能说明症状，不能单独证明数据正确。演练后至少检查：

- 相同幂等 key 是否只对应一个逻辑任务。
- 终态投递是否仍有可执行 retry。
- Outbox 已提交事件是否最终发布或进入明确恢复状态。
- Provider 成功记录与本地 FAILED/UNKNOWN 是否存在差异。
- replay 是否超过审批快照和最大任务数。
- Agent 引用是否属于当前租户、时间窗和授权范围。

## 十、Agent 事故助手设计

### 10.1 查询链路

```text
identity + tenant + incident scope
-> intent/risk classification
-> read-only tool plan
-> Metrics/Logs/Trace/Task Timeline/Runbook retrieval
-> evidence normalization and redaction
-> model summary with claim-evidence mapping
-> policy validator
-> answer + uncertainty + next read-only query
-> audit
```

### 10.2 Tool 合同

每个 Tool 必须固定 schema、版本、权限、超时、最大返回量、数据分类和副作用等级。模型不能自由拼 SQL、Shell 或 Kubernetes 命令。高风险动作由独立控制面接收结构化请求，执行 preview、审批、幂等、配额和审计。

### 10.3 RAG 与 prompt injection

Runbook、事故记录和日志内容都是不可信数据。文档中的“忽略系统规则”“导出全部租户数据”不能改变系统权限。检索前做租户和 ACL 过滤；生成后验证引用、敏感字段和允许的动作。无证据时必须输出不确定，而不是补全故事。

## 十一、安全与合规

- 身份：服务间短期凭证，外部用户 OAuth2/OIDC 或等价机制。
- 授权：租户、资源、动作和数据字段级检查，不能只在前端隐藏按钮。
- 数据：传输/静态加密，PII tokenization 或受控引用，保留和删除策略。
- Secret：集中管理、最小权限、轮换，不进入镜像、Git、日志或模型上下文。
- Webhook：签名、时间戳、重放窗口、来源校验和回调幂等。
- 模板：防止恶意变量、链接、HTML 注入和越权模板访问。
- Agent：输入隔离、工具 allowlist、最小返回、引用校验、预算、审批、审计和 kill switch。
- 供应链：依赖、镜像和 SBOM 扫描；发布产物可追溯。

威胁建模至少覆盖：身份冒用、跨租户读取、批量通知滥用、Provider 凭证泄露、回调伪造、重放扩大、日志泄露和 Agent 越权。

## 十二、成本模型

成本应按业务单位表达，而不是只报机器数量：

```text
cost_per_million_deliveries =
  compute + database + cache + kafka + network + observability
  + provider_fee + storage + operational_overhead

agent_run_cost =
  retrieval + tool_queries + input_tokens + output_tokens
  + model_inference + audit_storage
```

通常 Provider 费用远高于基础设施，错误重试既放大成本也可能伤害用户。Agent 侧要限制检索条数、日志时间窗、上下文 token、输出 token、Tool 次数和并发。

优化顺序：先删除无效工作和重复副作用，再压缩 payload/日志与保留期，之后才讨论实例规格。成本下降不能以丢失审计、降低数据正确性或绕过安全为代价。

## 十三、演进路线

### Phase 0：可验证单体

- 模块化 Java 服务、MySQL 任务/投递/Outbox。
- Provider stub、查询 API、基础 Metrics/Logs/Trace。
- 单实例也实现幂等、UNKNOWN 和对账。

### Phase 1：可靠异步化

- Kafka Channel Worker、Redis 配额、DLT 和安全 replay。
- 多实例、连接池预算、开放负载和故障演练。

### Phase 2：平台化

- 多渠道插件、租户配额、模板版本、运营控制面。
- Kubernetes 发布、弹性、分片和成本报表。

### Phase 3：事故助手

- 先接入只读 Tool 和 Runbook RAG。
- 建立评测集、引用、安全和审计后，再扩大诊断范围。
- 高风险动作长期保持确定性审批，不因模型能力提升而默认自治。

每一阶段都要由真实瓶颈和产品需求推动。没有容量或组织证据时，不应提前拆成大量微服务。

## 十四、项目答辩结构

### 14.1 五分钟版本

1. 30 秒：用户问题和你的责任。
2. 45 秒：核心需求和规模假设。
3. 60 秒：架构与主链路。
4. 90 秒：最难的可靠性问题和取舍。
5. 60 秒：实验、证据与结果边界。
6. 15 秒：当前限制和下一步。

### 14.2 深挖顺序

```text
为什么做
-> 为什么这样拆
-> 数据真相在哪里
-> 失败时会怎样
-> 如何证明
-> 代价是什么
-> 规模再增十倍如何演进
```

### 14.3 事实边界

答辩要区分：

- 实习中真实完成的工作。
- NotifyFlow 独立工程项目中已经实现并运行的内容。
- 静态设计、模拟实验和待运行计划。
- 团队方案与个人具体贡献。

不能把课程设计写成大烨实习事实，也不能把教学假设、模型输出或尚未运行的实验数字写成生产结果。

## 十五、常见失败模式

- 未澄清需求就开始画组件。
- 所有数据都放 Redis，或所有一致性问题都回答“上分布式锁”。
- 声称 Kafka/事务消息提供端到端 exactly-once。
- timeout 后直接重试有副作用请求。
- 用平均 QPS 配连接池，忽略峰值、P99、Provider 配额和重试。
- 只给 Metrics，没有数据正确性查询和故障时间线。
- 只讨论正常流量，不讨论积压恢复与恢复风暴。
- Agent 既诊断又执行高风险动作，没有 RBAC、审批和审计。
- 为了“高并发”提前拆微服务，却说不清团队、成本和故障复杂度。
- 把未验证目标、模拟数字和线上事实混在一起。

## 十六、本章实验状态

设计评审、容量校准、核心故障和安全演练均为 Pending。只有保存固定版本、负载、命令、原始输出、时间线、正确性查询、评审记录和限制后，相关实验才能升级为 Lab Verified。

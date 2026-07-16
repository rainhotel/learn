# 可观测性、压测与故障注入

## 课程信息

- 所属模块：可靠通知主链路
- 难度：深入
- 建议时长：24-32 小时
- 先修章节：第 1-8 章
- 对应项目里程碑：NotifyFlow 形成可证明的容量、可靠性和事故恢复证据
- 对应岗位能力：可观测性、性能工程、容量规划、生产排障、混沌与故障演练

## 学习目标

1. 从用户目标推导 SLI、SLO、error budget 和告警。
2. 设计低基数、可聚合、可诊断的 metrics/logs/traces。
3. 正确解释吞吐、并发、平均值、P95/P99 和饱和度。
4. 选择开放或封闭负载，避免 coordinated omission。
5. 设计可重复的 baseline、load、stress、spike、soak 和 recovery 测试。
6. 用受控故障注入验证超时、重试、降级、恢复和数据一致性。
7. 使用 JFR 和 JVM/线程池/连接池指标缩小性能问题范围。
8. 输出可用于项目答辩和简历的证据链。

## 一、为什么“有监控”仍可能不可观测

系统可能有上千个指标，却回答不了：

- 用户提交通知后多久开始执行？
- 已返回 200 的任务是否最终送达？
- 当前延迟上升来自数据库、Kafka、线程池还是供应商？
- lag 上升是突发流量还是 Consumer 变慢？
- 重试使成功率变高，还是只把下游打得更慢？
- 一次配置发布后到底影响了哪些租户和渠道？

可观测性不是数据数量，而是能否从外部输出推断系统内部状态，并快速验证假设。

## 二、从用户目标定义 SLI/SLO

### 2.1 定义

- SLI：实际测量的服务水平指标。
- SLO：SLI 在某个窗口内的目标。
- SLA：对外承诺及可能的商业后果。
- Error budget：`1 - SLO` 允许的失败空间。

### 2.2 NotifyFlow 示例

```text
SLI：任务创建成功率
SLO：滚动 30 天 >= 99.95%

SLI：任务从 ACCEPTED 到首次供应商尝试的延迟
SLO：P99 <= 30 秒

SLI：合法通知在业务 deadline 内进入明确终态的比例
SLO：>= 99.9%
```

第三个目标比“HTTP 200 比例”更接近用户结果。

### 2.3 Error budget

若月请求量 1000 万，SLO 99.9%：

```text
budget = 10,000,000 * (1 - 0.999) = 10,000 次
```

预算消耗过快时，可暂停高风险发布、增加修复投入或降低变更频率。

SLO 不应机械设为 100%。100% 既难以准确测量，也会消除发布速度与可靠性之间的风险交换空间。Error budget 如果不影响发布和优先级决策，就只是另一个 Dashboard 数字。

### 2.4 多窗口告警

单一分钟错误率会噪声很大，单月平均又太迟钝。实践中组合短窗口快速发现和长窗口确认持续消耗，告警应尽量面向用户症状。

## 三、四个黄金信号

### 3.1 Latency

- 成功与失败请求分开。
- 创建 API 延迟与异步最终完成延迟分开。
- 平均值、P50、P95、P99 同时理解。

快速返回 500 不应降低“总体平均延迟”后让仪表盘变绿。

### 3.2 Traffic

- API 请求/s。
- 创建任务/s、接收方/s。
- Kafka 消息/s。
- 供应商调用/s。
- Agent 请求/s 和 token/s。

业务复杂度不同，不能只按 HTTP 请求数比较容量。一个包含 10000 接收方的批次和单接收方请求成本不同。

### 3.3 Errors

- HTTP 5xx、验证失败。
- 业务状态失败。
- 超过 deadline。
- 返回 200 但任务没有落库或最终状态错误。
- 引用错误、越权工具调用等 Agent 质量错误。

### 3.4 Saturation

- CPU、内存和 GC。
- 线程池 active/queue/rejected。
- Hikari active/pending/timeouts。
- Kafka lag、Partition 热点。
- Redis 内存和 evictions。
- MySQL connections、lock waits、buffer pool、磁盘。

系统通常在资源达到 100% 前已经显著退化，P99 上升常是早期信号。

## 四、RED 与 USE

### 服务 RED

- Rate：请求或事件速率。
- Errors：错误比例。
- Duration：耗时分布。

### 资源 USE

- Utilization：资源忙碌比例。
- Saturation：等待或排队程度。
- Errors：资源错误。

黄金信号面向服务体验，RED/USE 帮助从服务向资源层缩小范围。

## 五、Metric 类型

### 5.1 Counter

只增不减的累计事件：

```text
notifyflow_task_created_total
notifyflow_delivery_attempt_total{provider,result}
notifyflow_retry_total{provider,category}
```

通常关注 rate，而不是进程启动以来的绝对值。

### 5.2 Gauge

可升可降的当前值：

```text
outbox_pending
thread_pool_queue_size
unknown_attempts
```

Gauge 只表示采样瞬间，不能用于可靠累计事件。

### 5.3 Timer/Histogram

记录 count、总耗时和分布：

```text
notifyflow_provider_request_duration
notifyflow_task_first_attempt_delay
notifyflow_reconciliation_duration
```

跨实例聚合百分位时优先使用可聚合 histogram bucket，不直接平均各实例 P99。

### 5.4 Long Task Timer

适合当前仍在执行的长任务数量和持续时间，例如 replay batch、批量导入和长时间对账。

## 六、基数控制

错误示例：

```text
tag taskId=900001
tag userId=123
tag phone=138...
tag traceId=...
tag exceptionMessage=完整文本
```

这些 tag 产生近乎无限的时间序列，并带来隐私风险。

适合低基数 tag：

- channel：sms/email/wechat。
- provider：sms-a/sms-b。
- result：success/transient/permanent/unknown。
- tenant_tier：free/pro/enterprise，而不是 tenantId。
- exception_class：经过白名单归类的类型。

高基数标识进入日志或 Trace，通过 exemplar/traceId 从指标跳转到单次请求。

## 七、Spring Boot 与 Micrometer

Spring Boot 4.1.0 自动配置 Micrometer 1.17.0 的 composite `MeterRegistry`。原则：

- 注入 Spring 管理的 `MeterRegistry`。
- 一组指标使用 `MeterBinder` 封装。
- 使用 `MeterFilter` 拒绝危险 tag 或重命名 meter。
- HTTP Server 默认指标名为 `http.server.requests`。
- Observation 可同时驱动指标与 Trace，但 tag 设计仍需审查。

Actuator endpoint bean 的存在与是否通过 HTTP/JMX 暴露是两个不同问题。生产环境不能直接开放全部管理端点，应显式配置 exposure、认证授权、独立管理端口和网络边界。

示意：

```java
Timer providerTimer = Timer.builder("notifyflow.provider.request")
        .tag("provider", providerCode)
        .tag("result", resultCategory)
        .publishPercentileHistogram()
        .register(meterRegistry);
```

不要在每次请求动态创建包含 taskId 的 Timer。

## 八、Logs、Traces 与 Baggage

### 8.1 结构化日志

推荐字段：

```json
{
  "timestamp": "...",
  "level": "ERROR",
  "service": "notify-worker",
  "traceId": "...",
  "eventId": "...",
  "taskId": "...",
  "tenantIdHash": "...",
  "provider": "sms-a",
  "attemptId": "...",
  "failureCategory": "UNKNOWN",
  "errorCode": "TIMEOUT"
}
```

日志需要脱敏、采样、保留期和访问控制。

### 8.2 Trace

Trace 表示请求跨组件的路径：

```text
HTTP create task
 -> MySQL transaction
 -> Outbox publish
 -> Kafka consume
 -> Provider HTTP
 -> result transaction
```

异步边界要显式传播 trace context 或使用 link，不能假设线程切换后上下文自动存在。

OpenTelemetry 负责生成、处理和导出 telemetry，不负责自动提供长期存储、查询和 Dashboard；仍需要后端系统。多个 signal 只有在 context 正确传播时才能可靠关联。

### 8.3 Baggage

Baggage 是跨信号传播的上下文。仅放小型、允许传播的字段；不能放 token、手机号、完整 prompt 或大量业务对象。

## 九、为什么只看平均值危险

样本：99 个请求 10 ms，一个请求 10 s。

```text
average ≈ 110 ms
P99/P100 暴露长尾
```

对批量通知，一个批次内只要部分接收方特别慢，整体完成时间就受长尾支配。必须按用户等待方式选择 percentile。

百分位也需要样本量和窗口。每分钟只有 20 个请求时，P99 不稳定；应结合更长窗口、直方图和原始样本。

## 十、压测目标与类型

| 类型 | 目标 |
|---|---|
| Baseline | 验证低负载正确性和基准开销 |
| Load | 验证预期峰值下 SLO |
| Stress | 找到拐点和失效模式 |
| Spike | 验证突发、排队和限流 |
| Soak | 发现泄漏、累积和长时间抖动 |
| Recovery | 验证停机/积压后的追赶 |
| Capacity | 确定安全容量和扩容阈值 |

每种测试的退出标准不同，不能只写“压测 1000 并发”。

## 十一、开放与封闭负载

### 11.1 封闭模型

固定 VU 完成一次 iteration 后再开始下一次。系统变慢时，发出的请求自然减少。

适合：固定用户数、用户必须等待上一步完成的交互。

### 11.2 开放模型

按独立到达率发起 iteration，不受上一请求耗时控制。

适合：固定 QPS 的 API、Webhook、消息流、定时任务峰值。

### 11.3 Coordinated omission

如果目标是 1000 RPS，但系统变慢后封闭模型只发出 300 RPS，测试绕开了最拥堵时本应到达的 700 RPS，延迟结果会过于乐观。

k6 开放模型可使用 `constant-arrival-rate` 或 `ramping-arrival-rate`。

## 十二、压测方法

### 12.1 固定变量

- 代码 commit 和配置。
- JVM 参数。
- 数据库数据量和索引。
- Topic/Partition。
- 机器资源。
- 数据集和请求比例。
- 是否预热连接、JIT 和缓存。

### 12.2 分阶段

```text
warm-up -> steady state -> peak -> fault -> recovery -> cool-down
```

不要把 warm-up 数据直接与稳态混合。

### 12.3 Workload mix

NotifyFlow 示例：

```text
60% 单接收方实时任务
25% 100 人批次
10% 定时任务
5% 查询和取消
```

按生产分布构造，不能只压最简单接口。

### 12.4 Threshold

k6 threshold 把目标编码为机器可判定条件：

```javascript
thresholds: {
  http_req_failed: ['rate<0.001'],
  http_req_duration: ['p(95)<300', 'p(99)<800'],
}
```

未满足条件时测试失败，避免人工只挑好看的曲线。

k6 threshold 失败会产生非零退出码，可进入 CI 门禁；但它只能证明本次环境和样本没有达到条件，不能单独外推生产容量。

## 十三、吞吐、并发与 Little's Law

稳态近似：

```text
L = λW
```

- L：系统平均在途请求数。
- λ：平均到达率。
- W：平均停留时间。

若 1000 req/s，平均端到端 200 ms：

```text
L = 1000 * 0.2 = 200 concurrent requests
```

这不是线程池大小公式。阻塞比例、下游连接、队列、CPU 和长尾都会改变实际资源需求。

## 十四、容量拐点

逐级增加负载，观察：

- 吞吐是否继续线性增长。
- P95/P99 是否突然上升。
- 队列和 pending connection 是否累积。
- CPU、GC、锁等待、I/O 是否成为瓶颈。
- 错误率和 timeout 是否增加。

安全容量不等于最大吞吐，应该在拐点前保留故障和恢复余量。

## 十五、NotifyFlow 指标地图

### API

- create rate、error rate、duration。
- idempotency conflict。
- batch recipient count distribution。

### Outbox

- pending、oldest age、publish rate/error、lease expired。

### Kafka

- produce error/latency、consumer lag、rebalance、DLT rate。

### Worker

- active、queue、rejected、processing duration。

### Provider

- request duration、429/5xx、timeout、Unknown、circuit state。

### Recovery

- retry rate、budget exhausted、reconciliation age、replay progress。

### 业务 SLI

- accepted-to-first-attempt。
- accepted-to-terminal-state。
- deadline success ratio。
- duplicate visible effect。

## 十六、Agent/RAG 指标

- request latency、first-token latency。
- input/output token。
- tool call count、error、timeout。
- retrieval hit、MRR/nDCG 或证据命中率。
- answer citation coverage。
- abstain rate、unsupported answer rate。
- cost per request。
- prompt/model/version。

用户 ID、完整 prompt 和原文档内容不作为 metric tag。

## 十七、故障注入原则

故障注入不是随机破坏。每次实验必须写清：

- 假设。
- 注入点。
- 爆炸半径。
- 监控和预期告警。
- 停止条件。
- 恢复步骤。
- 数据一致性检查。

先在本地/测试环境验证，再逐步扩大。

## 十八、故障场景

### 数据库慢

- 注入延迟或慢 SQL。
- 观察连接池 pending、P99、线程池和 timeout。
- 验证是否保护数据库，而不是无限排队。

### 供应商 503/429

- 观察 retry rate、circuit、Unknown 和 lag。
- 验证单点重试预算和分阶段恢复。

### Kafka/Consumer 停止

- 观察 lag、Outbox age 和恢复曲线。
- 验证 retention 与净追赶能力。

### Redis 不可用

- 验证限流/缓存降级策略和数据库压力。
- 不允许 Redis 故障破坏最终业务正确性。

### JVM/GC 压力

- 分配压力、锁竞争或阻塞线程。
- 结合 GC、线程 dump、JFR 和请求长尾定位。

## 十九、JFR

JDK 21 JFR API 支持：

- 自定义业务事件。
- 配置事件和 Recording。
- Event Streaming 实时处理。
- 解析 recording 文件。

NotifyFlow 可定义：

```text
ProviderCallEvent
OutboxPublishEvent
ReplayBatchEvent
```

JFR 是诊断证据的一部分，不替代用户 SLI 和分布式 Trace。

## 二十、实验报告

报告必须包含：

1. 目标和 SLO。
2. 环境、版本、资源和数据规模。
3. 负载模型与请求分布。
4. Threshold 和停止条件。
5. 指标曲线和原始输出。
6. 拐点与瓶颈证据。
7. 故障和恢复时间线。
8. 数据正确性检查。
9. 限制与不可外推范围。
10. 对代码、配置、容量和告警的修订。

## 二十一、常见错误

- 只看平均延迟。
- 系统变慢时压测工具自动降 QPS，却仍声称支持目标吞吐。
- 只压空数据库或简单请求。
- 指标 tag 放 taskId 导致基数爆炸。
- 告警面向 CPU，而不是用户症状。
- 故障注入没有停止条件和数据校验。
- 只记录故障，不验证恢复后的积压追赶。
- 把一次本机结果外推成生产容量。
- Agent 自动总结曲线，却没有引用原始指标和时间窗口。

## 二十二、章节作业

- 目标：为 NotifyFlow 完成一次“稳态负载 → 供应商故障 → 熔断 → 分阶段恢复”的证据链。
- 提交物：SLI/SLO、指标字典、Dashboard、k6 脚本、JFR/线程证据、故障时间线、压测报告和 15 分钟答辩。
- 验收：负载模型正确、threshold 可机器判断、故障可重复、恢复有数据正确性检查。
- 加分：Agent 基于指标和 Runbook 生成带引用的事故摘要，但不替代人工结论。

## 本章小结

- 可观测性从用户目标和可验证问题开始。
- 四个黄金信号是 latency、traffic、errors、saturation。
- Metrics 用于聚合，logs 记录事件，traces 展示请求路径，JFR 诊断 JVM 内部。
- 指标 tag 必须低基数，高基数 ID 放日志/Trace。
- 固定到达率应使用开放负载，避免 coordinated omission。
- Threshold 把 SLO 变成压测通过/失败条件。
- 安全容量应位于性能拐点之前，并保留故障恢复余量。
- 故障注入必须控制爆炸半径、停止条件和数据正确性。

## 版本记录

- v0.1，2026-07-14：完成官方资料核验和完整内容初稿；实验 Pending。

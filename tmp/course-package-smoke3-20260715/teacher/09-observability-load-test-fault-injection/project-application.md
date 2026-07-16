# NotifyFlow 项目应用：证据驱动的可靠性工程

## 1. 目标

把 NotifyFlow 的可靠性从“设计说明”变成可观测、可压测、可复现、可审计的证据：

- 用户结果是否满足 SLO。
- 哪个组件首先饱和。
- 目标峰值下是否保持正确性。
- 故障时告警是否及时。
- 保护和恢复策略是否实际生效。
- 恢复后是否存在重复、丢失或状态错误。

## 2. 服务目标

### API SLO

```text
任务创建可用性：30 天 >= 99.95%
任务创建 P95：< 200 ms
任务创建 P99：< 500 ms
```

### 异步执行 SLO

```text
实时任务 accepted -> first attempt P99 < 30 s
合法任务在 deadline 内进入明确终态 >= 99.9%
无法确定的 UNKNOWN 在 30 min 内收敛 >= 99%
```

这些是课程起始目标，必须通过业务容量和真实测试修订。

## 3. 指标字典

每个指标必须记录：名称、类型、单位、tag、来源、用途、告警和 owner。

### 3.1 API

| 名称 | 类型 | Tag | 用途 |
|---|---|---|---|
| `notifyflow.task.create` | Timer | result、tenant_tier | API 延迟和错误 |
| `notifyflow.task.created` | Counter | channel、tenant_tier | 创建速率 |
| `notifyflow.idempotency.conflict` | Counter | endpoint | 重复请求 |
| `notifyflow.batch.recipients` | DistributionSummary | channel | 请求复杂度 |

### 3.2 Outbox

| 名称 | 类型 | Tag | 用途 |
|---|---|---|---|
| `notifyflow.outbox.pending` | Gauge | event_type | 当前积压 |
| `notifyflow.outbox.oldest.age` | Gauge | event_type | 最老待发布时间 |
| `notifyflow.outbox.publish` | Timer | result | 发布延迟/错误 |
| `notifyflow.outbox.lease.expired` | Counter | publisher | 崩溃或慢实例 |

`publisher` 只能是有限实例组或可控 ID；实例频繁变化时不要作为长期 tag。

### 3.3 Worker

| 名称 | 类型 | Tag |
|---|---|---|
| `notifyflow.worker.task` | Timer | channel、result |
| `notifyflow.worker.queue.size` | Gauge | pool |
| `notifyflow.worker.active` | Gauge | pool |
| `notifyflow.worker.rejected` | Counter | pool、reason |

### 3.4 Provider

| 名称 | 类型 | Tag |
|---|---|---|
| `notifyflow.provider.request` | Timer | provider、result |
| `notifyflow.provider.error` | Counter | provider、category、code_family |
| `notifyflow.provider.unknown` | Gauge | provider |
| `notifyflow.provider.circuit` | Gauge | provider、state |

### 3.5 Recovery

| 名称 | 类型 | Tag |
|---|---|---|
| `notifyflow.retry` | Counter | provider、category |
| `notifyflow.retry.budget.exhausted` | Counter | provider |
| `notifyflow.dlt.case` | Counter | category、severity |
| `notifyflow.dlt.open` | Gauge | category、severity |
| `notifyflow.reconciliation.age` | Timer | provider、result |
| `notifyflow.replay.batch` | LongTaskTimer | risk |

## 4. 基数预算

允许的 tag 值要有上限：

```text
channel <= 5
provider <= 10
result <= 8
category <= 10
tenant_tier <= 4
pool <= 8
```

禁止：taskId、recipient、requestId、traceId、eventId、prompt、URL 原始 query、完整异常消息。

使用 `MeterFilter` 拒绝未知 provider/category，避免异常代码动态生成无限 tag。

## 5. 日志契约

### 通用字段

```text
timestamp level service environment version
traceId spanId requestId eventId taskId attemptId replayId
tenantHash channel provider result failureCategory errorCode
```

### 规则

- 每个关键状态变化只记录一次业务事件日志。
- 异常堆栈不在高频重试中无限重复。
- 手机号、邮件、token、prompt 和 payload 默认不明文。
- 日志中的时间统一使用 UTC/ISO 8601，展示层转换时区。
- 记录配置和策略版本，便于事故回溯。

## 6. Trace 设计

### 创建链路

```text
POST /tasks
  -> validate
  -> mysql.task+outbox
  -> response
```

### 异步链路

```text
outbox.publish
  -> kafka.produce

kafka.consume
  -> claim task
  -> provider.call
  -> mysql.result+outbox
```

异步消费者使用 message context 创建新 span，并 link 到生产 span。eventId/taskId 放 span attribute 前必须评估采样和后端基数，不能把高基数复制到 metrics。

## 7. Dashboard

### 用户结果面板

- 创建成功率和 P95/P99。
- first attempt delay。
- deadline success ratio。
- UNKNOWN 数量和年龄。

### 主链路面板

- Outbox pending/oldest age。
- Kafka lag/rebalance/DLT。
- Worker queue/rejected。
- Provider P99/429/5xx/circuit。

### 资源面板

- CPU、heap、GC pause、thread count。
- Hikari active/pending/timeout。
- MySQL QPS、lock wait、slow query。
- Redis latency/memory/eviction。

面板按“用户症状 -> 服务组件 -> 资源”顺序组织。

## 8. 告警

### Page

- 创建 API 持续不可用。
- deadline success ratio 快速消耗 error budget。
- Outbox oldest age 超过业务阈值。
- Provider 全故障且保护策略未打开。
- UNKNOWN 或 DLT age 超过处置 SLA。

### Ticket/Dashboard

- 单次 retry 增长。
- 缓慢容量趋势。
- 非核心批处理延迟。

CPU 高但用户无症状时通常先调查，不一定立即叫醒人。

## 9. 压测环境清单

- Git commit、镜像、JDK、JVM 参数。
- CPU/内存/磁盘/网络限制。
- MySQL/Redis/Kafka 版本和配置。
- 数据量、索引统计、缓存状态。
- Topic Partition 和副本。
- 供应商 Stub 行为。
- 指标采样间隔和时间同步。

## 10. 负载模型

### 实时创建

使用开放到达模型，按 RPS 控制：

```text
100 -> 300 -> 600 -> 1000 RPS
每阶段 10 min
```

### 批量任务

固定批次到达率，同时按 recipients 分布：1、10、100、1000、10000。

### 查询用户行为

使用封闭模型模拟固定用户等待查询结果。

### 定时峰值

在整点一次释放大量到期任务，验证 jitter、队列、限流和恢复。

## 11. k6 Threshold 示例

```javascript
export const options = {
  scenarios: {
    create_tasks: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 200,
      maxVUs: 1000,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.001'],
    http_req_duration: ['p(95)<200', 'p(99)<500'],
    dropped_iterations: ['count==0'],
  },
};
```

`dropped_iterations` 表示压测端无法维持目标到达率，不能忽略。

## 12. 数据正确性断言

压测不只看速度：

- 每个成功 create 对应一条 task 和一条 outbox。
- requestId 唯一约束没有重复业务结果。
- 消费重复不产生重复可见副作用。
- task 状态转换合法。
- DLT、UNKNOWN、retry 数与注入故障相符。
- 最终完成数 + 失败数 + 未决数 = 创建数。

## 13. 容量报告

每个负载阶段记录：

```text
arrival rate
completed throughput
error rate
P50/P95/P99
CPU/GC
thread queue/rejected
DB pool pending
Kafka lag
provider concurrency
```

找到第一个不能维持 SLO 的阶段，并区分压测端、应用、数据库、Broker 和供应商瓶颈。

## 14. 故障注入矩阵

| 故障 | 注入 | 预期保护 | 恢复验证 |
|---|---|---|---|
| MySQL 慢 | 延迟/锁等待 | timeout、连接池保护 | pending 清零、无丢任务 |
| Redis 不可用 | 停止/网络失败 | 缓存降级、DB 保护 | 无最终正确性破坏 |
| Kafka 停止 | Broker 不可达 | Outbox 积压 | 恢复后有界追赶 |
| Consumer 停止 | 停实例 | lag 告警 | 净消化速率符合计算 |
| Provider 503 | Stub 故障 | 单点重试、熔断 | 分阶段恢复 |
| Provider timeout | 丢响应 | UNKNOWN 对账 | 不重复副作用 |
| Worker queue 满 | 降线程/慢任务 | 拒绝/背压 | 无 OOM、恢复可控 |
| JVM allocation | 分配压力 | GC 指标告警 | JFR 定位热点 |

## 15. 故障实验模板

```text
Hypothesis:
Steady state:
Fault:
Blast radius:
Expected detection:
Expected protection:
Abort condition:
Recovery steps:
Correctness checks:
Actual timeline:
Conclusion:
```

## 16. JFR 事件

建议自定义事件字段：

```text
ProviderCallEvent:
  provider, result, duration, timeoutPhase

OutboxPublishEvent:
  eventType, batchSize, duration, result

ReplayBatchEvent:
  risk, itemCount, rate, result
```

不要写入手机号、完整 payload 或 token。

## 17. Agent 事故助手

只读输入：

- Dashboard 时间窗口。
- 指标查询结果。
- Trace/日志脱敏片段。
- JFR 摘要。
- Runbook 和历史复盘。

输出：

- 症状、时间线和影响范围。
- 可能根因及证据链接。
- 需要人工确认的假设。
- 推荐下一条只读查询。

Agent 不能凭摘要自动修改容量、清空队列或重放任务。

## 18. 简历表达

真实运行后可表达：

> 为 NotifyFlow 建立基于 Micrometer/OpenTelemetry 的用户 SLI、消息积压、线程池、连接池和供应商指标；使用 k6 开放到达模型与机器化 threshold 进行峰值、突发、浸泡和恢复测试，并通过数据库慢、供应商 503、Kafka/Consumer 停止和 JVM 压力注入验证告警、熔断、积压追赶和数据一致性，结合 JFR 与 Trace 定位性能瓶颈。

量化结果只能引用真实实验数据，并注明环境与不可外推边界。


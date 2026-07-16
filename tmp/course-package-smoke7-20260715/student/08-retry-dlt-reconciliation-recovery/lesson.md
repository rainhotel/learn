# 重试、DLT、对账与故障恢复控制面

## 课程信息

- 所属模块：可靠通知主链路
- 难度：深入
- 建议时长：20-28 小时
- 先修章节：第 5-7 章
- 对应项目里程碑：NotifyFlow 从“能发现失败”升级到“可恢复、可审计、可演练”
- 对应岗位能力：可靠性工程、消息恢复、外部系统一致性、事故处置、后台控制面设计

## 学习目标

完成本章后，学习者能够：

1. 建立可执行的错误分类，而不是对所有异常统一重试。
2. 设计重试次数、总时限、退避、抖动和令牌预算。
3. 选择 blocking retry、retry topic、数据库调度或人工恢复。
4. 设计 DLT、供应商对账、补偿和安全重放流程。
5. 将恢复动作建模为状态机、权限操作和审计事件。
6. 用事故演练和恢复指标验证系统，而不是只展示异常处理代码。

## 为什么要学

一个系统“失败后会重试”并不等于可靠。错误的重试会：

- 把下游过载放大为全面故障。
- 重复发送短信、重复扣款或重复执行 Agent 工具。
- 让毒消息长期占用 Partition。
- 使 DLT 成为无人负责的垃圾场。
- 在恢复瞬间释放全部积压，制造第二次事故。
- 让操作人员无法回答“谁在什么时候重放了哪些任务”。

可靠恢复需要同时设计数据面和控制面。

## 一、事故场景

NotifyFlow 的短信供应商从 10:00 开始出现延迟和 5xx：

```text
10:00 P99 从 300 ms 上升到 8 s
10:01 Consumer 开始超时并重试
10:02 retry topic 流量增加
10:03 供应商更过载，成功率继续下降
10:05 Consumer lag 和线程占用同时上升
10:08 操作人员暂停短信渠道
10:20 供应商恢复
10:21 全量恢复消费导致流量洪峰
10:22 供应商再次过载
```

真正的问题不是“有没有重试”，而是：

- 超时是否覆盖了完整调用？
- 重试发生在哪一层？
- 总重试预算是多少？
- 是否区分 429、5xx、非法号码和 Unknown？
- 暂停和恢复是否限速？
- 已经成功但客户端超时的请求如何确认？
- 谁能批量重放？

## 二、错误分类是恢复的入口

### 2.1 分类维度

| 类别 | 示例 | 自动重试 | 主要动作 |
|---|---|---|---|
| TRANSIENT | 短暂网络错误、偶发 5xx | 有界 | 退避、抖动、预算 |
| THROTTLED | 429、供应商配额 | 有界 | 尊重 Retry-After、降速 |
| PERMANENT | 非法号码、模板禁用 | 否 | 明确失败、通知业务方 |
| POISON | 反序列化、Schema 不支持 | 否或极少 | 隔离 DLT、修复数据/代码 |
| UNKNOWN | 客户端超时但副作用可能已发生 | 不直接重试 | 查询、回调、对账 |
| SYSTEMIC | 数据库不可用、供应商全故障 | 通常停止逐条重试 | 暂停、熔断、保护下游 |
| SECURITY | 越权、签名异常、敏感策略命中 | 否 | 隔离、告警、安全审计 |

### 2.2 分类应由稳定字段驱动

优先使用：

- HTTP 状态码和供应商错误码。
- 异常类型。
- 是否已经获得 provider request ID。
- 是否支持结果查询。
- 当前渠道健康状态。
- 业务截止时间。

不要只匹配异常消息字符串。

### 2.3 fatal exception

Spring Kafka `DefaultErrorHandler` 对部分异常默认视为 fatal，跳过重试并立即调用 recoverer。生产系统还应把明确的验证错误、非法格式和不支持版本加入不可重试分类。

## 三、重试预算

### 3.1 每次调用的预算

```text
deadline = 业务允许的最晚完成时间
attempt timeout = 单次调用最大时间
max attempts = 最大总尝试次数，包含首次
backoff = 两次尝试之间等待
retry token = 当前实例或渠道剩余的重试额度
```

最坏耗时近似：

```text
worstCase = Σ attemptTimeout + Σ backoff
```

若 `worstCase > deadline`，最后几次重试没有业务价值。

### 3.2 只在一个层次负责重试

五层调用链每层尝试 3 次，最底层可能承受：

```text
3^5 = 243
```

因此要指定唯一重试责任层：

- HTTP SDK 不重试，由业务 Worker 重试；或
- SDK 只做一次连接级快速重试，业务层不重复相同策略。

不能让网关、服务、SDK、MQ Consumer 和供应商代理都各自重试三次。

### 3.3 重试是额外流量

设原始请求速率为 `R`，平均每个请求额外重试 `r` 次：

```text
actualLoad = R * (1 + r)
```

故障率上升时 `r` 会快速增加。容量规划必须包含 retry traffic，而不是只看新任务 QPS。

## 四、退避、抖动与限速

### 4.1 上限指数退避

```text
delay = min(base * 2^(attempt-1), maxDelay)
```

必须同时有：

- 最大尝试次数。
- 最大总时限。
- 最大 backoff。
- 业务截止时间。

只有指数增长而没有上限，会让任务长期悬挂；只有上限而无限尝试，会让所有客户端以固定最大频率持续冲击下游。

### 4.2 抖动

Full jitter 示例：

```text
cap = min(base * 2^(attempt-1), maxDelay)
delay = random(0, cap)
```

抖动把同一批失败请求分散到不同时间，降低同步重试峰值。定时任务、租约续期和批量恢复也需要抖动。

### 4.3 重试令牌桶

为渠道或供应商建立独立 retry token：

- 有 token 时按策略重试。
- token 耗尽后，只以固定恢复速率释放少量探测。
- 新请求与重试请求可使用不同配额。

这样可以防止积压任务占满全部供应商容量。

## 五、三类重试路径

### 5.1 Blocking retry

当前 Listener 线程等待后重试。

- 优点：简单，原记录上下文完整。
- 缺点：阻塞 Partition，长 backoff 占用 Consumer 处理能力。
- 适用：极短、次数很少的瞬时错误。

### 5.2 Retry topic

失败记录发布到延迟级别不同的 Topic。

- 优点：原 Consumer 可继续推进，适合分钟级退避。
- 缺点：改变原始顺序；Topic、Header 和监控复杂；会产生额外消息流量。
- Spring Kafka 限制：non-blocking retry 不支持 batch listener，不能与 container transaction 组合。

### 5.3 数据库延迟任务

在 `next_attempt_at` 保存下次执行时间，由调度器领取。

- 优点：可修改、取消、查询和运营管理，适合长时间、业务状态丰富的恢复。
- 缺点：数据库扫描、索引和多实例领取复杂。
- 适用：NotifyFlow 外部供应商调用和 Unknown 对账。

### 5.4 选择矩阵

| 需求 | 首选 |
|---|---|
| 100-500 ms 瞬时错误、最多 1-2 次 | Blocking retry |
| 秒到分钟、无严格顺序 | Retry topic |
| 小时级、可取消、需运营查询 | 数据库调度 |
| 结果可能已发生 | 对账，不直接重试 |
| 永久格式错误 | DLT |

## 六、暂停、熔断与恢复

### 6.1 什么时候停止逐条重试

- 同一供应商错误率超过阈值。
- P99 超时持续上升。
- retry rate 接近或超过新请求速率。
- 下游明确返回维护或配额耗尽。
- 线程池、连接池或队列接近耗尽。

### 6.2 Circuit breaker 状态

```text
CLOSED -> 错误达到阈值 -> OPEN
OPEN -> 等待窗口 -> HALF_OPEN
HALF_OPEN -> 少量探测成功 -> CLOSED
HALF_OPEN -> 失败 -> OPEN
```

注意：熔断会引入模式切换，必须测试恢复条件。不能只测试“打开”，不测试“如何安全关闭”。

### 6.3 暂停 Kafka Consumer

暂停容器可以停止读取新消息，但：

- 已经在执行的调用仍需处理。
- lag 会增长。
- retention 必须覆盖最长暂停时间。
- 恢复不能瞬间放开全部并发。

### 6.4 分阶段恢复

```text
1% 探测 -> 5% -> 20% -> 50% -> 100%
```

每阶段检查：成功率、P99、429/5xx、Unknown、下游容量和 lag 下降速度。

## 七、DLT 是隔离区，不是垃圾场

### 7.1 DLT 的职责

- 让主链路继续推进。
- 保存足够证据用于诊断。
- 建立所有权、SLA 和处置流程。
- 支持安全、小批量重放。

### 7.2 Spring Kafka 默认行为

`DeadLetterPublishingRecoverer` 默认把记录发送到：

```text
<originalTopic>-dlt
same partition
```

因此 DLT 至少要有与原 Topic 相同的 Partition 数量。否则恢复本身可能失败。

### 7.3 Header 与数据安全

可保留：

- 原 Topic、Partition、offset。
- 异常类型和消息。
- eventId、aggregateId、tenantId。
- delivery attempt。
- 首次/最后失败时间。

堆栈 Header 可能很大，也可能包含路径、SQL 或敏感信息。Spring Kafka 支持排除堆栈 Header；生产系统应保存脱敏摘要，将完整堆栈放入受控日志系统。

### 7.4 DLT 生命周期

```text
OPEN -> ASSIGNED -> DIAGNOSED -> FIX_READY
     -> REPLAYING -> RESOLVED
     -> REJECTED / EXPIRED
```

每个状态必须有操作者、时间和原因。

## 八、UNKNOWN 与对账

### 8.1 超时不是失败证明

```text
NotifyFlow -> Provider 接收成功
Provider   -> 响应在网络中丢失
NotifyFlow -> timeout
```

如果直接重试，可能重复发送和收费。状态应为 `UNKNOWN`。

### 8.2 恢复顺序

1. 使用稳定 `attemptId` 查询供应商。
2. 等待供应商异步回调。
3. 定时拉取账单或状态文件。
4. 将本地尝试、回调和供应商记录对账。
5. 仍无法确定时进入人工 case。

### 8.3 回调乱序

回调可能晚于查询结果或重复到达。使用：

- `providerRequestId` 唯一键。
- provider event sequence 或发生时间。
- 合法状态转换。
- 原始回调审计表。

不能让晚到的 `ACCEPTED` 覆盖已经确认的 `DELIVERED`。

## 九、补偿

补偿是新的业务动作：

- 已发送错误通知：发送更正通知。
- 重复扣费：退款或冲正。
- Agent 错误创建工单：关闭工单并记录原因。
- 设备命令误执行：下发安全的反向命令，前提是领域允许。

补偿的风险：

- 反向动作也可能失败。
- 不是所有副作用可逆。
- 补偿可能需要审批。
- 多次补偿仍需幂等。

## 十、人工重放

### 10.1 为什么需要控制面

直接运行一个脚本把 DLT 全量发回原 Topic，缺少：

- 权限。
- 租户隔离。
- QPS 限制。
- 幂等策略。
- 预览和 dry-run。
- 审批与审计。
- 结果统计和暂停能力。

### 10.2 安全重放流程

```text
筛选 -> 预览 -> 诊断依据 -> 修复版本
-> dry-run -> 审批 -> 创建 replay batch
-> 小批量限速 -> 观察 -> 扩大
-> 结束/暂停/回滚策略 -> 复盘
```

### 10.3 原 eventId 还是新 replayId

- 保留原 eventId：已有消费幂等会跳过，适合验证“是否已经处理”。
- 新 eventId：会重新执行副作用，风险高。
- 推荐：保留原 eventId，新增 replayId；由业务处理器根据明确的 replay policy 决定是否允许重新执行。

不能通过偷偷修改 eventId 绕过幂等。

## 十一、恢复状态机

### 11.1 Delivery attempt

```text
PLANNED
 -> SENDING
 -> SUCCEEDED
 -> RETRY_WAIT
 -> UNKNOWN -> RECONCILING -> SUCCEEDED / FAILED / MANUAL
 -> PERMANENT_FAILED
```

### 11.2 Replay batch

```text
DRAFT -> REVIEWED -> APPROVED -> RUNNING
      -> PAUSED -> RUNNING
      -> COMPLETED / PARTIAL / CANCELLED / FAILED
```

状态更新采用 expected status/version 条件，防止两个操作员同时启动同一批次。

## 十二、恢复控制面的权限与安全

角色示例：

- Viewer：查看脱敏失败信息。
- Operator：执行低风险单条重放。
- Approver：批准大批量或有副作用重放。
- Admin：管理策略，不直接绕过审计。

高风险操作：

- 新 eventId 重新执行。
- 跨租户批量重放。
- 关闭幂等检查。
- 调高供应商并发。
- 执行不可逆 Agent 工具。

这些操作至少需要二次确认，重要场景需要双人审批。

## 十三、Agent 在恢复控制面中的边界

适合 Agent：

- 聚类相似异常。
- 检索 Runbook 和历史事故。
- 生成脱敏诊断摘要。
- 推荐错误分类和可能修复。
- 生成重放预览与风险清单。

不应默认自动执行：

- 批量重放。
- 退款、删除、关闭设备等不可逆操作。
- 修改重试策略和限流配额。
- 绕过租户权限。

Agent 输出必须附证据引用，并由确定性策略执行最终授权。

## 十四、指标与 SLO

### 14.1 恢复指标

- retry rate / original request rate。
- retry success rate。
- DLT ingress rate。
- oldest unresolved DLT age。
- Unknown case 数量和最长时长。
- reconciliation success rate。
- replay success/duplicate/failure。
- 从故障开始到暂停的 MTTD/MTTM。
- 从依赖恢复到 backlog 清空的 MTTR。

### 14.2 重试风暴告警

```text
retry_rate > original_rate * 0.5
provider_5xx > 10% for 5m
unknown_attempts 持续增长
oldest_dlt_age > 30m
consumer_lag 增长且 provider_p99 同时上升
```

### 14.3 恢复容量

```text
netDrainRate = safeRecoveryCapacity - newTrafficRate
recoveryTime = backlog / netDrainRate
```

若恢复容量小于新流量，必须限流入口、扩容或延长恢复时间，不能宣称“自动追平”。

## 十五、Runbook

一次供应商全故障的操作步骤：

1. 确认是单租户、单渠道还是全局。
2. 查看错误分类、P99、429/5xx、Unknown 和 lag。
3. 停止无价值逐条重试，打开渠道级熔断或暂停。
4. 保留新任务，不丢弃业务事实。
5. 联系供应商并记录事件时间线。
6. 供应商恢复后，以探测流量验证。
7. 按阶段释放新流量与 backlog。
8. 对 Unknown 做查询与对账。
9. 处理 DLT，必要时审批重放。
10. 完成事故复盘和策略修订。

## 十六、常见错误

### 所有异常统一重试

永久错误浪费资源，毒消息阻塞链路。必须分类。

### 每一层都重试

造成指数放大。必须指定唯一重试责任层。

### 只设置次数，不设置截止时间

最后几次重试可能已经超过业务价值窗口。

### 固定间隔无抖动

同批任务同步唤醒，形成周期性洪峰。

### 超时直接标记失败

外部副作用可能已发生，应进入 Unknown 和对账。

### DLT 全量一键重放

会再次触发原故障并压垮下游，应预览、审批、限速和分批。

### Agent 自动决定高风险恢复

模型建议不能替代确定性权限、业务规则和人工审批。

## 十七、章节作业

- 作业目标：为 NotifyFlow 设计并演练一次完整供应商故障恢复。
- 提交物：错误分类表、重试预算、状态机、DDL、恢复操作台原型、八组实验、事故时间线、Runbook 和 15 分钟讲解。
- 验收标准：没有无限重试；Unknown 有对账；重放有权限、限速和审计；所有指标有业务含义。
- 加分项：Agent 生成带引用的事故摘要，但不能自动执行高风险重放。

## 本章小结

- 重试是额外负载，必须有唯一责任点和预算。
- 指数退避必须配合上限、抖动、总时限和令牌限制。
- blocking retry、retry topic 和数据库调度服务于不同时间尺度。
- DLT 是隔离和诊断入口，不是问题终点。
- 外部调用超时应进入 Unknown，通过查询、回调和对账收敛。
- 补偿是新的业务动作，仍会失败且仍需幂等。
- 人工重放必须产品化为带权限、审批、限速和审计的控制面。
- Agent 可以辅助诊断，不能绕过确定性安全边界。

## 版本记录

- v0.1，2026-07-14：完成基于 Spring Kafka 4.1.0 与 AWS Builders' Library 的完整初稿；实验 Pending。


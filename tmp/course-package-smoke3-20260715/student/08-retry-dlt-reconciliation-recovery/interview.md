# 面试追问与回答要点

## 1. 哪些异常应该重试？

先分类：瞬时网络、部分 5xx 和限流可能重试；验证错误、非法数据、Schema 不兼容通常不重试；超时且副作用可能已发生进入 Unknown；系统性故障应暂停或熔断，避免逐条重试风暴。

## 2. 为什么重试会导致雪崩？

重试是额外请求。下游因过载失败时，重试进一步增加负载；多层各自重试会指数放大，例如五层每层三次可达 243 次底层调用。

## 3. 如何设计重试策略？

明确唯一责任层、max attempts、单次 timeout、总 deadline、上限指数退避、jitter、可重试错误、Retry-After、重试令牌和业务截止时间。

## 4. 固定退避和指数退避有什么区别？

固定退避简单但容易形成周期性同步峰值；指数退避逐渐降低频率，但仍需 cap、次数上限和 jitter。

## 5. 为什么需要 jitter？

同批失败任务若使用相同等待时间，会同时重试。jitter 把到达时间分散，降低重试峰值和争用。

## 6. blocking retry 与 retry topic 怎么选？

极短、次数少的错误可 blocking；秒到分钟且无严格顺序可 retry topic；长期、可取消、需运营管理的任务用数据库调度。retry topic 会改变顺序并增加 Topic/监控复杂度。

## 7. Spring Kafka non-blocking retry 有什么限制？

4.1.0 官方文档说明：不支持 batch listener，不能与 container transaction 组合。不能只加 `@RetryableTopic` 就忽略当前 Listener 和事务模式。

## 8. DefaultErrorHandler 做什么？

使用 BackOff 决定重新投递与恢复；可配置 recoverer，在达到次数后发送 DLT或执行其他恢复；fatal exception 可跳过重试。

## 9. DLT Topic 如何设计？

Spring 默认 `<originalTopic>-dlt` 且使用相同 Partition，因此 DLT Partition 数不能少于原 Topic。还要考虑 retention、权限、敏感 Header、监控、所有权和重放流程。

## 10. DLT 是否影响顺序？

会。失败消息被隔离后，后续消息可能继续处理；修复后再重放时会晚于后续事件。需要业务 version、sequence 和状态机拒绝过期事件。

## 11. 客户端超时后为什么不能直接重试？

超时只是客户端没拿到确定响应，供应商可能已经执行。直接重试会重复副作用。使用稳定 idempotencyKey、providerRequestId 查询、回调和账单对账。

## 12. 如何处理回调重复和乱序？

provider event ID 唯一约束、原文审计、合法状态机、sequence/发生时间和条件更新。晚到低阶段事件不能覆盖终态。

## 13. 什么是补偿事务？

补偿是用于抵消已提交业务动作的新动作，不是数据库回滚。它也可能失败，需要幂等、重试、审计和人工介入。

## 14. 如何安全重放 DLT？

先 preview 和 dry-run，确认修复版本、幂等行为、租户范围和外部副作用；审批后小批量限速，观察成功率和下游指标，再逐步扩大；所有 item 和操作留审计。

## 15. 重放使用原 eventId 还是新 eventId？

原 eventId 通常会被幂等跳过，安全但可能无法重新执行；新 eventId 会重新触发副作用。推荐保留 originalEventId，增加 replayId，由明确 replay policy 决定，不能偷偷换 ID 绕过幂等。

## 16. Circuit breaker 有什么风险？

会引入 CLOSED/OPEN/HALF_OPEN 模式切换，阈值错误可能误熔断或恢复过早。必须测试半开探测、分阶段恢复和多实例状态协调。

## 17. 消费暂停后要注意什么？

lag 和 retention、在途调用、恢复容量、rebalance、入口流量和分阶段恢复。暂停不是丢弃，恢复也不能瞬间全开。

## 18. 如何计算积压恢复时间？

`netDrainRate = safeCapacity - newTrafficRate`，`recoveryTime = backlog / netDrainRate`。如果净速率不大于零，积压不会下降。

## 19. 恢复控制面为什么需要数据库？

操作批次、审批、租户范围、状态机、暂停、进度和审计需要可查询的权威状态，不能只存在 Kafka Header 或进程内存中。

## 20. Agent 能否自动重放？

默认不能。Agent 适合异常聚类、Runbook 检索和风险建议；批量重放、退款、设备命令等高风险操作必须由确定性权限、策略和人工审批控制。

## 项目深挖

### 你如何证明没有重试风暴？

真实完成后展示：原始 QPS、retry QPS、令牌耗尽次数、错误率、下游 P99、熔断时间线和恢复曲线。只展示配置代码不能证明。

### 你的 DLT 谁负责？

回答 owner、告警 SLA、case 状态、诊断字段、审批人、重放批次和最终结果；如果没有负责人，DLT 设计不完整。

### 为什么不直接用 Kafka retry topic 处理所有失败？

Unknown 需要查询对账，长期任务需要取消和运营查询，永久错误不应重试，严格顺序会被 retry topic 改变。不同错误需要不同恢复通道。


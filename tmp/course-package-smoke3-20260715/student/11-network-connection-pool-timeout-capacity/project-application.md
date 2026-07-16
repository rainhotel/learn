# NotifyFlow 网络与超时方案

## 1. 依赖合同

| 依赖 | acquire/connect | request/read | 副作用 | timeout 后 |
|---|---|---|---|---|
| MySQL | Hikari acquire | JDBC/query timeout | 事务写 | acquire 超时未进 DB；执行中断按事务边界 rollback；断链时按 UNKNOWN 查询 |
| Redis | client connect | command timeout | 限流/缓存 | 按命令语义 |
| Kafka | metadata/connect | delivery timeout | 发布消息 | Producer 幂等只约束特定会话/配置；用 Outbox、业务键、消费回执或对账确认 |
| Provider | connect/TLS | response deadline | 发送通知 | UNKNOWN/对账 |
| Model API | connect | TTFT/idle/overall | token/Tool 建议 | 取消/恢复 run |

## 2. 配置原则

- 总 deadline 由用户 SLA 反推；每次调用只获得“剩余预算”，并为持久化、取消和清理保留时间。阶段可能复用、并行或被跳过，不能机械相加。
- pool acquire timeout 小于业务 deadline，饱和时快速拒绝。
- timeout、retry、circuit、rate limit 和 queue 使用统一预算。
- 不在数据库事务中等待 Provider 或模型响应。

## 3. Java 边界

`ModelGateway` 和 `ProviderClient` 返回结构化结果：SUCCESS、RETRYABLE、PERMANENT、UNKNOWN、CANCELLED。异常不直接跨层变成字符串。

## 4. SSE

事件持久化：`run.created`、`model.delta`、`tool.proposed`、`tool.result`、`run.completed/failed`。每个事件包含 run 内单调递增的 `id`；客户端用 `Last-Event-ID` 重连并容忍重复。服务端规定事件保留期，遇到断档返回快照或明确要求重新拉取；断线不改变数据库中的 run 真相。

## 5. 示例预算

异步创建通知任务的 API SLA 为 800 ms：鉴权 50 ms、Hikari acquire 80 ms、事务读写 250 ms、Outbox 写入 100 ms，至少保留 150 ms 用于响应、取消与清理，其余作为抖动余量。Provider worker 的单次 attempt 独立使用 3 s deadline，并把调用开始前的剩余时间传播给客户端；不足以完成一次安全尝试时不再发起调用。

## 6. 事故助手

Agent 只读聚合 dependency、pool、timeout、retry、Trace 和 socket 证据；修改 timeout/连接池/重试策略必须经过压测、审批和回滚。

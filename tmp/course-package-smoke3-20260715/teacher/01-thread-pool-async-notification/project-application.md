# NotifyFlow 项目应用：异步通知执行器

## 1. 目标

设计一个单实例内部执行器，用于限制短信、邮件和站内信的并发调用。它不是消息可靠性的唯一来源，任务事实必须先持久化。

## 2. 第一版设计

```java
ThreadPoolExecutor smsExecutor = new ThreadPoolExecutor(
        8,
        24,
        60,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(200),
        namedThreadFactory("notify-sms"),
        new ThreadPoolExecutor.AbortPolicy());
```

这些数值只是待压测的初始假设，不能直接作为生产答案。

## 3. 参数推导表

在写下数值前先填写：

| 项目 | 当前值 | 证据来源 |
|---|---:|---|
| 供应商允许 QPS |  | 合同/测试 |
| 平均响应时间 |  | 压测 |
| P95/P99 响应时间 |  | 压测 |
| HTTP 最大连接数 |  | 客户端配置 |
| 单任务平均内存 |  | 估算/分析 |
| 最大允许排队时间 |  | SLA |
| 实例数量 |  | 部署计划 |
| 单租户配额 |  | 产品规则 |

## 4. 接收任务前提

执行器只接收已经拥有以下信息的任务：

- `taskId`
- `notificationItemId`
- `tenantId`
- `channel`
- `idempotencyKey`
- `attempt`
- `deadline`

Runnable 本身不应成为唯一状态载体。

## 5. 拒绝处理

拒绝处理器不负责静默丢弃。推荐动作：

1. 增加拒绝指标。
2. 将任务保持为 `PENDING` 或标记为可重试状态。
3. 暂停/降低上游消费速度。
4. 记录池大小、队列长度和任务标识。
5. 由 MQ 重投、调度器扫描或人工重放恢复。

## 6. 渠道隔离

短信、邮件、站内信至少在逻辑上隔离：

- 独立线程池或独立并发许可。
- 独立供应商客户端和连接池。
- 独立超时、重试和限流策略。
- 独立成功率与延迟指标。

## 7. 任务包装器

可将 Runnable 包装以记录排队和执行耗时：

```java
record TimedTask(long submittedAtNanos, Runnable delegate) implements Runnable {
    @Override
    public void run() {
        long queueNanos = System.nanoTime() - submittedAtNanos;
        long started = System.nanoTime();
        try {
            delegate.run();
        } finally {
            long executionNanos = System.nanoTime() - started;
            recordMetrics(queueNanos, executionNanos);
        }
    }
}
```

真实实现还需传递 traceId、租户和任务 ID，并谨慎清理 ThreadLocal。

## 8. 关闭顺序

1. 将实例标记为不再接收新流量。
2. 暂停 MQ Consumer 或任务扫描器。
3. 等待已经领取的任务进入完成或可恢复状态。
4. `shutdown()` 业务线程池。
5. 超时后 `shutdownNow()`，记录未开始任务。
6. 释放 HTTP、数据库和监控客户端。

## 9. 必做指标

- `executor.active`
- `executor.pool.size`
- `executor.queue.size`
- `executor.queue.remaining`
- `executor.rejected.total`
- `task.queue.duration`
- `task.execution.duration`
- `notification.success.total`
- `notification.timeout.total`
- `notification.retry.total`

## 10. ADR 作业

编写 ADR：为什么 NotifyFlow 选择“有界平台线程池 + 渠道限流”，暂不直接使用无界队列或无限虚拟线程。

ADR 必须包含：背景、决策、替代方案、后果、验证计划和回滚条件。


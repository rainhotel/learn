# Teach-back 讲解稿

## 5 分钟版本

### 0:00-0:40 核心观点

可靠恢复不是“失败就重试”。重试会增加下游负载，错误设计甚至会让局部故障变成全面停机。

### 0:40-1:30 错误分类

我把失败分为瞬时、限流、永久、毒消息、Unknown 和系统性故障。只有瞬时和部分限流适合自动重试；永久错误直接失败，毒消息隔离到 DLT，Unknown 查询对账，系统性故障暂停或熔断。

### 1:30-2:20 重试预算

重试必须有唯一责任层。五层每层尝试三次，底层可能被调用 243 次。策略要同时包含 max attempts、单次 timeout、总 deadline、上限指数退避、jitter 和重试令牌。

### 2:20-3:10 DLT

DLT 是隔离区，不是终点。它需要保留原 Topic、Partition、offset、eventId、异常分类和失败时间，还要有 owner、SLA、诊断、修复和重放流程。Spring Kafka 默认发到原 Topic 加 `-dlt` 的相同 Partition，所以 DLT Partition 数不能更少。

### 3:10-4:05 Unknown 和对账

供应商已接收但响应丢失时，客户端看到 timeout。直接重试可能重复发送。NotifyFlow 将 attempt 标为 Unknown，用 attemptId/providerRequestId 查询、等待回调或账单对账，再收敛为成功、失败或人工处理。

### 4:05-5:00 安全重放

人工重放必须先 preview、dry-run 和审批，再小批量限速执行。保留 originalEventId 和 replayId，不能改 ID 绕过幂等。Agent 可以生成带引用的诊断和风险建议，但不能默认执行批量重放。

## 15 分钟版本

### 第一部分：事故时间线（2 分钟）

画出供应商变慢、Consumer 超时、retry 流量增加、供应商进一步过载、lag 上升的正反馈。指出扩 Consumer 会加重下游压力。

### 第二部分：错误分类（2 分钟）

画七类错误表，重点解释 Unknown 和 Systemic 为什么不能进入普通逐条重试。

### 第三部分：重试预算（3 分钟）

写出：

```text
3^5=243
worstCase = Σ timeout + Σ backoff
actualLoad = originalRate * (1 + retriesPerRequest)
```

解释单点重试、指数退避、cap、jitter、token bucket 和 deadline。

### 第四部分：三类恢复路径（2 分钟）

比较 blocking retry、retry topic 和数据库调度，并说出 Spring Kafka non-blocking retry 的 batch/transaction 限制。

### 第五部分：DLT 与对账（3 分钟）

画出 DLT case 生命周期，以及 Provider timeout -> Unknown -> query/callback/bill -> result 的对账链。

### 第六部分：控制面（2 分钟）

画 replay batch：DRAFT、APPROVED、RUNNING、PAUSED、COMPLETED。说明租户权限、审批、QPS、幂等和审计。

### 第七部分：Agent 边界（1 分钟）

Agent 负责检索、聚类、摘要和建议；确定性状态机、权限和人工审批负责执行。

## 必须画出的图

### 重试放大

```text
API(3) -> Service(3) -> Adapter(3) -> SDK(3) -> Proxy(3)
3^5 = 243
```

### Unknown 对账

```text
SENDING -> timeout -> UNKNOWN
                  -> query/callback/bill
                  -> SUCCEEDED / FAILED / MANUAL
```

### 重放控制面

```text
filter -> preview -> dry-run -> approval
-> rate-limited replay -> observe -> audit
```

## 自测标准

- 能闭卷解释为什么 timeout 不等于 failed。
- 能计算至少一个重试预算和恢复时间。
- 能说出 DLT 默认分区限制。
- 能列出五项重放前检查。
- 不使用“无限重试”“彻底保证”或“Agent 自动处理一切”。


# Teach-back 讲解稿

## 5 分钟版本

### 0:00-0:45 从用户目标开始

NotifyFlow 的监控目标不是 CPU 低，而是任务能被创建、及时开始执行并在 deadline 内进入明确终态。先定义 SLI/SLO，再设计指标和告警。

### 0:45-1:30 四个黄金信号

Latency、traffic、errors、saturation。成功和失败延迟分开；错误还包括 200 但结果错误、超过 deadline；饱和度看队列、连接池和 lag，不只看 CPU。

### 1:30-2:20 三类信号

Metrics 看趋势，logs 看事件细节，traces 看跨服务路径，JFR 看 JVM 内部。指标 tag 只能使用 provider/result 等低基数枚举，taskId 放日志和 Trace。

### 2:20-3:20 负载模型

固定 VU 是封闭模型，系统变慢后请求自然减少，会产生 coordinated omission。若目标是固定 500 RPS，要使用开放到达模型，并监控 dropped iteration。

### 3:20-4:10 Threshold 和容量

k6 threshold 把错误率和 P95/P99 写成测试通过条件。逐级加压找吞吐不再线性、P99 和队列陡升的拐点，安全容量位于拐点前。

### 4:10-5:00 故障演练

故障注入要有假设、爆炸半径、停止条件和恢复数据校验。NotifyFlow 需要验证数据库慢、供应商 503、Consumer 停止和 JVM 压力，并证明告警、保护、恢复和最终数据都正确。

## 15 分钟版本

### 第一部分：SLI/SLO（2 分钟）

画出用户旅程：create -> accepted -> first attempt -> terminal。为每一段定义 SLI 和目标，计算 error budget。

### 第二部分：信号体系（3 分钟）

画黄金信号与 RED/USE；比较 Counter、Gauge、Timer；说明高基数问题和 metric/log/trace/JFR 的关联。

### 第三部分：压测模型（3 分钟）

画封闭模型等待上一 iteration，开放模型按时钟到达。解释 coordinated omission、dropped iteration、warm-up 和 workload mix。

### 第四部分：容量（2 分钟）

写 `L=λW`，说明吞吐、并发和延迟关系；画负载增加时吞吐、P99、队列的拐点。

### 第五部分：故障注入（3 分钟）

用供应商 503 演示：稳态 -> 故障 -> 告警 -> 熔断 -> 积压 -> 1% 探测 -> 分阶段恢复 -> 数据校验。

### 第六部分：证据输出（2 分钟）

说明压测报告必须包含环境、版本、负载、threshold、指标、故障时间线、正确性和不可外推边界。

## 必须画出的图

```text
User SLI -> Service golden signals -> Component RED -> Resource USE
```

```text
Closed: finish -> next arrival
Open: clock -> arrivals independent of latency
```

```text
load ↑ -> throughput linear -> knee -> queue/P99/errors ↑
```

## 自测标准

- 不把并发等同于 QPS。
- 能解释 coordinated omission。
- 能列出五个禁止作为 tag 的字段。
- 能写出一个 k6 threshold。
- 能说明为什么故障实验必须校验数据正确性。


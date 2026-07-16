# 练习答案与评分标准

## 一、基础题

### 1. 四个黄金信号（4 分）

- Latency：API/provider/first-attempt P99。（1）
- Traffic：request/task/message/provider call rate。（1）
- Errors：5xx、deadline failure、UNKNOWN 等。（1）
- Saturation：queue、pool pending、lag、CPU/GC。（1）

### 2. SLI/SLO/SLA（4 分）

SLI 是测量值，SLO 是内部目标，SLA 是外部承诺；error budget 是 SLO 允许的不符合比例或数量。（每项 1）

### 3. Metric 类型（4 分）

任务总数 Counter；Outbox 当前积压 Gauge；供应商耗时 Timer；批次大小 DistributionSummary；运行中 replay LongTaskTimer。五项全对 4 分，错一项扣 0.8。

### 4. 基数（4 分）

taskId 近乎每次不同，会制造大量时间序列和成本。（2）放结构化日志、Trace attribute 或 exemplar 链接中。（2）

### 5. 信号（4 分）

Metrics 看聚合趋势；logs 看离散事件和字段；traces 看跨组件请求路径；JFR 看 JVM 内部事件、线程、分配等诊断。（每项 1）

## 二、计算题

### 6. Error budget（5 分）

`20,000,000 * (1 - 0.9995) = 10,000` 次。（5）

### 7. Little's Law（5 分）

`L=1200*0.25=300`。（3）不能直接当线程数，因为请求可能异步、CPU/阻塞比例不同，还受连接、队列和长尾影响。（2）

### 8. 恢复时间（5 分）

净速率 `7000-3000=4000/s`。（2）时间 `6,000,000/4000=1500s=25min`。（3）

### 9. 百分位（5 分）

总耗时 `990*20 + 9*500 + 10000 = 34300 ms`，平均 34.3 ms。（3）平均掩盖少量长尾；P99 接近正常/500ms 边界，max 显示 10s 极端请求。（2）

### 10. Cardinality（5 分）

`10*8*4=320` 个组合。（2）加入 100 万 taskId 后理论可到 3.2 亿组合，造成存储、内存、查询和费用问题。（3）

## 三、压测设计

### 11. 模型选择（6 分）

- 固定 1000 RPS：开放。（1.5）
- 100 用户等待轮询：封闭。（1.5）
- 整点任务：开放/外部到达事件，模拟一次释放。（1.5）
- 200 操作员：封闭用户模型。（1.5）

### 12. Coordinated omission（5 分）

系统变慢导致测试端少发 700 RPS，恰好遗漏最拥堵时本应到达的请求；测得的排队和长尾低于真实固定到达率场景。（5）

### 13. Threshold（5 分）

```javascript
thresholds: {
  http_req_failed: ['rate<0.001'],
  http_req_duration: ['p(95)<200', 'p(99)<500'],
  dropped_iterations: ['count==0'],
}
```

每项 1，语法和单位 1。

### 14. Workload mix（4 分）

比例合理且合计 100%（1）；覆盖复杂批次（1）；准备足够 tenant/template/recipient 数据（1）；避免缓存和唯一键导致不真实结果（1）。

### 15. 正确性（5 分）

任五项，每项 1：task/outbox 同数、requestId 无重复、状态机合法、总数守恒、重复消息无重复副作用、DLT/UNKNOWN 与注入一致、无孤儿 attempt、对账收敛。

## 四、故障分析

### 16. CPU 40%（7 分）

任六项 1 分：连接池 pending、数据库锁/I/O、线程池队列、外部供应商、Kafka 热 Partition、GC pause、锁竞争、DNS/TLS、磁盘、压测端瓶颈。能说明用 Trace/JFR/指标关联再加 1 分。

### 17. 恢复过载（7 分）

扩 Consumer 会增加供应商并发和 retry，超过安全容量。（2）先 1%/5% 探测（1），限制新流量和 backlog 独立配额（1），按 20/50/100% 放量（1），观察成功率/P99/429/UNKNOWN（1），支持暂停回退（1）。

### 18. 高基数（6 分）

立即回滚/禁用 meter（1），使用 MeterFilter 拒绝（1），清理或缩短异常数据保留（1），改用低基数 route/status（1），requestId 放日志/Trace（1），建立 tag 白名单和 cardinality 告警（1）。

## 五、表达

### 19. 简历纠错（5 分）

真实运行并保存 Dashboard、压测输出、故障时间线和数据正确性证据后，可表达为：

> 在固定版本和资源环境中，使用 k6 开放到达模型对 NotifyFlow 的实时创建、批量任务和恢复链路进行分阶段压测，通过错误率、P95/P99、dropped iteration、线程池、连接池和 Kafka lag threshold 判断结果；注入数据库慢、供应商 503 和 Consumer 停止，验证告警、熔断、积压追赶和数据一致性。容量结论仅适用于报告所列环境。

模型明确（1）、不虚构 10 万 QPS（1）、使用长尾和 threshold（1）、故障受控（1）、说明边界（1）。

### 20. Agent（5 分）

可提供脱敏指标、Trace/日志摘要、JFR 摘要、Runbook、历史复盘和时间窗口。（2）不能自动扩容无限资源、清空队列、批量重放、修改 SLO、删除数据或执行不可逆操作。（3）

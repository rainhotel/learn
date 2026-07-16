# 面试追问与回答要点

## 1. 什么是可观测性？

通过系统输出推断内部状态并验证故障假设的能力。不是指标数量，而是能否从用户症状定位到组件和资源。

## 2. 四个黄金信号是什么？

Latency、traffic、errors、saturation。成功和失败延迟应分开；错误包含显式、隐式和违反策略；饱和度关注受约束资源及其提前退化。

## 3. SLI、SLO、SLA 区别？

SLI 是测量，SLO 是目标，SLA 是外部承诺。Error budget 把 SLO 的容错空间量化，用于平衡变更与可靠性。

## 4. 为什么不只看平均延迟？

平均值会掩盖长尾，批量和下游链路常由最慢请求决定。结合 P50/P95/P99、max、错误和样本量。

## 5. P99 可以直接跨实例平均吗？

不可以。百分位不可简单平均。应聚合 histogram bucket 或原始分布，再计算全局 quantile。

## 6. Counter 和 Gauge 如何选？

累计事件用 Counter，当前状态用 Gauge。Gauge 不可靠保存历史发生次数，Counter 关注 rate/increase。

## 7. 为什么 taskId 不能作 tag？

高基数会制造海量时间序列。taskId 放日志/Trace，metric tag 使用 provider、result 等有限枚举。

## 8. Spring Boot 如何注册指标？

4.1.0 自动配置 composite MeterRegistry。注入 Spring 管理的 registry；复用指标用 MeterBinder；控制和拒绝 meter 用 MeterFilter。

## 9. Metric、log、trace 如何配合？

Metric 告诉“是否有问题”，Trace 告诉“请求经过哪里”，Log 提供事件细节。通过时间、traceId/eventId 和 exemplar 关联。

## 10. 什么是开放和封闭负载？

封闭模型下一 iteration 等待上一轮完成；开放模型按独立到达率发起。固定 QPS 场景通常用开放模型。

## 11. 什么是 coordinated omission？

封闭模型中系统变慢会让压测端自动降低到达率，遗漏最拥堵时本应到达的请求，使延迟结果过于乐观。

## 12. k6 threshold 有什么价值？

把错误率、P95/P99、dropped iteration 等 SLO 编码为机器化 pass/fail，测试不满足条件时失败。

## 13. 并发 1000 和 1000 QPS 相同吗？

不同。并发是在途数量，QPS 是单位时间完成或到达速率。二者通过延迟相关，近似 `L=λW`。

## 14. 如何找容量上限？

逐级增加开放负载，观察吞吐、P99、错误、队列、连接池、CPU/GC、I/O。找到吞吐不再线性增长或延迟陡升的拐点；安全容量要低于极限。

## 15. 什么是 soak test？

在安全负载下持续数小时，发现内存、线程、连接、meter、缓存和句柄随时间累积的问题。

## 16. 压测为什么还要查数据库？

高性能但产生重复、丢失或非法状态的结果无意义。验证 task/outbox、状态机、幂等和总数守恒。

## 17. CPU 不高为什么还会慢？

可能阻塞在数据库、连接池、外部 HTTP、锁、磁盘、网络、Kafka 热 Partition 或压测端。用队列/pending、Trace 和 JFR 定位。

## 18. 故障注入怎么保证安全？

明确假设、环境、爆炸半径、停止条件、监控、恢复步骤和数据校验；从本地/测试环境和小范围开始。

## 19. JFR 能解决什么？

记录和流式处理 JVM 事件，包括自定义事件；用于分配、线程、锁、GC 等 JVM 内部诊断。不能替代用户 SLI 和分布式 Trace。

## 20. 如何证明一次优化有效？

固定版本、资源、数据和负载，至少进行基线与修改后对比；同时报告吞吐、长尾、错误、资源和正确性，并说明误差与外推边界。

## 项目深挖

### 你压测过多少 QPS？

只回答真实数据，并附机器资源、请求组成、持续时间、P99、错误率、threshold 和瓶颈。没有证据时说“计划验证”，不能猜数字。

### 你的告警如何避免噪声？

Page 面向用户症状和持续 error-budget 消耗；原因型指标进入 Dashboard/Ticket。设置持续时间、多窗口和明确 owner/runbook。

### Agent 如何参与事故分析？

Agent 汇总脱敏指标、Trace、日志和 Runbook，生成带证据的假设；人和确定性控制面决定扩容、重放、降级等操作。


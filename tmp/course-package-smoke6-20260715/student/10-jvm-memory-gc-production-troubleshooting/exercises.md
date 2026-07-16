# 第 10 章练习

## 一、概念题

1. 区分 JMM、JVM 规范运行时数据区和 HotSpot 内存实现。
2. 解释 GC Roots、强/软/弱/虚引用的适用边界。
3. 比较 G1、Parallel、ZGC 的吞吐、延迟和内存开销取舍。
4. 为什么 heap 使用率低不能证明进程内存安全？

## 二、诊断题

5. GC pause 从 20 ms 升到 300 ms，但 API P99 没有同步升高，列出至少三种解释。
6. heap used 平稳、RSS 每小时增长，设计一条 NMT/buffer/thread 排查链路。
7. `Java heap space`、`Metaspace`、`Direct buffer memory` 和 native thread 失败如何分流？
8. 线程 Dump 中 200 个线程 `WAITING`，为什么不能直接判定死锁？

## 三、NotifyFlow 设计题

9. 给任务 payload、重试、DLT 和 Agent context 设计保留期限与大小预算。
10. 设计一个“不重启全量实例”的 OOM 止血方案，包含停止条件和证据保留。
11. 设计自定义 JFR 事件，要求字段低基数、可关联业务时间线但不把 taskId 作为聚合标签。

## 四、事故题

12. Provider 503 后重试堆积，CPU 低、heap 高、P99 高。你如何证明是 payload/队列保留还是 GC？
13. 虚拟线程服务的吞吐下降，JFR 显示 pinning。列出验证、降级和修复步骤。
14. Agent 说“疑似内存泄漏”，你如何要求它给出证据链，并阻止它直接执行 heap dump？

## 五、证据题

15. 设计一组实验，比较短生命周期对象、高分配率和保留链；列出输入、指标、JFR/GC 证据和边界。
16. 写一段简历描述，要求明确实验环境、不要编造生产数据。

## 作业提交

- 一张 JVM 资源地图。
- 一份 NotifyFlow 内存预算表。
- 一份症状到证据的 Runbook。
- 一组尚未运行实验的 Pending 清单。

# 第 10 章 Teach-back

## 5 分钟：heap 不是进程内存

1. 先画 heap、线程栈、Metaspace、Direct、Code Cache 和 RSS。
2. 给出 heap 平稳但 RSS 增长的三个假设。
3. 说明下一条只读证据和不能执行的高风险动作。

## 15 分钟：一次 NotifyFlow 内存事故

结构：症状 -> 止血 -> heap/native/thread 分流 -> JFR/GC/NMT 证据 -> 修复 -> 回归 -> 边界。

必须说清楚：GC pause 不等于 API P99，Agent 摘要不等于根因。

## 45 分钟：带实验设计的完整课程

- 10 分钟：JMM/JVM/HotSpot 三层边界。
- 10 分钟：对象生命周期、GC Roots、G1 与选择。
- 10 分钟：OOM/RSS/线程/锁排障 Runbook。
- 10 分钟：NotifyFlow 内存预算和 Agent 上下文预算。
- 5 分钟：Pending 实验、证据等级和简历边界。

## 试讲验收

- 听众能复述至少三类非 heap 内存。
- 听众能为 heap OOM 和 native thread 失败选择不同证据。
- 讲者没有把第 09 章 JFR Phase A 扩写成 GC 诊断。
- 讲者能回答“哪些动作需要人工审批”。

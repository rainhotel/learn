# 第 10 章：Java 21 JVM 内存、GC 与生产排障

## 章节定位

- 类型：Concept + Incident + Project + Lab Design + Interview + Teach-back
- 难度：深入
- 建议学习时间：24-32 小时
- 先修章节：第 3 章 JMM、第 9 章可观测性与 JFR Phase A
- 资料基线：Java SE 21 规范、Oracle JDK 21 GC Tuning Guide、JFR/JCMD/NMT、JEP 439、JEP 444
- 对应项目：NotifyFlow 内存预算、GC 基线、线程与锁诊断、OOM 处置和 JVM 事故助手

## 与相邻章节的边界

- 第 3 章讨论 Java Memory Model：可见性、有序性、原子性和 happens-before。
- 第 9 章讨论用户 SLI、指标、压测、故障注入，以及 JFR 如何进入证据链。
- 第 10 章讨论 JVM 运行时内存、对象生命周期、GC、线程/锁和生产排障流程。
- 第 11 章将继续讨论网络、连接池、超时和容量，不把所有慢请求都归因于 GC。

“JMM”与“JVM 内存区域”不是同一概念，本章不会用堆、栈示意图解释并发可见性。

## 当前状态

- 阶段：八件套完整内容初稿，实验 Pending
- 调研日期：2026-07-15
- 已完成：讲义、NotifyFlow 应用方案、练习答案、面试追问、Teach-back 和一手来源清单
- 可复用先修证据：第 09 章已验证真实 `ThreadPoolExecutor` 拒绝路径及 4 条自定义 JFR 事件
- 未完成：GC 日志、分配热点、锁竞争、heap dump、NMT、Metaspace、Direct Memory、虚拟线程 pinning 和 OOM 实验

本目录不能标记为 Lab Verified、Release Candidate 或 Released。第 09 章 JFR Phase A 只证明自定义事件采集链路，不证明本章任何 GC、分配、锁或 OOM 结论已经实验验证。

## 学习顺序

1. `lesson.md`
2. `project-application.md`
3. `lab/README.md`（实验设计已完成，运行状态 Pending）
4. `exercises.md`
5. `answers.md`
6. `interview.md`
7. `teach-back.md`
8. `sources.md`

## 本章核心问题

1. JMM、JVM 规范运行时数据区和 HotSpot 实际内存分别回答什么问题？
2. 为什么“对象都在堆、引用都在栈”不是可靠的工程表述？
3. GC Roots、可达性和对象保留路径如何解释内存泄漏？
4. G1 的 Region、Young GC、并发标记、Mixed GC 和 Full GC 分别意味着什么？
5. Parallel、G1、ZGC 应按吞吐、延迟和内存占用如何选择？
6. 为什么 GC pause、GC cycle 和请求 P99 不能直接画等号？
7. heap 使用不高但进程 RSS 持续增长，可能来自哪些 native memory？
8. `Java heap space`、`Metaspace`、`Direct buffer memory` 和 `unable to create native thread` 如何区分？
9. 如何用 GC log、Metrics、Thread Dump、JFR、class histogram、heap dump 和 NMT 建立证据链？
10. Agent 可以辅助哪些诊断，哪些动作必须人工审批？

## 退出标准

- 能严格区分 JMM、规范内存区域和 HotSpot 实现。
- 能画出堆、线程栈、Metaspace、Code Cache、Direct Buffer 与进程 RSS 的关系。
- 能解释 GC Roots、强/软/弱/虚引用和 classloader leak。
- 能基于业务目标比较 G1、Parallel 和 ZGC，而不是背诵“最强 GC”。
- 能读取 GC 日志并区分高分配率、内存泄漏、堆过小和晋升压力。
- 能设计 heap/native/thread/lock 四条排障路径，并控制诊断动作风险。
- 能为 NotifyFlow 建立内存预算、对象生命周期、Agent 上下文预算和 OOM Runbook。
- 能在没有真实证据时明确说 Pending，不编造优化比例、线上故障或容量数字。

## 发布前缺口

- 创建并运行 Java 21 GC 日志基础实验。
- 完成 G1 分配压力、Humongous Object、老年代增长和 Full GC 对照实验。
- 完成 heap leak、classloader leak、Direct Buffer 和 native thread exhaustion 实验。
- 完成线程死锁、锁竞争、平台线程与虚拟线程 pinning 的 JFR/Thread Dump 证据。
- 完成 heap dump、MAT/同类工具保留路径分析和脱敏检查。
- 完成 NMT baseline/diff，并验证容器内 heap 外空间预算。
- 将 JVM 时间线与 NotifyFlow P99、错误率和业务正确性关联。
- 完成学习者作业与 5/15/45 分钟真实 Teach-back。

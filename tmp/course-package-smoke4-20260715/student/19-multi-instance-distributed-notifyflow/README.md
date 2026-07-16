# 第 19 章：多实例与多机分布式 NotifyFlow

## 章节定位

- 类型：Distributed Systems + Java Backend + Reliability + System Design + Lab Design
- 难度：深入
- 建议学习时间：28-36 小时
- 先修章节：MySQL、Redis、Kafka、恢复控制面、可观测性、Docker/Kubernetes、Agent Runtime
- 对应项目：NotifyFlow 多实例任务调度、租约、分片、配额和故障恢复

## 当前状态

- 阶段：八件套完整内容初稿，实验 Pending
- 调研日期：2026-07-15
- 已完成：多实例状态边界、租约/fencing、幂等、分片、限流、Outbox、补偿、扩缩容和事故设计
- 未完成：多进程/多节点、数据库/Kafka/Redis/Kubernetes 故障运行证据

本章不能标记为 Lab Verified、Release Candidate 或 Released。

## 核心问题

1. 单机代码扩成多实例后，哪些状态必须外置和版本化？
2. 任务如何只被一个实例拥有，同时允许故障后接管？
3. lease、heartbeat、fencing token 和分布式锁有什么区别？
4. 时钟漂移、网络分区和进程暂停如何破坏“锁还有效”的假设？
5. 分片、消费者组、一致性哈希和数据库领取分别适合什么场景？
6. 幂等、唯一约束、Outbox、Inbox 和补偿如何组合？
7. 全局/租户/渠道配额和 backpressure 如何避免恢复风暴？
8. 扩缩容、rebalance、节点下线和多区域故障如何观测与恢复？
9. Agent 如何辅助分布式事故分析但不成为新的控制面风险？

## 退出标准

- 能把单实例内存状态改造成数据库/事件/租约状态。
- 能设计 lease + fencing，解释旧 owner 为什么不能继续写。
- 能为任务、事件、回调和 Tool 设计幂等与唯一约束。
- 能选择数据库 SKIP LOCKED、Kafka 分区或显式分片方案。
- 能设计租户/渠道/供应商分层配额和恢复预算。
- 能处理扩缩容、rebalance、网络分区、时钟漂移和重复消息。
- 能输出数据正确性、lag、lease、fencing 和恢复时间线证据。

## 发布前缺口

- 完成 2-3 个 Java 进程并发领取与崩溃接管实验。
- 完成 lease 到期、旧 owner 恢复和 fencing 阻断实验。
- 完成 Kafka rebalance、重复消息、分区扩容和顺序实验。
- 完成 Redis/DB/网络分区和时钟偏移故障实验。
- 完成全局/租户/渠道限流和分阶段恢复实验。
- 完成 Kubernetes scale/节点下线和数据正确性验证。

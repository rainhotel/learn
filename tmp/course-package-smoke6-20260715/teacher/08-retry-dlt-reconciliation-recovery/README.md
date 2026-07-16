# 第 8 章：重试、DLT、对账与故障恢复控制面

## 章节定位

- 类型：Concept + Incident + Project + Lab + Interview + Teach-back
- 难度：深入
- 建议学习时间：20-28 小时
- 先修章节：Spring 事务、Redis 幂等、Kafka 可靠消息与 Outbox
- 官方资料：Spring for Apache Kafka 4.1.0、Apache Kafka 4.3.1、AWS Builders' Library
- 对应项目：NotifyFlow 错误分类、重试预算、供应商对账、DLT 处置和人工重放控制面

## 与第 7 章的边界

- 第 7 章回答：消息如何写入、复制、消费、提交 offset，以及为什么会重复或丢失。
- 第 8 章回答：错误发生后谁来恢复、重试多少次、何时停止、如何隔离、如何对账、如何安全重放。

本章不重复讲 Partition、ISR 和 Producer ACK，而是把恢复能力作为一个需要产品化的后台系统。

## 当前状态

- 阶段：完整内容初稿，前两组 Java 21 实验已验证
- 调研日期：2026-07-14
- 已完成：官方资料核验、讲义、NotifyFlow 恢复控制面设计、练习答案、面试、试讲，以及重试放大和 Full Jitter Java 21 实验
- 未完成：错误分类、Spring Kafka DLT、Unknown 对账、安全重放、操作台实现、真实事故演练和学习者 Teach-back

本目录不能标记为 Lab Verified 或 Released。

## 学习顺序

1. `lesson.md`
2. `project-application.md`
3. `lab/README.md`
4. `exercises.md`
5. 参考答案（提交后由教师解锁）
6. `interview.md`
7. `teach-back.md`
8. `sources.md`

## 本章核心问题

1. 哪些错误应该重试，哪些应立即失败或隔离？
2. 为什么多层各重试 3 次可能把一次请求放大为 243 次底层调用？
3. 指数退避为什么还需要上限、抖动、总时限和重试预算？
4. blocking retry、retry topic 和数据库延迟任务如何选择？
5. DLT 需要保留哪些来源、异常和业务证据？
6. 外部供应商超时后，为什么结果应为 `UNKNOWN` 而不是 `FAILED`？
7. 人工重放如何避免绕过幂等、权限和租户边界？
8. 补偿为什么不是数据库回滚，何时需要人工审批？
9. 如何用 SLO、告警和演练证明恢复能力？

## 退出标准

- 能设计错误分类表和单一重试责任点。
- 能计算重试放大倍数、总尝试次数和最坏恢复时间。
- 能设计带上限指数退避、抖动、令牌预算和截止时间的策略。
- 能解释 Spring Kafka blocking/non-blocking retry 的限制。
- 能设计 DLT、对账 case、replay batch 和审计表。
- 能处理供应商 `UNKNOWN`、回调乱序和重复回调。
- 能完成一次“下游全故障 → 暂停 → 恢复 → 限速追赶”的事故演练。
- 能明确哪些操作可以由 Agent 建议，哪些必须由人审批。

## 发布前缺口

- 完成八组恢复实验并保存真实时间线。
- 实现最小恢复控制面 API 和数据库表。
- 建立操作权限、双人审批或高风险确认机制。
- 完成至少一次 DLT 修复和小批量重放演练。
- 用压测数据确定重试预算与恢复容量。
- 完成学习者作业和真实 Teach-back。

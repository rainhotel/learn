# 第 7 章：Kafka 可靠消息、消费恢复与 Transactional Outbox

## 章节定位

- 类型：Concept + Lab + Project + Incident + Interview + Teach-back
- 难度：深入
- 建议学习时间：22-30 小时
- 主线版本：Apache Kafka 4.3.1、Spring for Apache Kafka 4.1.0
- 对照版本：Apache RocketMQ 5.0 官方文档
- 计划环境：Java 21、Docker、MySQL 8.x、Kafka 4.3.1、Spring Boot/Spring Kafka
- 对应项目：NotifyFlow 任务事件、Outbox 发布、消费幂等、重试、DLT 与人工重放

## 当前状态

- 阶段：完整内容初稿，实验待验证
- 调研日期：2026-07-14
- 已完成：Kafka/Spring Kafka/RocketMQ 官方资料基线、完整讲义、项目设计、练习答案、面试题、试讲稿，以及 Kafka 4.3.1 基础实验包静态 RED/GREEN 验证
- 未完成：Docker Engine 启动后的 Kafka 运行态实验、Spring Kafka 示例代码、故障注入报告、学习者作业和真实 Teach-back

本目录目前不能标记为 Lab Verified 或 Released。

## 学习顺序

1. `lesson.md`
2. `lab/README.md`
3. `project-application.md`
4. `exercises.md`
5. `answers.md`
6. `interview.md`
7. `teach-back.md`
8. `sources.md`

## 本章核心问题

NotifyFlow 引入消息队列后，必须能回答：

1. 消息成功写入 Kafka 的判定条件是什么？
2. `acks=all`、ISR 和 `min.insync.replicas` 如何共同决定可用性与耐久性？
3. Producer 幂等为什么不能替代消费者业务幂等？
4. offset 在处理前或处理后提交，各自会产生什么失败窗口？
5. 为什么 Kafka 的 exactly-once 不能直接证明“数据库和短信供应商只执行一次”？
6. rebalance、慢消费、毒消息和下游故障如何形成积压与重试风暴？
7. task 与消息如何避免“双写不一致”？
8. DLT 之后由谁处理，如何安全重放，如何留下审计证据？
9. Kafka 和 RocketMQ 在 Agent 平台、互联网后端与先进制造中如何选型？

## 概念依赖

```text
Topic / Partition / Replica / ISR
    -> Producer key / batching / acks / retry / idempotence
    -> Consumer group / poll / offset / rebalance
    -> at-most-once / at-least-once / exactly-once 边界
    -> retry / DLT / 人工重放
    -> Transactional Outbox
    -> schema 演进 / 监控 / 容量 / 故障恢复
```

## 计划实验

1. `acks`、副本和最小 ISR 的故障矩阵。
2. Producer 超时重试与幂等生产者重复对比。
3. 消费完成但 offset 未提交时崩溃，观察重复消费。
4. offset 先提交再处理时崩溃，观察业务处理丢失。
5. 同 key 分区内顺序与跨分区无全局顺序。
6. 停止消费者制造 lag，再恢复并记录追赶速度。
7. 增减消费者触发 rebalance，记录暂停时间和重复窗口。
8. poison message 进入重试链和 DLT，并完成人工修复与重放。
9. Outbox 发布成功但标记失败，验证重复发布与消费幂等。

## 退出标准

- 能画出 Producer、Broker、Partition、Replica、Consumer Group 与 offset 的状态流。
- 能根据业务实体选择 key，并解释顺序范围和热点风险。
- 能设计 `acks=all + replication.factor=3 + min.insync.replicas=2`，同时说明失去 ISR 时写入会失败。
- 能用崩溃时间线解释 at-most-once 和 at-least-once。
- 能明确 Kafka EOS 的系统边界，不承诺外部副作用 exactly-once。
- 能设计 task + outbox 同事务、至少一次发布、消费幂等、DLT 和人工重放闭环。
- 能用 lag、ISR、请求延迟和错误率判断系统是否正在失效。
- 能比较 Kafka、RocketMQ 和数据库任务表，给出基于需求的选择。

## 发布前缺口

- 所有计划实验获得真实命令输出、日志和时间线。
- 固定 Kafka、Spring Kafka、Docker 镜像和客户端 patch 版本。
- 建立可重复的 Spring Kafka 示例工程和 MySQL Outbox 表。
- 完成至少一次 Broker/Consumer/数据库故障注入报告。
- 学习者练习达到 80 分并完成真实 Teach-back。

## 下一步

1. 建立 Kafka 4.3 隔离实验环境。
2. 先完成 offset、顺序、lag 和 rebalance 四组基础实验。
3. 再接入 MySQL Outbox、消费幂等、重试与 DLT。
4. 保存指标、日志、失败时间线和修订结论。

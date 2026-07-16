# 第 9 章：可观测性、压测与故障注入

## 章节定位

- 类型：Concept + Lab + Project + Incident + Interview + Teach-back
- 难度：深入
- 建议学习时间：24-32 小时
- 先修章节：线程池、MySQL、Redis、Kafka、故障恢复控制面
- 资料基线：Google SRE、Spring Boot 4.1.0、Micrometer 1.17.0、OpenTelemetry、Grafana k6、JDK 21 JFR
- 对应项目：NotifyFlow SLI/SLO、指标日志 Trace、容量压测、故障演练和事故证据

## 与前两章的边界

- 第 7 章：解释消息链路为什么重复、丢失、乱序或积压。
- 第 8 章：设计失败后的重试、DLT、对账和人工恢复。
- 第 9 章：证明系统是否满足目标，以及故障发生时能否及时发现、定位和恢复。

## 当前状态

- 阶段：完整内容初稿；实验 1-4 基础验证与实验 9 Phase A 自定义 JFR 已通过，其余实验 Pending
- 调研日期：2026-07-14
- 已完成：官方资料核验、讲义、NotifyFlow 指标设计、长尾/负载模型/指标基数/线程池容量实验、真实有界线程池拒绝路径、自定义 JFR 事件、练习答案、面试与试讲初稿
- 已准备未验证：Spring Boot 4.1.0/Micrometer 1.17.0 最小工程、低基数 Timer、危险 tag 策略和 Actuator exposure 测试
- 已准备未运行：k6 开放/封闭/分阶段恢复脚本，Node.js 语法检查通过
- 未完成：Micrometer Maven 运行、k6 runtime/threshold、JFR GC/分配/锁分析、Dashboard、真实压测曲线、基础设施故障演练和学习者 Teach-back

本目录不能标记为 Lab Verified 或 Released。

## 核心问题

1. 指标、日志、Trace 和 Profile 分别回答什么问题？
2. SLI、SLO、SLA 和 error budget 有什么区别？
3. P99 为什么可能比平均值更重要？成功与失败请求为何要分开统计延迟？
4. Counter、Gauge、Timer 和 Histogram 如何选择？
5. 为什么不能把 taskId、userId、traceId 直接作为指标 tag？
6. 开放负载和封闭负载分别模拟什么？
7. coordinated omission 如何让系统看起来比真实情况更快？
8. 压测如何设置 threshold，使失败自动反映到退出码？
9. 故障注入如何控制爆炸半径、停止条件和恢复验证？
10. 如何把压测结果转成容量、告警和简历证据？

## 退出标准

- 能为 NotifyFlow 定义用户视角 SLI/SLO 和 error budget。
- 能设计低基数、可聚合的 Micrometer 指标。
- 能通过 traceId/eventId 关联日志、Trace、任务和消息。
- 能区分开放/封闭负载并识别 coordinated omission。
- 能设计 baseline、load、stress、spike、soak 和 recovery 测试。
- 能用吞吐、并发、延迟和 Little's Law 估算容量。
- 能设计受控的数据库慢、供应商 503、Kafka 停止、GC 压力等故障实验。
- 能产出可复现的压测报告和事故复盘。

## 发布前缺口

- 完成至少一个 Spring Boot/Micrometer 可运行示例。
- 将已验证的开放/封闭确定性模型升级为真实 k6 脚本和 threshold。
- 将已验证的长尾与基数模型升级为 Micrometer Histogram、`MeterFilter` 和真实资源开销实验。
- 完成容量拐点与 Little's Law 对照实验。
- 将已验证的真实拒绝路径升级为到达率压测、Micrometer 与 JFR P99 联动实验。
- 完成数据库、Kafka、供应商和 JVM 故障演练。
- 建立可公开展示的 Dashboard、压测报告和 JFR 分析。
- 完成学习者作业和真实 Teach-back。

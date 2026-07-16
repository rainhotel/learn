# 第 13 章实验：系统设计评审与项目答辩

## 当前状态

- 状态：Pending
- 已完成：评审与演练矩阵、证据结构和发布门槛
- 未完成：NotifyFlow 端到端运行、故障演练、陌生评审和答辩录像

## 实验矩阵

1. 容量估算与开放负载校准。
2. 创建 API 幂等与并发重复提交。
3. Outbox 发布器崩溃和重复发布。
4. 消费者崩溃、rebalance 与消费幂等。
5. Provider timeout -> UNKNOWN -> 查询/回调/对账。
6. Kafka 积压、背压、优先级降级和恢复风暴。
7. Redis 故障、缓存降级和数据库保护。
8. 数据库慢、连接池饱和与快速拒绝。
9. 跨租户、prompt injection、越权 replay 和审计测试。
10. 5/15/45 分钟陌生评审与追问复盘。

## 证据目录

```text
evidence/<review-or-experiment>/
  assumptions.md
  environment.md
  architecture.md
  workload.md
  raw-output/
  correctness.md
  questions.md
  conclusion.md
```

## 发布门槛

- 容量模型与运行误差有解释，不能只给纸面数字。
- 每次故障同时验证可用性、资源和最终数据正确性。
- 至少一名陌生评审者完成 45 分钟追问。
- 问题清单与设计修订形成可追踪版本。
- 没有运行和评审证据前，不得声称“生产级”或 Released。

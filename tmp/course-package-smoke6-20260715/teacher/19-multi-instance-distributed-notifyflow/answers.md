# 第 19 章参考答案与评分

## 核心答案

- lease 解决临时所有权，fencing 解决旧 owner 恢复后的陈旧写入。
- exactly-once 不作为端到端承诺；使用至少一次、幂等、状态机、对账和补偿。
- 全局配额不能简单等于每实例本地限流之和，副本变化会改变实际上限。
- 扩缩容/rebalance 是状态迁移，必须处理 in-flight、offset、lease 和重复。
- Agent 只读分析不等于控制权限，释放 lease/重置 offset/replay 必须审批。

## 评分

| 维度 | 分值 |
|---|---:|
| 状态/lease/fencing | 25 |
| 幂等/Outbox/补偿 | 20 |
| 分片/扩缩容 | 15 |
| 配额/backpressure | 15 |
| 故障和正确性证据 | 20 |
| Agent 边界 | 5 |

只写“加分布式锁”而没有 lease/fencing/故障恢复的答案不得超过 50 分。

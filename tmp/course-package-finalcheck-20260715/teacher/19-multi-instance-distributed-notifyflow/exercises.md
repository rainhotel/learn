# 第 19 章练习

1. 把单机 `ConcurrentHashMap` 任务 owner 改造成数据库 lease 设计。
2. 解释旧 owner 恢复为什么需要 fencing token。
3. 比较 SKIP LOCKED、Kafka consumer group 和显式 shard lease。
4. 为 API、Outbox、Consumer、Provider、Callback 和 Agent Tool 设计幂等键。
5. 设计租户/渠道/Provider/retry/replay 分层配额。
6. 设计时钟漂移和 GC pause 下的 lease 实验。
7. 设计 Kafka rebalance 时 in-flight 与 offset 的处理。
8. 设计 Kubernetes scale down 的优雅下线和强杀恢复。
9. 设计热点租户、分片迁移和一致性哈希对照。
10. 设计网络分区、慢 Provider、DB 不可用和重复回调故障矩阵。
11. 写一份分布式数据正确性 SQL/事件对账清单。
12. 设计 Agent 对 lease/fencing 事故的只读分析与审批边界。

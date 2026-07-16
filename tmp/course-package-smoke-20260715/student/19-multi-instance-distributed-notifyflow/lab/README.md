# 第 19 章实验：多实例任务、租约与恢复

## 当前状态

- 状态：Pending
- 已完成：实验矩阵、断言、故障模型和证据目录设计
- 未完成：多进程/多节点/数据库/Kafka/Redis/Kubernetes 运行

## 实验矩阵

1. 两个 Java 进程 SKIP LOCKED 领取，无重复 owner。
2. worker pause、lease 到期、另一实例接管。
3. 旧 worker 恢复，fencing token 拒绝陈旧写。
4. API/Outbox/Consumer/Provider/Callback 全链路重复事件。
5. Kafka rebalance、崩溃、offset 与 in-flight。
6. hash shard 扩容、热点租户和迁移。
7. 全局/租户/渠道/Provider 配额，多副本不超发。
8. DB/Redis/Kafka/Provider 网络分区与 slow 故障。
9. Kubernetes scale down、graceful termination 和强杀。
10. retry/replay 分阶段恢复与 backlog time-to-drain。
11. 时钟偏移、GC pause 和 heartbeat 丢失。
12. Agent 分布式事故摘要、只读查询和审批边界。

## 证据

```text
evidence/<experiment>/
  topology.md
  versions.md
  workload.md
  fault-timeline.md
  leases.csv
  offsets.csv
  metrics.json
  logs.jsonl
  correctness.sql
  conclusion.md
```

## 发布门槛

- 至少两个独立进程或 Pod。
- 有 pause/slow/partition，不只 kill。
- 有 lease/fencing、offset、幂等和配额证据。
- 有 Provider 副作用和最终状态对账。
- 未运行前不得写自动故障转移、全局限流准确率或集群容量。

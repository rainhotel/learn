# 第 19 章资料与验证状态

## 一手资料

1. MySQL 8.4 Locking Reads / SKIP LOCKED：<https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html>
2. Apache Kafka Design：<https://kafka.apache.org/documentation/#design>
3. Redis distributed locks：<https://redis.io/docs/latest/develop/use/patterns/distributed-locks/>
4. Kubernetes Leases：<https://kubernetes.io/docs/concepts/architecture/leases/>
5. Kubernetes graceful termination：<https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-termination>
6. Raft paper：<https://raft.github.io/raft.pdf>
7. The Chubby lock service：<https://research.google/pubs/the-chubby-lock-service-for-loosely-coupled-distributed-systems/>
8. Amazon Builders' Library: Timeouts, retries and backoff：<https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/>

## 使用规则

- 锁/lease 方案必须说明存储、时间、续约、fencing 和故障模型。
- Kafka/Redis/MySQL 的语义按固定版本和真实运行核验。
- 论文机制不能直接写成 NotifyFlow 已实现或已验证。

## 当前状态

| 项目 | 状态 | 证据 |
|---|---|---|
| lease/fencing/幂等理论 | 资料核验/讲义初稿 | 官方文档/论文 |
| 多进程领取与接管 | Pending | 尚无运行输出 |
| Kafka rebalance/顺序 | Pending | Docker Engine 未运行 |
| 全局限流/配额 | Pending | 尚无多实例证据 |
| Kubernetes scale/下线 | Pending | 尚无集群证据 |

本章不能标记为 Lab Verified、Release Candidate 或 Released。

# NotifyFlow 多实例架构

## 1. 任务表关键字段

```text
task_id, tenant_id, state, version
owner_id, lease_until, fencing_token
next_attempt_at, attempt_count
created_at, updated_at
```

领取在短事务中更新 owner/lease/token；Provider 调用在事务外进行；结果写回必须携带 task version 和 fencing token。

## 2. 多实例时序

```text
worker-A claim token=41
-> A pause
-> lease expires
-> worker-B claim token=42
-> B succeeds and writes token=42
-> A resumes and tries token=41
-> storage/provider adapter rejects stale token
```

## 3. 分片方案

- Kafka：`tenantId + channel` 作为 key 保留局部顺序。
- DB recovery scanner：按 hash bucket + lease 领取。
- 大租户：专属 quota/shard，避免挤占普通租户。
- Agent run：按 runId 事件顺序，副作用 Tool 通过全局幂等键保护。

## 4. 配额

入口、普通消费、retry、UNKNOWN reconciliation 和 replay 使用独立预算。恢复流量不能抢占正常流量，也不能超过 Provider 安全容量。

## 5. Kubernetes 下线

preStop/termination：readiness false -> 停止领取 -> 等待有界 in-flight -> 释放 lease/提交 offset -> 退出。超时后允许强杀，但依赖幂等和对账恢复。

## 6. Agent 事故助手

只读查询：实例/分区/lease/fencing/lag/quota/事件。高风险建议必须生成 preview 和 requiredApproval，不执行 scale、offset reset、shard move 或 replay。

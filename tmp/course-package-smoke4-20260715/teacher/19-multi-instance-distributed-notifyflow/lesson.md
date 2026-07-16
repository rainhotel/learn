# 第 19 章讲义：从单实例到多机器可靠执行

## 一、多实例首先是状态问题

单实例中的 `Map`、本地队列、定时器和“当前 owner”在多实例下不再是全局事实。业务状态必须放在数据库、消息系统或具备一致性语义的外部存储；本地内存只做可丢弃缓存。

先问：谁拥有任务？所有权持续多久？如何续约？旧 owner 恢复后如何阻止它继续写？

## 二、任务领取方案

### 数据库 SKIP LOCKED

多个 worker 在短事务中选择待处理行并 `FOR UPDATE SKIP LOCKED`，更新 owner/lease/status 后提交。适合数据库任务表、中等吞吐和强业务状态关联。不要在事务中调用下游。

### Kafka 消费者组

分区在消费者间分配，天然形成分片和顺序边界。rebalance 期间要处理 in-flight、offset 和幂等；分区数限制最大并行度。

### 显式分片

按 tenant/channel/task hash 分片，实例领取 shard lease。适合需要稳定归属和批处理，但要处理热点、扩容迁移和 shard 恢复。

## 三、lease 与 fencing

lease 是带有效期的所有权声明：

```text
owner_id
lease_until
fencing_token
version
```

仅检查本地时间不安全。旧 owner 可能因 GC pause、网络阻塞或进程挂起错过续约，然后恢复继续写。fencing token 是单调递增版本，下游写入必须拒绝旧 token，才能阻止“僵尸 owner”。

分布式锁若只返回“拿到了”，但下游不校验版本，就不能解决旧 owner 恢复问题。

## 四、时间不是全局真相

机器时钟可能漂移、跳变或不同步。超时和 lease 尽量使用存储端时间、单调时钟测量本地持续时间，并为漂移留安全裕量。日志时间用于分析，不应独自决定跨机顺序；业务顺序使用版本、offset、sequence 或数据库提交事实。

## 五、幂等与去重

常见层次：

- API idempotency key + tenant 唯一约束。
- Outbox eventId 唯一。
- Consumer Inbox/processed_event 唯一。
- Provider request key 唯一。
- Callback providerEventId 唯一和状态单调迁移。
- Agent Tool idempotency key + attempt 状态。

去重表要有保留期限和冲突语义。幂等不等于忽略重复；重复请求应返回已存在结果或明确冲突。

## 六、Outbox、Inbox 与补偿

Outbox 把业务状态和待发布事件放进同一数据库事务；异步发布可能重复，因此消费者幂等。Inbox 记录已消费事件；补偿用于已经提交且无法原子回滚的跨系统副作用。

不要承诺 exactly-once end-to-end。更可靠的表述是：至少一次传输 + 幂等处理 + 状态机 + 对账/补偿，达到业务效果的一致性。

## 七、分片与热点

hash 分片简单，但大租户、热点渠道和大 payload 会造成不均衡。一致性哈希减少扩缩容迁移，但不能自动消除热点。可使用虚拟节点、热点租户隔离、动态权重或专属分片。

分片键必须与顺序、权限和查询需求一致。按 taskId 分片可能打散同租户配额；按 tenant 分片可能形成大租户热点。

## 八、配额、限流和 backpressure

分层预算：

```text
global capacity
-> tenant quota
-> channel quota
-> provider quota
-> retry/recovery budget
```

每个实例本地限流会随副本数变化，不能直接当全局配额。可以使用集中计数、配额分片、令牌租赁或网关控制，并明确一致性/可用性取舍。

backpressure 要从 Provider 反向传播到消费者、队列和入口。下游故障时继续扩容消费者只会增加重试和积压。

## 九、扩缩容与 rebalance

新增实例需要领取新任务/分区，旧实例优雅停止：停止领取 -> 完成/转移 in-flight -> 提交 offset/释放 lease -> 退出。强杀可能造成重复，必须依赖幂等和恢复。

Kafka rebalance、Kubernetes rollout、HPA scale 和 shard migration 都是状态迁移，应记录 generation、owner、offset、lease 和时间线。

## 十、故障模型

- crash-stop：进程直接退出。
- pause：GC、调度或冻结，之后恢复。
- network partition：实例与 DB/Kafka/Provider 部分断开。
- slow：请求变慢但未失败，最容易造成队列和超时放大。
- duplicate/reorder：消息或回调重复、乱序。
- clock skew：时间判断不一致。

实验必须覆盖 pause 和 slow，不能只测试 kill 进程。

## 十一、数据正确性

分布式实验至少检查：

- 每个任务最终状态唯一且合法。
- Provider 副作用没有重复或重复可证明无害。
- Outbox/Inbox/offset/attempt 数量可对账。
- lease/fencing 不允许旧 owner 写入。
- 租户和渠道配额没有跨实例超发。
- 恢复后 backlog 单调下降，UNKNOWN 收敛。

## 十二、可观测性

低基数指标：owner count、lease renew failure、fencing reject、rebalance、lag、backlog、duplicate、idempotency hit、quota reject、recovery rate。

taskId/eventId 放日志/Trace，不放 metric tag。事故时间线关联 Kubernetes event、Kafka group、DB lease、Provider、Agent run 和业务正确性。

## 十三、Agent 边界

Agent 可以汇总拓扑、lease、lag、事件、日志和 Runbook，生成假设与只读查询。它不能直接迁移 shard、强制释放锁、修改配额、扩容消费者、重置 offset 或重放数据；这些动作需要审批、preview、fencing 和审计。

## 十四、实验全部 Pending

本章没有真实多进程/多节点运行证据。未运行前不得声称无重复、自动故障转移、全局限流准确率或集群容量。

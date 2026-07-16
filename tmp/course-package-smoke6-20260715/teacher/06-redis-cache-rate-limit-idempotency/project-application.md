# NotifyFlow 项目应用：Redis 只做可恢复的加速层

## 设计目标

在不改变数据库最终事实的前提下，使用 Redis：

- 限制租户和供应商调用速率。
- 缓存模板与渠道配置。
- 快速识别短期重复请求。
- 展示任务进度和实时计数。

## 数据所有权矩阵

| 数据 | 权威来源 | Redis 作用 | Redis 丢失后 |
|---|---|---|---|
| 通知任务状态 | MySQL | 进度缓存 | 回源数据库重建 |
| 幂等最终结果 | MySQL 唯一约束 | 快速拒绝和结果引用 | 查询数据库，正确性不变 |
| 模板/渠道配置 | MySQL/配置中心 | Cache Aside | 回源并重建 |
| 短窗口计数 | Redis | 权威窗口状态 | 按降级策略放行/拒绝/本地限流 |
| 计费流水 | MySQL/账务系统 | 不缓存或仅摘要 | 不能依赖 Redis 恢复 |
| Worker 领取权 | MySQL lease/version | 通常不使用 Redis 锁 | 数据库恢复扫描 |

## 一、租户与供应商限流

### key

```text
nf:rate:create:{tenantId}:{epochSecond}
nf:rate:provider:{providerId}:{epochSecond}
```

### Lua 固定窗口伪代码

```lua
local current = redis.call('INCR', KEYS[1])
if current == 1 then
  redis.call('EXPIRE', KEYS[1], ARGV[1])
end
return current
```

应用比较返回值与 limit。TTL 要覆盖窗口并留少量清理余量。

### 分层保护

1. 租户套餐额度。
2. 单接口保护。
3. 供应商 QPS 配额。
4. 全局数据库/线程池保护。

返回中包含 limit、remaining、reset time 和拒绝原因，便于客户端退避。

### Redis 不可用

- 普通创建接口：本地保守限流 + 数据库保护。
- 高成本短信：可选择 fail-closed 或降低配额。
- 紧急告警：可设置独立白名单和最小保障通道。

策略必须按业务风险配置，不能全局固定。

## 二、模板缓存

### key/value

```text
nf:cache:template:v3:{tenantId}:{templateId}
```

value 包含：

- `schemaVersion`
- `templateVersion`
- `content`
- `channel`
- `updatedAt`

### 读流程

1. 查 Redis。
2. miss 时查 MySQL。
3. 不存在则短 TTL 缓存空值。
4. 存在则带随机抖动 TTL 写入。

### 更新流程

1. MySQL 事务更新模板并写 Outbox 失效事件。
2. 提交后删除缓存。
3. 删除失败由 Outbox 消费重试。
4. 下一次读取回源并重建。

对强一致预览接口直接读数据库；发送执行可携带创建任务时确定的模板版本，避免缓存变化影响已创建任务。

## 三、短期幂等快速路径

### key

```text
nf:idem:create:{tenantId}:{idempotencyKey}
```

### 流程

1. `SET key PROCESSING NX PX processingTtl`。
2. 成功后尝试插入 MySQL 唯一记录和任务。
3. 唯一约束冲突时读取已有结果。
4. 成功后 Redis value 改为 `SUCCESS:{taskId}` 并设置结果 TTL。
5. 执行失败时根据异常类型删除、保留失败状态或等待过期。

### 崩溃窗口

- Redis 占位成功、数据库未写入：占位最终过期，请求可重试。
- 数据库写入成功、Redis 结果未更新：重复请求命中 PROCESSING 后查询数据库。
- Redis key 被淘汰：数据库唯一约束阻止重复任务。

## 四、任务进度缓存

MySQL 保留完整状态机；Redis 只缓存前端高频轮询摘要：

```text
nf:progress:task:{tenantId}:{taskId}
```

value 可包含 status、attempt、updatedAt。状态更新成功后异步刷新或删除缓存。若缓存比数据库旧，页面允许短暂陈旧，并提供强制刷新路径。

## 五、热点与大 key 约束

- 单模板热点：应用本地短 TTL 缓存 + Redis，减少网络热点。
- 单租户超大流量：限流 key 可按接口或供应商拆分，避免所有流量集中到一个 Cluster slot。
- 进度 Hash：禁止把一个租户全部任务放进单 Hash。
- ZSet 限流：每次写入同时清理窗口外数据，设置元素上限和报警。

## 六、为什么不使用 Redis 锁领取任务

任务领取已经由 MySQL `FOR UPDATE SKIP LOCKED`、lease、owner、version 和条件更新完成。再加 Redis 锁会引入：

- 两个状态源。
- 锁与任务状态不一致。
- Redis 故障转移和 TTL 边界。
- 更复杂的恢复顺序。

只有跨数据库资源且无法用任务表条件更新表达时，才重新评估锁方案，并强制使用 fencing token。

## 七、Agent/RAG 映射

### Agent 工具限流

- 按租户、工具、用户和外部供应商限流。
- 高风险工具 Redis 故障时 fail-closed。
- 执行幂等仍由数据库 invocation id 和工具侧协议保证。

### RAG 缓存

- 缓存查询 embedding、检索结果或重排结果时，key 必须包含模型、索引和文档版本。
- 语料更新后旧 key 可自然过期或按版本隔离。
- 不缓存包含敏感权限结果而忽略用户/租户维度。

## 八、指标与告警

### 业务指标

- 每租户 allowed/rejected。
- 幂等命中、数据库兜底和冲突次数。
- 模板缓存命中率、回源 QPS、重建并发。
- Redis 故障降级次数。

### Redis 指标

- P95/P99、超时、连接等待。
- used_memory、evicted_keys、expired_keys。
- slowlog、blocked clients。
- replication lag、failover。
- big/hot key 报告。

## 九、验收标准

- 删除 Redis 后，任务正确性和数据库唯一性仍成立。
- 限流 Lua 不产生无 TTL 泄漏 key。
- 模板缓存更新失败可通过 Outbox 重试失效。
- 缓存击穿压测时数据库 QPS 有明确上限。
- Redis 淘汰幂等 key 后，数据库仍拒绝重复创建。
- 所有 key 包含租户、用途和版本，且设定大小与 TTL 上限。
- 能演示 Redis 故障时各接口的 fail-open/fail-closed 策略。

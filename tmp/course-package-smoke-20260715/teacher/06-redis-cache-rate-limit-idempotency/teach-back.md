# Teach-back：15 分钟讲清 Redis 工程边界

## 5 分钟版本

### 0:00-0:45 问题

“Redis 幂等 key 被淘汰后，通知会不会重复创建？”由此说明 Redis 不是最终事实。

### 0:45-1:45 原子性

讲单命令、`MULTI/EXEC` 和 Lua；展示 `INCR`/`EXPIRE` 竞态。

### 1:45-3:00 三个场景

- 限流：Lua + TTL + 降级策略。
- 缓存：Cache Aside + 失效重试。
- 幂等：Redis 快速路径 + MySQL 唯一约束。

### 3:00-4:10 故障边界

异步复制、eviction、热 key/大 key、锁过期。

### 4:10-5:00 结论

Redis 用于可丢失、可重建、需要低延迟的状态；最终正确性留在持久化系统和业务协议。

## 15 分钟版本

1. 真实故障导入：幂等 key 淘汰。（2 分钟）
2. 数据结构、key、TTL 与 eviction。（3 分钟）
3. 限流竞态和 Lua。（3 分钟）
4. Cache Aside、击穿和一致性。（3 分钟）
5. 复制、Sentinel、Cluster、锁与 fencing。（3 分钟）
6. NotifyFlow 数据所有权总结。（1 分钟）

## 必须画出的图

### 数据所有权

```text
Client -> Redis fast path -> MySQL truth
              | miss/fail       |
              +---------------> |
```

### Cache Aside 竞态

画出旧读、数据库更新、缓存删除、旧值回填的时间线。

### 锁过期

```text
A token=10 ---- pause ---- resumes ----X rejected
           B token=11 ---- writes OK
```

## 演示顺序

1. 错误限流产生无 TTL key。
2. Lua 修复。
3. 热点 key 失效造成数据库 QPS 峰值。
4. eviction 删除幂等 key，数据库唯一约束阻止重复。
5. 旧锁持有者被 fencing token 拒绝。

当前实验未运行，正式试讲不得用预期结果冒充实际输出。

## 追问清单

- 固定窗口为什么会双倍突发？
- TTL 和 eviction 有何不同？
- `WAIT` 为什么不是强一致？
- Sentinel quorum 为什么不等于 failover 多数？
- hash tag 会不会制造热点？
- Java 原生序列化有什么风险？
- locking cache writer 的锁粒度是什么？
- watchdog 为什么仍需 fencing token？

## 评分

| 维度 | 分值 |
|---|---:|
| 真实问题导入 | 10 |
| 原子性与限流 | 20 |
| 缓存一致性 | 20 |
| 高可用与锁边界 | 20 |
| NotifyFlow 迁移 | 15 |
| 实验证据 | 10 |
| 来源和版本 | 5 |

80 分以上且实验证据非零，才进入发布修订。

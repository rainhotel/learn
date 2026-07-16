# Redis 实验设计

## 计划基线

- Java 21
- Docker 28.3.0
- Redis 7.4.x
- Spring Data Redis 4.1.x
- Lettuce
- JUnit 5、Testcontainers 或 Docker Compose

具体 patch 版本将在首次拉取并运行后固定。目前本机确认 Docker CLI 存在，但 `redis-cli` 未单独安装。

## 实验 1：限流竞态

### RED/故障

客户端执行 `INCR` 后模拟崩溃，不执行 `EXPIRE`。断言 key 的 TTL 为 -1。

### GREEN/修复

Lua 原子执行 INCR 和首次 EXPIRE。并发执行后所有窗口 key 均有 TTL，allowed + rejected 等于请求总数。

## 实验 2：固定窗口边界突发

在窗口末尾和下一窗口开头各发送一批请求，记录 100ms 内实际放行量。对比滑动窗口或令牌桶。

## 实验 3：缓存击穿

同一模板 key 过期时启动 100-500 并发读：

- 无保护。
- JVM single-flight。
- Redis 互斥重建。
- 逻辑过期。

记录数据库请求次数、总吞吐和 P95/P99。

## 实验 4：eviction 与幂等兜底

设置较小 `maxmemory`，持续写入缓存直到幂等 key 被淘汰。再次提交相同请求，验证 MySQL 唯一约束仍只产生一个任务。

## 实验 5：大 key 与延迟

构造大 String、Hash、ZSet，比较小 key 与大 key 操作延迟。使用：

```text
redis-cli --bigkeys
redis-cli --memkeys
redis-cli --keystats
SLOWLOG GET
```

实验容器内提供 `redis-cli`。

## 实验 6：锁过期与 fencing

Worker A 获得 token 10 后暂停超过 TTL；Worker B 获得 token 11 并写入；A 恢复写入。无 fencing 时可能覆盖，有 fencing 时下游拒绝 token 10。

## 实验 7：复制与 Sentinel

一主两从、三 Sentinel：

- 写入并记录 replication offset。
- 杀死主节点。
- 记录检测、选举、提升、客户端恢复时间。
- 对比不同故障时点的数据缺口。

## 实验 8：Spring Data Redis

- 验证普通多次模板操作是否使用同一事务连接。
- 使用 `SessionCallback` 执行 MULTI/EXEC。
- 模拟超时并验证 DISCARD 清理。
- 对比 non-locking 与 locking cache writer。
- 验证 JSON 与 Java 原生序列化的可读性和安全配置。

## 当前状态

| 实验 | 状态 | 证据 |
|---|---|---|
| 限流竞态 | Pending | 无运行输出 |
| 窗口突发 | Pending | 无运行输出 |
| 缓存击穿 | Pending | 无运行输出 |
| eviction 幂等 | Pending | 无运行输出 |
| 大 key | Pending | 无运行输出 |
| fencing | Pending | 无运行输出 |
| Sentinel | Pending | 无运行输出 |
| Spring Data Redis | Pending | 无运行输出 |

## 验收输出

- 固定镜像和依赖版本。
- 一键启动/停止命令。
- 测试与压测原始输出。
- P50/P95/P99、数据库回源、eviction 和 failover 时间线。
- 故障前、故障中、恢复后的数据状态。
- 结论区分官方保证、实验观察和工程建议。

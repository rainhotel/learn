# 第 6 章官方资料基线

## 调研说明

- 调研日期：2026-07-14
- 来源范围：Redis 官方文档 `redis.io/docs/latest/`
- 当前阶段：只记录已核对事实与待实验假设，不填入虚构实验结果
- 版本策略：官方 latest 文档可能包含 Redis 8.x 新命令；实验版本确定后必须逐项核对命令可用版本

## 1. Redis Data Types

- URL：https://redis.io/docs/latest/develop/data-types/
- 支持结论：
  - Redis 是数据结构服务器，不只是字符串缓存。
  - 官方文档列出 String、Hash、List、Set、Sorted Set、Stream、Bitmap、概率型结构等。
  - 数据结构应根据访问与更新模式选择。
- 课程应用：模板缓存、配额计数、延迟任务候选、去重集合和事件流选型比较。

## 2. `SET`

- URL：https://redis.io/docs/latest/commands/set/
- 支持结论：
  - `SET` 可在单条命令中组合条件写入和过期时间。
  - 选项包括 `NX`、`XX`、`GET`、`EX`、`PX`、绝对过期和保留 TTL 等；具体选项需按实验版本核对。
  - `SET key value NX PX ttl` 是单实例锁和短期幂等占位的基础原语之一。
- 边界：命令原子不等于完整业务协议正确；还要处理 owner、过期、重试和下游写入。

## 3. `EXPIRE`

- URL：https://redis.io/docs/latest/commands/expire/
- 支持结论：
  - 可以为 key 设置 TTL，并支持 `NX`、`XX`、`GT`、`LT` 条件。
  - 删除或覆盖 key 内容的命令会清除已有超时；部分就地修改不会清除，需按命令语义核对。
  - TTL 是数据生命周期的一部分，更新缓存时必须显式考虑是否保留或重设。

## 4. Redis Transactions

- URL：https://redis.io/docs/latest/develop/using-commands/transactions/
- 支持结论：
  - `MULTI`/`EXEC` 将命令排队，并在执行时序列化、连续执行，不会被其他客户端命令插入中间。
  - Redis 事务不提供关系数据库式的运行时回滚；某条命令在执行阶段报错时，其他命令仍可能执行。
  - `WATCH` 提供乐观锁语义，受监视 key 变化时 `EXEC` 会中止。
- 课程重点：不要把 Redis `MULTI/EXEC` 等同于 MySQL ACID 事务。

## 5. Lua Scripting

- URL：https://redis.io/docs/latest/develop/programmability/eval-intro/
- 支持结论：
  - Lua 脚本在 Redis 服务器内执行，可以组合多条命令和控制逻辑。
  - 脚本执行具有原子观察效果，适合消除客户端多次往返之间的竞态。
  - 脚本执行期间服务器不能穿插处理其他命令，因此脚本必须短小、有界，避免长循环和大 key 扫描。
- 课程应用：固定窗口限流、带 owner 校验的释放、条件扣减和幂等状态转换。

## 6. `INCR` Rate Limiter Pattern

- URL：https://redis.io/docs/latest/commands/incr/
- 支持结论：
  - 官方文档给出基于 `INCR` 的 API 限流模式。
  - `MULTI` 中组合 `INCR` 与 `EXPIRE` 可保证每次操作共同执行。
  - 单计数器方案若客户端执行 `INCR` 后未执行 `EXPIRE`，会产生竞态和泄漏 key。
  - 官方示例建议用 Lua 把 `INCR` 与首次设置 TTL 合并。
- 待扩展：固定窗口边界突发、滑动窗口、令牌桶和多机时钟问题需要额外实验。

## 7. Key Eviction

- URL：https://redis.io/docs/latest/develop/reference/eviction/
- 支持结论：
  - `maxmemory` 达到限制后，Redis 根据 eviction policy 决定拒绝写入或淘汰 key。
  - 策略包括 `noeviction`、allkeys 系列和 volatile 系列；volatile 系列只在带 TTL 的 key 中选择。
  - 如果没有带 TTL 的 key，volatile 策略可能表现得类似 `noeviction`。
  - 应监控 `evicted_keys`、`expired_keys`、内存和被拒绝命令，而不是只看命中率。
- 课程应用：证明短期幂等 key 可能因淘汰丢失，数据库唯一约束不能删除。

## 8. Redis Persistence

- URL：https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
- 支持结论：
  - RDB 是时间点快照。
  - AOF 记录写命令并在启动时重放。
  - 可以关闭持久化，也可以组合 RDB 与 AOF。
  - AOF 默认 every-second fsync 策略下，故障时仍可能丢失约一秒写入。
  - RDB、AOF 在恢复速度、文件大小、性能和数据损失窗口上存在权衡。
- 课程应用：缓存是否需要持久化取决于数据能否从数据库重建；不能因为开启 AOF 就把 Redis 当成绝对可靠唯一真相。

## 9. Distributed Locks with Redis

- URL：https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/
- 支持结论：
  - 单实例基础获取方式是 `SET resource random-value NX PX ttl`。
  - value 必须标识当前锁请求；释放时只能删除 value 仍匹配的 key，防止删除他人的新锁。
  - Redis 8.4 文档提供 `DELEX ... IFEQ`；旧版本可用 Lua 比较并删除。
  - 锁续期仍必须校验 owner，并限制续期次数。
  - 官方一致性说明明确建议长任务考虑 fencing token，不要假设进程存活期间锁一定仍属于它。
  - TTL 依赖非单调时钟，时钟跳变可能影响一致性。
- 课程应用：NotifyFlow 更优先使用数据库 lease/version；Redis 锁实验用于理解租约边界，不作为万能互斥方案。

## 10. Redis Replication

- URL：https://redis.io/docs/latest/operate/oss_and_stack/management/replication/
- 支持结论：
  - Redis 基础复制采用 leader-follower 模式，默认是异步复制。
  - 链路短暂中断时可尝试部分重同步；无法部分重同步时需要完整同步。
  - `WAIT` 可以等待指定数量副本确认，但不能把 Redis 变成强一致 CP 系统；故障转移时已确认写仍可能丢失。
  - 副本可用于只读扩展，但必须接受复制延迟和旧数据。
  - 主节点关闭持久化又自动重启可能以空数据集覆盖副本，官方明确提示这是危险配置。
- 课程应用：限流、幂等和锁的故障语义不能只用“有副本”证明安全。

## 11. Redis Sentinel

- URL：https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/
- 支持结论：
  - Sentinel 为非 Cluster 部署提供监控、通知、自动故障转移和客户端配置发现。
  - Sentinel quorum 主要用于判断主节点客观下线。
  - 实际执行故障转移还需要选举 leader，并获得 Sentinel 进程的多数授权。
  - 客户端应通过 Sentinel 获取当前主节点，而不是永久写死旧地址。
- 课程应用：故障转移实验必须验证客户端重连、短暂不可用、旧主恢复和数据缺口。

## 12. Redis Cluster Specification

- URL：https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/
- 支持结论：
  - Cluster keyspace 被划分为 16384 个 hash slot，基础映射为 `CRC16(key) mod 16384`。
  - hash tag 可让多个 key 落在同一个 slot，以支持多 key 操作。
  - 节点使用异步复制，故障和网络分区期间存在丢失已确认写的窗口。
  - 少数分区不可用；多数分区满足副本条件后可恢复服务。
  - Cluster 只支持数据库 0，客户端通常缓存 slot 到节点映射并处理重定向。
- 课程应用：key tag 既能支持原子多 key 操作，也可能制造租户热点槽，必须通过流量分布评审。

## 13. Diagnosing Latency Issues

- URL：https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/
- 支持结论：
  - 先测量操作系统 intrinsic latency，再判断 Redis 自身延迟。
  - 慢命令、大数据结构、网络、fork、AOF 磁盘 I/O、Transparent Huge Pages 和 swap 都可能制造尾延迟。
  - Redis 内部 latency monitor、slow log 和系统工具需要联合使用。
- 课程应用：缓存命中率高不代表系统健康，必须同时观察 P95/P99、慢命令、fork 和内存换页。

## 14. Redis CLI

- URL：https://redis.io/docs/latest/develop/tools/cli/
- 支持结论：
  - `--bigkeys` 查找元素数量大的 key。
  - `--memkeys` 和 `--keystats` 采样内存占用与长度。
  - `--hotkeys` 需要 LFU 类 maxmemory policy 才能工作。
  - `--scan` 基于 SCAN 遍历 key，支持 pattern 和 count。
  - `--intrinsic-latency` 可测量系统固有延迟。
- 课程应用：生产排查禁止直接把阻塞式全量 key 扫描当作常规手段。

## 15. Spring Data Redis Transactions

- URL：https://docs.spring.io/spring-data/redis/reference/redis/transactions.html
- 文档版本：Spring Data Redis 4.1.0
- 支持结论：
  - 普通 `RedisTemplate` 调用不保证多次操作使用同一连接；事务操作可使用 `SessionCallback`。
  - `multi()` 与 `exec()` 之间发生异常时，需要 `discard()` 清理连接的事务状态。
  - `setEnableTransactionSupport(true)` 会把连接绑定到当前线程事务。
  - 事务内写命令排队到提交；Spring Data Redis 对读命令可能使用新的非线程绑定连接。
- 课程应用：不要假设 JDBC 事务和 Redis 事务天然组成一个原子分布式事务。

## 16. Spring Data Redis Cache

- URL：https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html
- 文档版本：Spring Data Redis 4.1.0
- 支持结论：
  - 默认 `RedisCacheWriter` 是 non-locking，吞吐更高，但 `putIfAbsent`、`clean` 等多命令操作可能重叠。
  - locking writer 的锁是 cache 级别，不是单 entry 级别，并增加请求和等待。
  - 批量清理可选择基于 SCAN 的 batch strategy，避免无界 KEYS 风险；支持程度受驱动和部署模式影响。
  - Spring Data 的 TTI 类行为基于 `GETEX`，要求 Redis 6.2+，且所有访问路径都必须一致刷新 TTL。
- 课程应用：`@Cacheable` 不是一致性协议，缓存清理和并发行为仍需实验。

## 17. Spring Data RedisTemplate and Serialization

- URL：https://docs.spring.io/spring-data/redis/reference/redis/template.html
- 文档版本：Spring Data Redis 4.1.0
- 支持结论：
  - `RedisTemplate` 配置完成后可在线程间复用。
  - Redis 存储的是字节，Java 类型如何编码由序列化器决定。
  - `StringRedisTemplate` 使用字符串序列化，便于人工排查。
  - 官方警告 Java 原生反序列化可能触发远程代码执行，不应在不可信环境使用，通常优先选择 JSON 等格式。
- 课程应用：key 使用稳定可读字符串；value 需要版本字段、类型边界和兼容迁移策略。

## 已核验与待核验边界

### 已核验官方事实

- 命令与选项语义。
- Redis 事务的连续执行和无数据库式回滚。
- Lua 的原子执行与阻塞边界。
- eviction、RDB/AOF、锁 owner 与 fencing token 要求。

### 待实验

- 限流准确率、窗口突发和并发竞态。
- 热 key 对单线程事件循环和网络的影响。
- 大 key 删除与延迟峰值。
- 缓存击穿下数据库 QPS。
- AOF/RDB 恢复时间和数据损失窗口。
- 主从切换期间锁、幂等 key 与读一致性。
- Spring Data Redis 事务中读写连接和可见性。
- non-locking cache writer 并发操作差异。

## 待补官方资料

- Lettuce 连接复用、超时和重连语义。
- Redisson watchdog、可重入锁和 fencing 能力边界。
- Redis ACL、TLS 与多租户安全。

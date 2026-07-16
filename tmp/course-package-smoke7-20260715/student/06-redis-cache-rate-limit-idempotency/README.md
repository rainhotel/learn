# 第 6 章：Redis 缓存、限流与短期幂等

## 章节定位

- 类型：Concept + Lab + Project + Incident + Interview + Teach-back
- 难度：进阶
- 建议学习时间：18-24 小时
- 官方资料：Redis latest、Spring Data Redis 4.1.0
- 计划实验：Redis 7.4.x、Java 21、Docker、Spring Data Redis/Lettuce
- 对应项目：NotifyFlow 租户限流、模板缓存、短期幂等和热点治理

## 当前状态

- 阶段：完整内容初稿，实验待验证
- 调研日期：2026-07-14
- 已完成：官方资料基线、问题地图、完整讲义、项目应用、练习答案、面试和试讲初稿
- 未完成：Docker/Java 实验、故障注入、学习者作业和真实 Teach-back

本目录目前不能标记为 Lab Verified 或 Released。

## 学习顺序

1. `lesson.md`
2. `lab/README.md`
3. `project-application.md`
4. `exercises.md`
5. 参考答案（提交后由教师解锁）
6. `interview.md`
7. `teach-back.md`
8. `sources.md`

## 本章核心问题

NotifyFlow 引入 Redis 前，必须先回答：

1. 哪个真实瓶颈需要 Redis，而不是为了简历堆组件？
2. 缓存是可丢失的加速层，还是不可丢失的业务真相？
3. `INCR`、`EXPIRE`、Lua 和事务分别提供什么原子性？
4. 限流 key、窗口、突发流量和多租户配额如何设计？
5. Redis 幂等记录过期、淘汰或故障后，数据库如何兜底？
6. 缓存穿透、击穿、雪崩、热 key 和大 key 如何复现和治理？
7. 分布式锁过期后，旧持有者如何被 fencing token 阻止？
8. Redis 持久化、复制和故障转移是否足以承载唯一事实？

## NotifyFlow 场景范围

### 适合优先评估

- 租户或 API key 维度的短窗口限流。
- 模板和渠道配置的缓存。
- 任务进度的短期展示缓存。
- 数据库唯一约束之外的短期重复请求快速拒绝。
- 热点任务的计数、配额和统计。

### 不作为唯一真相

- 通知任务最终状态。
- 计费流水。
- 必须永久保留的幂等结果。
- 无法容忍重复执行的 Agent 工具副作用。

这些状态仍需数据库唯一约束、状态机、Outbox 或外部幂等协议兜底。

## 概念依赖

```text
数据结构与 key 设计
    -> TTL 与内存淘汰
    -> 原子命令 / MULTI / WATCH / Lua
    -> Cache Aside 与一致性
    -> 限流与短期幂等
    -> 热 key / 大 key / 击穿 / 雪崩
    -> 持久化、复制与故障边界
    -> 锁、租约与 fencing token
```

## 计划实验

### 1. 限流竞态

- 错误实现：客户端分开执行 `INCR` 和条件 `EXPIRE`。
- 故障：客户端在两条命令之间失败，key 可能没有 TTL。
- 修复：Lua 或明确的事务方案。
- 验证：并发请求数、允许数、拒绝数、TTL 和泄漏 key 数量。

### 2. 缓存击穿

- 构造一个高频模板 key 同时过期。
- 对比无保护、互斥重建、逻辑过期或 single-flight。
- 记录数据库 QPS、P95/P99 和恢复时间。

### 3. 缓存一致性

- 数据库更新后删除缓存。
- 注入删除失败、并发读写和重试。
- 说明 TTL 只能限制不一致时间，不能自动证明强一致。

### 4. 淘汰与幂等失效

- 配置 `maxmemory` 和不同 eviction policy。
- 观察幂等 key 被淘汰后重复请求是否穿透。
- 验证数据库唯一约束仍能阻止重复业务结果。

### 5. 锁过期与旧持有者

- Worker A 获得带 TTL 的锁后暂停。
- 锁过期，Worker B 获得新锁并更新 fencing token。
- Worker A 恢复并尝试写入。
- 下游只接受更大的 token，拒绝过期持有者。

## 退出标准

- 能根据访问模式选择 String、Hash、Set、ZSet 或 Stream，不按名称猜用途。
- 能解释 Redis 事务没有数据库式运行时回滚。
- 能用 Lua 消除限流的 `INCR`/`EXPIRE` 竞态，并说明脚本过长会阻塞服务器。
- 能设计多租户限流 key、窗口和失败降级策略。
- 能说明缓存和 Redis 幂等记录丢失后的数据库兜底。
- 能解释锁随机值、安全释放、TTL、续期、fencing token 和时钟边界。
- 能通过指标和故障实验说明 Redis 的收益与风险。

## 发布前缺口

- 八组 Docker/Java 实验获得真实输出。
- 固定 Redis、Spring Data Redis 和客户端 patch 版本。
- 完成缓存击穿与限流压测报告。
- 完成 Sentinel 故障时间线和数据缺口记录。
- 学习者练习达到 80 分并完成真实 Teach-back。

## 下一步

1. 使用 Redis 7.4.x、Java 21 和 Spring Data Redis 建立隔离实验环境。
2. 完成限流竞态、缓存击穿、淘汰、锁过期和故障转移实验。
3. 保存压测指标、命令输出和故障时间线。
4. 完成学习者练习和 Teach-back 后修订发布。

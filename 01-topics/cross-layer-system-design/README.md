# 跨层系统设计哲学

## Goal

这个主题研究操作系统、计算机组成、网络、数据库与高并发高可用 Web 系统中反复出现的设计问题。

目标不是记住更多中间件，而是建立一套可迁移的系统推理框架：

> 在资源有限、操作并发、组件可能失败、状态存在副本的环境中，如何保护关键不变量，并在正确性、延迟、吞吐、可用性、成本和可恢复性之间做出可解释的取舍。

## Why This Matters

- CPU Cache、页缓存、数据库 Buffer Pool、CDN 和 Redis 都在利用局部性，但一致性边界不同。
- Mutex、数据库锁、乐观并发控制和分布式租约都在协调冲突，但故障模型不同。
- 调度队列、线程池、连接池和消息队列都在处理资源竞争与速度不匹配，但队列不会创造容量。
- 文件系统 Journal、数据库 WAL、Kafka Log 和 Outbox 都使用日志与重放，但每一层保护的语义不同。
- 单机系统中的超时和进程失败相对明确；分布式系统中的超时通常只意味着“结果未知”。

## Core Questions

学习每个机制时固定回答：

1. 必须保护的不变量是什么？
2. 状态放在哪里，由谁拥有修改权？
3. 哪一步是原子点、线性化点或提交点？
4. 并发、重复、乱序、超时和崩溃会造成什么？
5. 为性能增加的缓存、副本、异步和批处理破坏了什么？
6. 过载时应该等待、拒绝、丢弃、降级还是隔离？
7. 如何通过测试、指标和故障注入证明设计成立？
8. 这个机制明确不能保证什么？

## Scope

### Included

- 系统推理：不变量、状态所有权、故障模型、线性化点、端到端语义。
- 单机基础：缓存层次、虚拟内存、并发、调度、I/O、文件系统与恢复。
- 数据库内部：索引、Buffer Pool、事务、MVCC、WAL、并发控制。
- Web 工程：多级缓存、线程池、连接池、消息队列、限流、背压、熔断、幂等。
- 分布式系统：部分失败、时间与顺序、复制、分片、一致性、共识、租约与 fencing。
- 高可用工程：SLI/SLO、容量、尾延迟、故障域、可观测性、灾难恢复和演练。

### Not Included Yet

- 不以背诵 Redis、Kafka、Kubernetes 命令为主线。
- 不在没有业务不变量的前提下堆砌微服务和中间件。
- 不直接实现生产级数据库或共识系统；复杂实验以教学模型为主。
- 不把所有操作系统机制强行映射到分布式系统，必须标注类比边界。

## Outcomes

完成后应能：

- 从业务需求中提炼关键不变量和失败语义。
- 区分线程安全、数据库事务一致性和分布式一致性。
- 解释缓存、锁、队列、日志、重试、副本和分片的收益与代价。
- 解释为什么 RPC 不是本地函数调用，分布式锁不是远程 Mutex。
- 为系统定义容量边界、过载策略、SLI/SLO、RTO/RPO。
- 用压测和故障注入验证架构判断。
- 对陌生系统完成一次包含正常路径与失败路径的设计审查。

## Learning Structure

1. 系统推理与测量
2. 并发、不变量与状态所有权
3. 局部性、缓存与副本
4. I/O、调度、排队与背压
5. 事务、日志与崩溃恢复
6. 网络失败、时间、重试与幂等
7. 复制、分片、一致性与共识
8. 高可用、过载控制与可观测性
9. 综合系统设计与故障演练

完整阶段计划见 `outline.md`，贯穿项目见 `projects.md`。

## Status

- 阶段：Phase 0 - 系统推理与测量
- 优先级：High
- 最近一次更新：2026-07-20
- 当前学习模式：完整大纲驱动 + 实验验证 + 贯穿项目
- 贯穿项目：高并发预约与库存服务 V0

## Connections To Existing Topics

- 计算机网络：`../408-computer-network/`
- 计算机组成存储系统：`../408-computer-organization-storage-system/`
- Java 后端工程实践：`../java-backend-big-tech-preparation/`
- TCP 演进专题：`../tcp-evolution/`

本主题负责跨层原理和判断框架；已有 Java 后端主题负责具体技术栈、求职项目和工程交付。

## Core Resources

### P0 主线

- [OSTEP](https://pages.cs.wisc.edu/~remzi/OSTEP/)：并发、虚拟化、持久化。
- [Designing Data-Intensive Applications](https://dataintensive.net/)：数据系统、事务、复制、分片与一致性。
- [CMU 15-445 Database Systems](https://15445.courses.cs.cmu.edu/)：存储、索引、事务、并发与恢复。
- [MIT 6.5840 Distributed Systems](https://pdos.csail.mit.edu/6.824/)：故障、Raft、复制与分片实验。
- [Google SRE Book](https://sre.google/sre-book/table-of-contents/)：SLO、过载、级联故障与可靠性工程。

完整资料分级、阅读目的和先修要求见 `resource-map.md`。

## Next 3 Actions

1. 阅读 `human-guide.md`，用其中的十问框架分析一次“库存扣减”。
2. 完成 Phase 0：定义贯穿项目的不变量、指标和失败模型。
3. 实现 V0 单进程预约/库存服务，并保存第一份吞吐—延迟基线。

## Human And AI Views

- 给人看：`human-guide.md`
- 给 AI 看：`ai-context.md`
- 完整大纲：`outline.md`
- 知识地图：`notes.md`
- 资料地图：`resource-map.md`
- 实验与项目：`projects.md`
- 方法与公式：`formula-sheet.md`
- 问题与误区：`qa.md`
- 进度跟踪：`progress.md`


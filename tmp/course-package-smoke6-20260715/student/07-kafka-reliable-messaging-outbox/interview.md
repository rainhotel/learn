# 面试追问与回答要点

## 1. Kafka 如何保证消息不丢？

先反问范围：Producer 到 Broker、Broker 存储、Consumer 处理，还是端到端业务结果？

回答结构：

1. Producer 使用 `acks=all`、合理超时与幂等重试。
2. Topic 配置足够副本与 `min.insync.replicas`，监控 ISR，谨慎 unclean leader election。
3. Consumer 在业务提交后确认 offset，采用至少一次。
4. 数据库与 Kafka 使用 Outbox；重复用唯一约束、状态机和供应商幂等处理。
5. retention、灾备、DLT、对账和故障注入共同决定最终可靠性。

## 2. `acks=all` 和 `min.insync.replicas` 是什么关系？

`acks=all` 要求 Leader 等待 ISR 中副本满足确认；`min.insync.replicas` 定义 ISR 至少剩多少时才允许成功写入。常见组合是 RF=3、min ISR=2、acks=all。ISR 少于 2 时写失败，以可用性换耐久性。

## 3. Kafka Producer 幂等解决什么？

解决同一 Producer 会话因发送重试产生的 Kafka 日志重复，依赖 Producer ID 和 sequence。它不负责：

- 两个相同业务请求。
- Consumer 重复处理。
- 外部 HTTP 副作用。
- 人工重放或 Outbox 重复发布。

## 4. 为什么 `max.in.flight` 会影响顺序？

关闭幂等时，如果多个请求在途，前一个批次失败重试而后一个先成功，日志顺序可能变化。幂等模式允许有限的在途请求并通过序列号维持协议语义，但仍要遵守官方配置约束。

## 5. Consumer 为什么会重复消费？

常见原因：

- 业务成功后、offset 提交前崩溃。
- rebalance 时处理未完成。
- offset 被重置或人工回放。
- retry topic/DLT 重放。
- Outbox 重复发布。

因此重复是设计输入，不是罕见异常。

## 6. 如何保证消费幂等？

分层回答：

- 事件层：`consumerName + eventId` 唯一键。
- 业务层：请求唯一键和条件状态更新。
- 外部层：供应商幂等 token、结果查询和对账。
- 观察层：记录 duplicate、冲突和重放批次。

## 7. 手动提交 offset 就可靠了吗？

不一定。关键是提交发生在业务事务前还是后、批量提交覆盖哪些记录、异常时容器如何处理，以及 rebalance 时是否仍有在途副作用。必须用崩溃实验验证。

## 8. Kafka exactly-once 如何实现？

Kafka 事务可以把向多个 Partition 写入和消费 offset 提交原子化；消费者配合 `read_committed` 隔离未提交记录。它主要覆盖 Kafka consume-process-produce。若写 MySQL 或调用供应商，需要外部系统协作、同库 offset、Outbox/Inbox 或业务幂等。

## 9. 什么是 Transactional Outbox？

在同一个数据库本地事务里写业务数据和待发布事件，独立 Publisher 再把 Outbox 发布到 Broker。它解决业务提交后消息未发送的双写丢失，但 Publisher 发送成功、标记失败会重复，所以仍是至少一次。

## 10. Outbox 表会不会成为瓶颈？

会。治理包括：

- `(status, next_attempt_at, id)` 覆盖扫描路径。
- 小批量、短事务、`SKIP LOCKED`。
- lease 防止永久 IN_FLIGHT。
- 分区/归档已发布记录。
- 限制 Publisher 对数据库的轮询 QPS。
- 规模增大后评估 CDC。

## 11. 如何保证消息顺序？

先定义范围。Kafka 只能保证 Partition 内顺序：

- 同一业务实体使用相同 key。
- Producer 和路由保持稳定。
- Consumer 不做无约束异步并发。
- 状态机使用 version/sequence 条件更新。
- 不追求无必要的全局单 Partition。

## 12. Consumer lag 很高怎么排查？

1. 判断全部 Partition 还是单个 Partition。
2. 对比生产速率、消费速率和处理耗时。
3. 检查下游 DB/HTTP、错误和 retry。
4. 检查 rebalance、GC、CPU、线程池和 poll 配置。
5. 检查热 key 和 poison message。
6. 计算净追赶能力，确认扩容是否真的有效。

## 13. poison message 怎么处理？

反序列化/Schema 永久错误不做无限重试。记录来源、异常和事件版本，有界重试后进入 DLT；修复代码或数据后小批量限速重放，并依赖业务幂等。

## 14. DLT 有什么缺点？

- 改变原始顺序。
- 容易成为无人负责的垃圾场。
- 重放可能制造二次流量峰值。
- 敏感 payload 可能泄露。
- 修复后是否应绕过原幂等需要明确策略。

## 15. Kafka 为什么快？

可回答分区并行、顺序追加、批处理、页缓存和零拷贝等方向，但不要把“顺序写”简化成所有场景都没有随机 I/O，也不要脱离端到端网络、压缩、复制和 Consumer 处理成本。

## 16. Partition 越多越好吗？

不是。更多 Partition 提高并发上限，但增加元数据、文件句柄、选举、rebalance、客户端内存和运维成本。还会影响 key 的重新映射和恢复时间。应由吞吐、顺序范围和故障恢复目标决定。

## 17. 如何处理消息积压？

- 先止住下游故障、毒消息或重试风暴。
- 计算 backlog 和净消化速率。
- 在 Partition 足够且下游能承受时扩 Consumer。
- 临时降级非核心事件或暂停 Producer 入口。
- 保护数据库和供应商，避免“追积压”造成二次事故。

## 18. Kafka 与 RocketMQ 怎么选？

Kafka 更适合事件流、日志、回放、数据生态和 Agent 平台；RocketMQ 在 Java 业务消息、事务消息、message group FIFO 和延迟消息上体验集中。还要考虑团队经验、已有基础设施、云服务、运维能力和招聘市场，不能只比较功能列表。

## 19. RocketMQ 事务消息是不是解决了分布式事务？

它通过 half message、本地事务、二次确认和状态回查，保证本地事务与消息生产的最终一致性。官方明确不保证下游消费结果与上游一致；消费者仍需重试、幂等和业务恢复。

## 20. 延迟消息可以代替任务调度系统吗？

不一定。RocketMQ 5.0 官方页面给出默认最大 24 小时、默认粒度 1000 ms，大量同一投递时间会产生负载。复杂日历、取消、修改、长期计划和可视化管理通常更适合数据库调度或专用调度器。

## 项目深挖追问

### 为什么大烨实习没有使用 Kafka，却在简历出现？

必须诚实回答：Kafka/Outbox 是暑期独立工程化重构项目，用类似通知业务作为问题背景，不是对原实习线上架构的描述。

### 你如何证明方案有效？

回答应包含真实完成后的证据：

- Broker/Consumer/数据库故障注入。
- eventId 重复数和业务结果数。
- lag 与追赶曲线。
- Outbox backlog 与恢复时间。
- DLT 修复和重放记录。
- 压测前后 P95/P99 与资源使用。

当前尚未运行实验时，不得声称已有这些结果。


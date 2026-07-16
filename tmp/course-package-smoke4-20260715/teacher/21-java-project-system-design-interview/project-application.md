# NotifyFlow 项目面试与证据答辩包

## 1. 使用目的

本文件把 NotifyFlow 可靠通知平台与 Agent 事故助手整理成可追问的项目答案。它不是简历成品，也不代表任何真实面试已经发生。

使用时必须遵守：

- 只描述个人真实完成的部分。
- 已运行、确定性模型、静态设计和计划分层表达。
- 每个数字绑定证据卡。
- 不能把独立项目包装为大烨实习内容。

## 2. 一句话项目定义

NotifyFlow 是一个多租户可靠通知与恢复平台：业务系统异步创建通知任务，平台使用数据库事实、Outbox、消息传递、幂等、UNKNOWN 对账和受控重放处理重复与部分失败；只读 Agent 事故助手基于授权证据辅助诊断，但不直接执行高风险动作。

这句话描述的是课程主项目目标。实际答辩时还要说明当前实现范围和 Pending 项。

## 3. 三档项目介绍

### 3.1 90 秒版本

按以下顺序组织：

1. 业务问题：外部 Provider 慢、重复、超时和回调乱序使“调用成功”不等于业务终态。
2. 核心方案：MySQL 保存任务与 Outbox 事实，Kafka 承载异步事件，Redis 只承担可重建的缓存/限流；Provider timeout 进入 UNKNOWN，由回调或对账收敛。
3. 可靠性：入口幂等、消费幂等、有界重试、DLT case、对账和审批重放共同处理失败。
4. 个人证据：说明自己真实完成的 Java、SQL、模型实验和设计文件，并指出未运行部分。
5. 取舍：不承诺端到端 exactly-once，Agent 默认只读，高风险动作走确定性控制面。

### 3.2 3 分钟版本

在 90 秒版本基础上加入：

- API 的 tenant + idempotency key + request hash 合同。
- task 与 delivery/attempt 分离的状态机。
- 线程池、连接池、Provider 配额和背压关系。
- 一项真实实验观察及其限制。
- 当前最大缺口和下一步验证。

### 3.3 8 分钟版本

加入架构图、写入时序、容量假设、故障决策表、观测与数据正确性查询。最后保留两分钟接受面试官选择方向。

### 3.4 15 分钟版本

形成正式项目答辩：

~~~text
需求与非目标
-> 架构与个人边界
-> API/状态机/数据模型
-> Outbox 与消费
-> timeout/UNKNOWN/对账
-> 过载、恢复和多实例
-> 可观测性与证据
-> Agent 权限与评测
-> 限制、ADR 和演进
~~~

## 4. 当前证据索引

以下索引用于训练“当场定位证据”，不是新的实验结论。

| 证据 ID | 当前状态 | 可支持的回答 | 不支持的回答 |
|---|---|---|---|
| E01-RUN-01 | JDK 21 已验证 | 线程池接收顺序、CallerRuns 反馈、中断合同 | 生产吞吐或最佳参数 |
| E03-RUN-01 | JDK 21 已验证 | volatile 发布、volatile 自增非原子、monitor HB | 所有硬件上的性能结论 |
| E04-SQL-01 | MySQL 8.0.40 已验证 | 联合/覆盖索引、SKIP LOCKED、隔离快照、死锁 | NotifyFlow 全链路容量 |
| E08-RUN-01 | 确定性 Java 模型已验证 | 多层重试放大、Full Jitter 分散尖峰 | 真实 Provider 恢复表现 |
| E09-LOAD-01 | 确定性模型已验证 | 长尾、开放/封闭负载、指标基数、容量拐点 | k6 真实 runtime |
| E09-JFR-01 | JDK 21 已验证 | 有界线程池拒绝路径、自定义 JFR 事件链路 | GC/锁/分配与 P99 因果 |
| D07-DESIGN-01 | 静态设计，Kafka runtime Pending | Outbox/消费幂等设计与验证计划 | Kafka 集群故障已验证 |
| D12-DESIGN-01 | 设计 Pending | Docker/Kubernetes 部署、探针和下线方案 | 集群发布和回滚已验证 |
| D17-DESIGN-01 | 设计 Pending | RAG 指标、引用、安全门禁 | 模型质量提升数字 |
| D18-DESIGN-01 | 设计 Pending | Agent Tool、Memory、状态机和审批边界 | Agent Runtime 已端到端运行 |
| D19-DESIGN-01 | 设计 Pending | lease、fencing、多实例恢复方案 | 多机器故障已验证 |

证据源：

- [第 01 章线程池实验](../01-thread-pool-async-notification/lab/README.md)
- [第 03 章 JMM 实验](../03-jmm-volatile-synchronized/lab/README.md)
- [第 04 章 MySQL 实验](../04-mysql-index-transaction-task-table/lab/README.md)
- [第 08 章恢复实验](../08-retry-dlt-reconciliation-recovery/lab/README.md)
- [第 09 章可观测性实验](../09-observability-load-test-fault-injection/lab/README.md)
- [第 12 章容器实验](../12-docker-kubernetes-deployment-foundations/lab/README.md)
- [第 17 章 RAG 评测实验](../17-rag-evaluation-citation-security/lab/README.md)
- [第 18 章 Agent Runtime 实验](../18-tool-memory-agent-runtime/lab/README.md)
- [第 19 章多实例实验](../19-multi-instance-distributed-notifyflow/lab/README.md)

## 5. 项目追问树

### 根问题 A：为什么做这个项目？

~~~text
业务价值是什么
-> 用户是谁
-> 为什么普通同步调用不够
-> 最大风险是丢失、重复还是延迟
-> 首版非目标是什么
-> 你怎样证明问题被改善
~~~

回答边界：不要虚构真实客户量、生产事故或商业收益。可以使用明确标注的教学假设。

### 根问题 B：整体架构是什么？

~~~text
入口 API
-> MySQL task/delivery/outbox
-> Outbox publisher
-> Kafka + channel workers
-> Provider + callback
-> reconciliation/replay
-> telemetry + Agent assistant
~~~

继续追问：

- 为什么 MySQL 是真相源？
- Redis 去掉会怎样？
- Kafka 不可用时能接收多久？
- 模块化单体与微服务如何选择？

### 根问题 C：怎样保证不丢、不重？

安全回答不是“exactly-once”，而是：

~~~text
数据库本地事务保存 task + outbox
-> 至少一次发布与消费
-> 稳定业务键和状态机吸收重复
-> timeout 进入 UNKNOWN
-> 回调/查询/对账收敛
-> 审批和配额下安全重放
~~~

追问到 Provider 不支持幂等时，应明确不能承诺绝对去重，需要暴露风险、减少盲目重试并加强人工确认。

### 根问题 D：并发和容量怎样控制？

~~~text
入口配额
-> 有界线程池
-> 连接池
-> Kafka 分区/consumer
-> Provider 并发与 QPS
-> retry/reconciliation/replay 独立预算
~~~

回答中引用 E01-RUN-01 或 E09-LOAD-01 时，要说明它们是本地实验/模型，不是生产容量。

### 根问题 E：MySQL 表和索引怎样设计？

至少说明：

- task 与 delivery 分离。
- tenant 作用域幂等唯一键。
- recovery 使用 state + next_attempt_at + id 的领取索引。
- callback/provider request 唯一键。
- 大 payload 与热状态分离。
- 领取查询通过执行计划和并发实验验证。

E04-SQL-01 可以支持 SKIP LOCKED、索引和隔离回答，但不能证明最终生产表设计已调优。

### 根问题 F：Kafka 和 Outbox 为什么需要组合？

主线：

- 业务库提交与 Kafka send 是两个资源。
- 同事务写 Outbox 保证“业务事实与待发事件”共同提交。
- Publisher 崩溃可重复发布，因此 consumer 仍需幂等。
- offset 与 Provider 副作用不原子，仍需 UNKNOWN 和对账。

D07-DESIGN-01 当前只能支持设计解释，Kafka runtime 仍 Pending。

### 根问题 G：最难的故障是什么？

推荐选择 Provider 接收请求但响应丢失：

1. 客户端看到 timeout。
2. 副作用可能已经发生。
3. 本地状态进入 UNKNOWN，不直接 FAILED。
4. 使用稳定 provider request id 查询，或等待授权回调。
5. 对账收敛后再决定是否重试。
6. 验证是否发生重复 Provider 副作用。

这是设计路径；对应真实 Provider 故障实验尚未完成。

### 根问题 H：怎样做可观测性？

回答分四层：

- 用户结果：接受率、完成率、端到端完成延迟。
- 流水线：Outbox oldest age、Kafka lag、retry/DLT/UNKNOWN backlog。
- 资源与依赖：线程/连接池、DB、Redis、Kafka、Provider。
- 正确性：幂等冲突、终态倒退、Outbox 漏发布、Provider 与本地状态差异。

E09-JFR-01 只支持自定义事件链路和拒绝路径，不能声称已经完成完整生产排障。

### 根问题 I：Agent 为什么存在，为什么不自动修？

Agent 用于读取授权后的 Metrics、Logs、Trace、任务时间线和 Runbook，输出带引用的摘要与只读建议。模型具有不确定性，也可能被间接 prompt injection 影响，因此 replay、扩容、清队列、改配额等动作进入独立控制面，使用 RBAC、preview、审批、幂等、配额、审计和 kill switch。

D17/D18 当前为设计证据，不可回答成已完成端到端评测。

### 根问题 J：你个人做了什么？

使用四栏回答：

| 类别 | 回答内容 |
|---|---|
| 自己实现 | 只列亲自完成且可打开的代码/SQL/实验 |
| 自己设计 | 架构、状态机、ADR、验证计划 |
| 课程已有 | 明确是课程框架或参考实现 |
| 尚未完成 | 基础设施运行、陌生评审和真实生产验证 |

若无法明确个人边界，暂停使用该项目描述并返回第 20 章事实台账。

## 6. 可安全使用的示例回答

### 线程池参数如何确定？

> 我不会先套 CPU 核数公式。NotifyFlow 的 Provider 调用属于受下游配额和连接限制的阻塞 I/O，我先用到达率、服务时间和下游安全并发估算在途量，再用开放负载观察 queue、rejected、P99 和完成吞吐的拐点。课程第 09 章的确定性模型显示固定容量下继续提高到达率只会增加排队和拒绝，吞吐不再增长；这支持容量推理，但不是生产参数。真实配置还要结合 Provider 限额、连接池和故障恢复预算校准。

### 为什么不直接用 Redis 做幂等？

> Redis 适合短期去重加速，但任务创建的最终幂等由 MySQL 的 tenant + idempotency key 唯一约束保护。Redis 故障、过期或主从切换不能让同一逻辑请求创建多个任务。若 key 相同但 request hash 不同，服务端返回冲突，而不是复用旧结果。

### Kafka 是否保证消息不重复？

> Kafka 可以配置可靠生产和提供事务能力，但在 consumer 更新 MySQL、调用外部 Provider 的端到端链路里仍可能重复。我的设计使用至少一次传递、业务幂等和状态机；Provider timeout 进入 UNKNOWN，通过查询或回调对账。当前 Kafka runtime 实验仍 Pending，所以我只把它描述为设计方案。

### 项目性能是多少？

> 目前不能给出 NotifyFlow 端到端生产吞吐。已有证据包括线程池容量模型、开放/封闭负载模型和真实有界 ThreadPoolExecutor/JFR 事件实验；k6、Kafka、Redis 与端到端 Provider stub 仍未全部运行。我可以展示现有环境和原始输出，也可以说明下一步如何做可复现压测，但不会把模型数字写成生产性能。

## 7. 反向核验问题

完成介绍后，请评审者随机选择：

1. 打开一个数字的原始证据。
2. 让学习者解释证据不能证明什么。
3. 修改一个约束，例如流量十倍或 Provider P99 变为 5 秒。
4. 删除 Redis、Kafka 或 Agent 中一个组件，要求重新设计。
5. 注入一次 timeout、重复消息、死锁或重启。
6. 追问个人贡献的具体类、SQL、实验步骤和修订。

任何一项无法回答，都应转成纠错卡，而不是用更多术语掩盖。

## 8. 系统设计联动题

题目：把 NotifyFlow 扩展为先进制造设备告警与工单通知平台。

需要先澄清：

- 设备数量、事件频率、告警等级和峰值风暴。
- 工厂网络是否间歇离线，边缘侧是否要缓冲。
- 同一设备事件顺序与去重窗口。
- 告警、工单、短信和控制命令的权限差异。
- 数据地域、工业协议、审计和安全要求。
- Agent 是否只能诊断，能否生成工单草稿。

设计至少覆盖：

- device event id 与 tenant/site/device 作用域。
- 边缘缓冲、中心 ingestion 和断线重放。
- 高优先级告警与普通通知隔离。
- 设备时钟偏差和事件时间。
- 工单状态机与人工确认。
- Agent 只读证据、引用与禁止直接控制设备。

这里仍是系统设计练习，不代表拥有制造企业生产经验。

## 9. 项目面试包完成定义

- 三档介绍稿事实口径一致。
- 每个技术选型有需求、替代方案、代价和重评条件。
- 每个数字能在 60 秒内打开证据。
- 至少覆盖重复、timeout、积压、数据库慢、跨租户和 Agent 越权六类追问。
- 评审者能指出至少三个薄弱点，学习者完成修订。
- 所有 Pending 内容在回答中明确标注。


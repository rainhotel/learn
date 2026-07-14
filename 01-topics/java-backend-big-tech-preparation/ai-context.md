# AI Context

## Current Goal

帮助用户在暑期将 Java 发展为主语言，形成大厂后端与 Agent/AI 应用后端的完整竞争力，并将全部学习成果生产为可教学、可验证、可售卖的文档型课程。

## Course Standard

- 每章必须达到 `../../05-meta/course-production-system.md` 定义的 L3 标准。
- 每章包含讲义、实验、项目应用、练习、答案、面试追问、来源和试讲复盘。
- 不能将 AI 生成文本直接视为课程完成，必须经过实验、试讲和修订。

## Resume Constraint

- 大烨实习不得虚构 MySQL、Redis、MQ 或 RAG 的使用。
- 新增技术应放入“基于历史业务场景的独立工程化重构项目”。
- 所有量化数据必须注明是历史线上数据还是暑期压测数据。

## Learning Dependencies

- MQ 依赖并发、事务和幂等基础。
- RAG 依赖基本后端接口、数据模型和评测意识。
- 微服务不是前置条件，项目先采用模块化单体。
- 简历改写依赖项目证据和事实核验。

## Current Production State

- 课程第 01-04 章已有完整初稿和已验证实验。
- 第 05 章 `course/05-spring-transaction-proxy/` 已完成讲义、项目应用、练习答案、面试、来源和试讲初稿。
- 第 05 章实验使用 Java 21、Spring Framework 7.0.8、H2 2.3.232；当前只有测试骨架，尚未获得 Maven RED-GREEN 证据。
- 不得把第 05 章标记为 Lab Verified 或 Released。
- 第 06 章 `course/06-redis-cache-rate-limit-idempotency/` 已完成完整内容初稿和官方资料基线，实验仍为 Pending。
- Redis 章节必须强调缓存/短期幂等不是唯一真相，数据库唯一约束和持久化状态机仍是兜底。
- 不得把第 06 章标记为 Lab Verified 或 Released。
- 第 07 章 `course/07-kafka-reliable-messaging-outbox/` 已完成完整内容初稿，实验仍为 Pending。
- 第 07 章固定 Kafka 为 NotifyFlow 与 Agent 事件平台主线，RocketMQ 仅作为中国 Java/制造业岗位对照。
- 第 07 章 Kafka patch 版本已固定为 4.3.1，基础 Compose 与 PowerShell 实验包已通过静态 RED/GREEN 验证。
- Docker Engine 当前未运行，Kafka 顺序和 offset/lag 脚本尚未获得运行证据；不得把静态验证扩写为 Lab Verified。
- 第 07 章必须强调 Kafka EOS 主要覆盖 Kafka consume-process-produce，MySQL 与供应商副作用仍需 Outbox、业务幂等、状态机、查询和对账。
- 不得把第 07 章标记为 Lab Verified 或 Released。
- 第 08 章 `course/08-retry-dlt-reconciliation-recovery/` 已完成完整内容初稿；重试放大和 Full Jitter 实验已通过 Java 21 验证，其余六组 Pending。
- 第 08 章真实实验结果：多层重试 243 次对单点重试 3 次，放大 81 倍；Full Jitter 峰值 1044 对固定退避 10000，比例 10.44%。
- 第 08 章与第 07 章边界：第 07 章讲消息机制，第 08 章讲错误分类、重试预算、DLT、Unknown 对账、补偿、安全重放和恢复控制面。
- 第 08 章必须强调 timeout 不等于 failed，多层重试会放大，DLT 不等于解决，Agent 不能绕过权限直接执行高风险恢复。
- 不得把第 08 章标记为 Lab Verified 或 Released。

## Recommended Stack

- Java 21、Spring Boot、Maven。
- MySQL、Redis。
- Kafka 4.3 为主线；RocketMQ 5.0 作为选型对照。
- PostgreSQL + pgvector 或 Milvus 二选一。
- Spring AI 或 LangChain4j 二选一。
- JUnit 5、Testcontainers、JMH、k6/JMeter、Docker Compose。

## Expanded Course Scope

- LLM 底层：Tokenizer、Embedding、Transformer、Attention、KV Cache、推理、量化、服务化。
- AI 应用：RAG、Tool Calling、Agent Loop、Memory、MCP、评测、安全和成本。
- Docker：namespace、cgroup、镜像层、网络、卷、Compose、资源与安全。
- Kubernetes：控制面、Pod、Deployment、Service、配置、探针、资源、发布与故障。
- 多机分布式：幂等、租约、分片、服务发现、限流、Outbox、补偿、可观测性。

所有扩展内容必须绑定 NotifyFlow/RAG 真实需求和可运行故障实验。

## Open Questions

- 大烨实习真实 Java/Python 职责边界。
- 当时是否实际接触数据库、缓存或消息中间件。
- 用户每周可投入的稳定时间。
- MQ、向量库和 Java AI 框架的最终选择。

## Next Actions

1. 获得 Maven 构建写权限后，完成第 05 章八组事务实验。
2. 使用 MySQL/Testcontainers 复验传播、连接池和真实数据库差异。
3. 固定第 06 章 Redis 实验版本，完成限流、击穿、eviction、fencing、大 key 和 Sentinel 实验。
4. 固定第 07 章 Kafka/Spring Kafka 实验版本，完成 offset、rebalance、lag、DLT、最小 ISR 和 Outbox 故障实验。
5. 实现第 08 章恢复控制面最小模块，运行重试放大、Unknown 对账和安全重放实验。
6. 安排用户完成第 01-08 章学习、练习和 Teach-back，依据反馈修订。
7. 继续进行实习事实访谈和简历证据整理。

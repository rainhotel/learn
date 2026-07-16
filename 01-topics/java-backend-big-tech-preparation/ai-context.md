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

- 课程级 `course/notifyflow-mvp/` 已建立五模块 Spring Boot 3.5.11 工程：domain、application、infrastructure、boot、provider-stub。
- 当前真实证据：Java 21 核心合同输出 `NOTIFYFLOW_CORE_CONTRACT_CHECK_PASSED`；Flyway V1 在 H2 2.3.232 MySQL mode 创建 task/attempt/outbox/reconciliation 四表。
- 当前静态实现：状态机、规范化 SHA-256 指纹、TaskCreationStore、JDBC task+outbox 事务、REST API、Provider Stub 三场景与测试源码。
- Maven/JUnit/Spring runtime 未通过；依赖下载提权审批服务返回 403。不得把 javac/H2 schema 扩写为 Spring 事务、HTTP 或完整 MVP 已验证。

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
- 第 09 章 `course/09-observability-load-test-fault-injection/` 已完成完整内容初稿；四组基础验证、真实有界线程池拒绝路径和实验 9 Phase A 自定义 JFR 事件已验证。
- 第 09 章真实实验结果：平均值 15.85 ms 对最大值 10000 ms；开放模型慢阶段丢弃 10500 次；低基数 6 条时序对 `taskId` 10000 条；线程池 250/s 时拒绝 2900 次、P99 600 ms、吞吐约 200/s。
- 第 09 章 Micrometer 最小工程已准备，但 Maven 依赖解析前被权限阻塞，当前没有有效 RED/GREEN，不能扩写为已验证。
- 第 09 章真实 JFR 证据：6 次提交接受 4、拒绝 2，录制文件含 4 条 `com.notifyflow.ProviderCall`；这只证明单 JVM 自定义事件链路，不证明 GC、分配、锁竞争、生产开销或分布式追踪。
- 第 09 章 k6 脚本已准备并通过 Node.js 语法检查，但本机未安装 k6；不得把脚本静态检查扩写为压测、threshold 或容量证据。
- 第 10 章 `course/10-jvm-memory-gc-production-troubleshooting/` 八件套已完成初稿，覆盖 JVM 运行时内存、GC、OOM、RSS、线程/锁、JFR、NMT、虚拟线程和 Agent 排障边界；本章实验全部 Pending。
- 第 11 章 `course/11-network-connection-pool-timeout-capacity/` 八件套已完成初稿，覆盖 DNS/TCP/TLS/HTTP、连接复用、分阶段超时、连接池、Little's Law、UNKNOWN、retry budget、SSE 和资源耗尽；本章实验全部 Pending。
- 第 11 章不得把连接链路描述成固定顺序：复用连接会跳过 DNS/TCP/TLS；MySQL/Kafka timeout 后的结果必须按阶段和副作用语义处理。
- 第 12 章 `course/12-docker-kubernetes-deployment-foundations/` 八件套已完成初稿，覆盖 namespace/cgroup/overlayfs、镜像、Compose、Pod/Deployment/Service、探针、资源、发布回滚、HPA、DNS 和安全；实验全部 Pending。
- 第 13 章 `course/13-system-design-project-defense/` 八件套已完成初稿，把 NotifyFlow 与 Agent 事故助手组织为需求、容量、API/数据、可靠性、多实例、观测、安全、成本、演进和答辩证据；实验与陌生评审全部 Pending。
- 第 14 章 `course/14-llm-principles-inference-rag-foundations/` 八件套已完成初稿，覆盖 Transformer/Attention/RoPE、prefill/decode/KV cache、Embedding/ANN、RAG 评测、引用、Tool/Memory/Agent Runtime 和 Java 后端边界；实验全部 Pending。
- 第 15 章 `course/15-document-processing-chunking-ingestion/` 八件套已完成初稿，负责多格式文档解析、OCR 边界、结构化清洗、chunk、metadata/ACL、版本/删除、去重、幂等和失败恢复；实验全部 Pending。
- 第 16 章 `course/16-hybrid-retrieval-rerank-vector-database/` 八件套已完成初稿，负责 sparse+dense 混合召回、filter、RRF、rerank、HNSW/IVF、pgvector/Milvus、索引生命周期和租户隔离；实验全部 Pending。
- 第 17 章 `course/17-rag-evaluation-citation-security/` 八件套已完成初稿，负责评测集、Recall/MRR/nDCG、faithfulness、answer correctness、claim-level 引用、拒答、ACL、红队和回归门禁；实验全部 Pending。
- 第 18 章 `course/18-tool-memory-agent-runtime/` 八件套已完成初稿，覆盖 Agent 状态机、Tool 合同、权限、幂等、UNKNOWN、审批、Memory、SSE、崩溃恢复、多 Agent 和 prompt injection；实验全部 Pending。
- 第 19 章 `course/19-multi-instance-distributed-notifyflow/` 八件套已完成初稿，覆盖多实例领取、lease/fencing、幂等、Outbox/Inbox、分片、配额、backpressure、扩缩容、时钟和故障恢复；实验全部 Pending。
- 第 20 章 `course/20-resume-fact-ledger-evidence-expression/` 八件套已完成初稿；任何大烨实习表述必须先通过事实台账，MySQL/Redis/MQ/RAG/Agent 等后学内容不得迁入实习。
- 第 21 章 `course/21-java-project-system-design-interview/` 八件套已完成初稿；闭卷、录音、模拟面试和真实反馈全部 Pending。
- 第 22 章 `course/22-algorithm-teach-back-validation/` 八件套已完成初稿；96 题是规划配额，不是已完成数量，成绩与陌生读者验证全部 Pending。
- 第 23 章 `course/23-job-strategy-portfolio-release/` 八件套已完成初稿；实时 JD、投递反馈、作品复现、版权和陌生学习者测试全部 Pending。
- 上述负载与基数结果是确定性模型，不是 k6、Micrometer heap 或 Prometheus 实测；真实 Micrometer/k6 与完整 JFR 诊断（GC/分配/锁/采样/P99 关联）仍为 Pending，Phase A 自定义事件链路已验证。
- 第 09 章与前章边界：第 07-08 章设计可靠与恢复机制，第 09 章负责通过 SLI/SLO、metrics/logs/traces/JFR、压测和故障注入证明机制有效。
- 第 09 章必须强调开放/封闭负载区别、coordinated omission、低基数 tag、机器化 threshold 和故障后的数据正确性。
- 不得把第 09 章标记为 Lab Verified 或 Released。

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

1. 建立课程级 NotifyFlow 最小纵切和一键环境检查：创建任务 -> MySQL/Outbox -> 消费 -> Provider stub -> UNKNOWN/对账 -> metrics。
2. 建立学生包/教师包构建，默认从学生包排除 `answers.md`，实现答案解锁。
3. 获得 Maven 构建写权限后，完成第 05/09 章 Spring/Micrometer 实验。
4. 启动 Docker 后运行 Redis、Kafka、Compose 和第 12/19 章核心故障实验。
5. 固定模型、Embedding、向量库和评测集，运行第 14-18 章最小 RAG/Agent 纵切。
6. 选择第 01-09 章中的一个模块进行陌生学习者 Alpha 试教，保存作业、评分、Teach-back 和修订证据。
7. 完成大烨实习事实访谈和第 20 章逐句简历审计。
8. 建立 `release-evidence/<version>/`、许可证报告、隐私检查、版本和勘误入口。

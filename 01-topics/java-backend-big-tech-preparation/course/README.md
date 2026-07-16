# 《Java 后端与 Agent 工程实战》课程目录

课程定位、学习结果、评分、证据等级和发布门槛见 [课程产品规格](product-spec.md)；章节先修关系见 [课程依赖图](dependency-map.md)；核心版与进阶版学时见 [学习路线与学时合同](learning-tracks.md)；每日与每周执行方式见 [十周核心版学习者手册](student-workbook.md)；求职证据和面试交付见 [求职证据包](job-readiness-pack.md)；统一用词见 [课程术语表](glossary.md)；逐章证据和下一验证动作见 [证据状态矩阵](evidence-matrix.md)；授课、编辑、版本和版权要求见 [教师与编辑规范](instructor-editorial-guide.md)；整课发布门禁见 [发布检查表](release-checklist.md)。

当前全课程文件、实验和发布缺口见 [2026-07-15 质量审计](quality-audit-2026-07-15.md)。

课程级集成项目的目标合同见 [NotifyFlow MVP 规格](notifyflow-mvp-spec.md)；学生/教师分包由 `tools/build-packages.ps1` 生成；当前内部 Alpha 发布证据见 [alpha-2026-07-15](release-evidence/alpha-2026-07-15/scope.md)。

## 使用方式

学习者默认顺序：讲义 -> 实验 -> 项目应用 -> 练习 -> 面试 -> 试讲 -> 复盘。`answers.md` 由教师或学习流程在提交后解锁，不进入首次阅读路径。

章节完成不等于发布。只有学习者完成作业与试讲并依据反馈修订后，才能进入发布状态。

## 已制作章节

| 编号 | 章节 | 当前状态 | 验证 |
|---|---|---|---|
| 01 | [线程池与异步通知任务](01-thread-pool-async-notification/README.md) | 完整初稿，待学习与试讲 | JDK 21 实验通过 |
| 02 | [HashMap 与集合选型](02-hashmap-collection-selection/README.md) | 完整初稿，待学习与试讲 | JDK 21 实验通过 |
| 03 | [JMM、volatile 与 synchronized](03-jmm-volatile-synchronized/README.md) | 完整初稿，待学习与试讲 | JLS 21 核对，JDK 21 实验通过 |
| 04 | [MySQL 索引、事务与任务表设计](04-mysql-index-transaction-task-table/README.md) | 完整初稿，待学习与试讲 | MySQL 8.0.40 SQL 实验通过 |
| 05 | [Spring 事务、AOP 代理与业务边界](05-spring-transaction-proxy/README.md) | 完整内容初稿，实验待验证 | Spring 7.0.8 官方资料已核对；Maven 执行权限待恢复 |
| 06 | [Redis 缓存、限流与短期幂等](06-redis-cache-rate-limit-idempotency/README.md) | 完整内容初稿，实验待验证 | Redis/Spring Data Redis 官方资料已核对；Docker 实验待运行 |
| 07 | [Kafka 可靠消息、消费恢复与 Transactional Outbox](07-kafka-reliable-messaging-outbox/README.md) | 完整内容初稿，实验待验证 | Kafka 4.3.1、Spring Kafka 4.1.0、RocketMQ 5.0 官方资料已核对；故障实验待运行 |
| 08 | [重试、DLT、对账与故障恢复控制面](08-retry-dlt-reconciliation-recovery/README.md) | 完整内容初稿，待补其余实验 | 重试放大与 Full Jitter Java 21 实验已通过；其余六组 Pending |
| 09 | [可观测性、压测与故障注入](09-observability-load-test-fault-injection/README.md) | 完整内容初稿，待补其余实验 | 基础实验、真实线程池/JFR Phase A 已通过；Micrometer/k6 runtime 与完整 JFR Pending，k6 脚本已静态检查 |
| 10 | [Java 21 JVM 内存、GC 与生产排障](10-jvm-memory-gc-production-troubleshooting/README.md) | 八件套完整初稿，实验 Pending | Oracle JDK 21/JFR/JCMD/JEP 资料已核对；尚无本章 GC/OOM/NMT 运行证据 |
| 11 | [网络、连接池、超时与容量](11-network-connection-pool-timeout-capacity/README.md) | 八件套完整初稿，实验 Pending | RFC/JDK/Hikari/Kubernetes 等资料已整理；网络、连接池、UNKNOWN 与 SSE 实验 Pending |
| 12 | [Docker、Kubernetes 与 NotifyFlow 部署基础](12-docker-kubernetes-deployment-foundations/README.md) | 八件套完整初稿，实验 Pending | Docker/Kubernetes 官方资料已整理；Docker Engine 与集群运行证据 Pending |
| 13 | [系统设计与项目答辩](13-system-design-project-defense/README.md) | 八件套完整初稿，实验 Pending | NotifyFlow/Agent 设计评审框架已完成；端到端演练、陌生评审和答辩录像 Pending |
| 14 | [LLM 原理、推理、RAG 与 Agent 基础](14-llm-principles-inference-rag-foundations/README.md) | 八件套完整初稿，实验 Pending | Transformer/RAG/ANN/Agent 一手资料已整理；模型、向量库和评测运行证据 Pending |
| 15 | [企业文档处理、切分与 Ingestion](15-document-processing-chunking-ingestion/README.md) | 八件套完整初稿，实验 Pending | 解析、OCR 边界、版本、ACL、幂等和失败恢复已设计；真实解析与 Embedding 管线 Pending |
| 16 | [混合检索、重排与向量数据库工程](16-hybrid-retrieval-rerank-vector-database/README.md) | 八件套完整初稿，实验 Pending | sparse/dense、RRF、rerank、pgvector/Milvus 和租户 ACL 已设计；运行与压测 Pending |
| 17 | [RAG 评测、引用与安全](17-rag-evaluation-citation-security/README.md) | 八件套完整初稿，实验 Pending | 评测集、引用验证、拒答、红队和回归门禁已设计；真实语料、模型与评测运行 Pending |
| 18 | [Tool、Memory 与可靠 Agent Runtime](18-tool-memory-agent-runtime/README.md) | 八件套完整初稿，实验 Pending | Agent/Tool/Memory/安全资料已整理；Java Runtime、工具与攻击评测 Pending |
| 19 | [多实例与多机分布式 NotifyFlow](19-multi-instance-distributed-notifyflow/README.md) | 八件套完整初稿，实验 Pending | lease/fencing/幂等/分片资料已整理；多进程、Kafka、K8s 故障证据 Pending |
| 20 | [简历事实台账与证据表达](20-resume-fact-ledger-evidence-expression/README.md) | 八件套完整初稿，验证 Pending | 大烨实习边界、证据等级、三类简历和压力审计已设计；真实逐句审计 Pending |
| 21 | [Java、项目与系统设计面试](21-java-project-system-design-interview/README.md) | 八件套完整初稿，模拟 Pending | 技术题图谱、追问树、评分与纠错协议已设计；闭卷和三轮模拟 Pending |
| 22 | [算法、Teach-back 与学习验证](22-algorithm-teach-back-validation/README.md) | 八件套完整初稿，执行 Pending | 96 题路线、间隔复习、白板协议和陌生读者测试已设计；完成数量均 Pending |
| 23 | [岗位策略、作品修订与课程发布](23-job-strategy-portfolio-release/README.md) | 八件套完整初稿，执行 Pending | JD 采样、投递实验、作品与发布门禁已设计；实时样本和陌生读者完整试用 Pending |

## 下一批章节

1. 完成 Spring 事务实验的 RED-GREEN 验证。
2. 完成 Redis 限流、击穿、eviction、fencing 和 Sentinel 实验。
3. 完成 Kafka offset、rebalance、lag、DLT 与 Outbox 故障实验。
4. 实现恢复控制面最小 API、对账任务与安全重放实验。
5. 将第 09 章基础模型升级为 Micrometer 指标、k6 开放负载、JFR 和故障注入实验，完成 Stage 2 V0.1 证据闭环。
6. 运行第 11 章网络分段、连接池饱和、UNKNOWN、SSE 和资源耗尽实验。
7. 固定模型、向量库和评测集，运行第 14-18 章实验。
8. 完成 Docker/Kubernetes 真实运行，再推进多机分布式。
9. 执行第 20-23 章事实审计、模拟面试、算法路线、JD 采样和陌生读者测试。

## 状态定义

- Research：资料研究中。
- Draft：讲义初稿。
- Lab Verified：实验已验证。
- Partial Lab：只表示部分实验获得证据，不是独立发布状态，章节仍需列出其余 Pending。
- Teach Pending：等待学习者试讲。
- Teach Verified：真实学习者完成作业和试讲，并依据反馈修订。
- Release Candidate：实验、教学、复现、版权和交付门禁已通过，等待陌生学习者最终试用。
- Released：陌生学习者验证、阻断问题关闭、版权检查和版本归档完成。

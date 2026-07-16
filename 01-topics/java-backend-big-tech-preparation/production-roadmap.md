# 课程制作路线

## 总体策略

学习路线和课程制作路线并不完全相同。学习时按依赖推进；制作时优先完成能形成项目闭环、最能检验能力、最适合试讲的模块。

## Stage 0：课程基础设施

- [x] 确定课程定位与学习者画像。
- [x] 建立章节模板和质量标准。
- [x] 建立资料分级和来源地图。
- [x] 建立课程产品规格、评分体系、证据等级和发布门槛。
- [x] 建立章节依赖图与排课规则。
- [x] 建立十周学习者执行手册、周测和复盘模板。
- [x] 建立岗位采样、事实台账、简历证据和面试交付模板。
- [x] 建立课程目录编号和模块依赖图。
- [x] 确定代码仓库结构与版本策略。
- [x] 建立术语表、风格指南和版权检查表。

## Stage 1：首批四章

先完成：

1. HashMap 与集合选型。
2. JMM、volatile 与 synchronized。
3. 线程池与异步通知任务。
4. MySQL 索引与通知任务表设计。

当前状态：

- 线程池章节：讲义、实验、练习答案、面试、来源和试讲稿已完成初稿；等待用户实际学习与试讲反馈后发布。
- HashMap 章节：讲义、JDK 21 源码核对、实验、练习答案、面试、来源和试讲稿已完成初稿；等待用户实际学习与试讲反馈后发布。
- JMM 章节：讲义、JLS 21 核对、可重复实验、练习答案、面试、来源和试讲稿已完成初稿；等待用户实际学习与试讲反馈后发布。
- MySQL 章节：讲义、MySQL 8.4 资料核对、8.0.40 SQL 实验、练习答案、面试、来源和试讲稿已完成初稿；等待用户实际学习与试讲反馈后发布。

选择原因：

- 对 Java 后端面试价值高。
- 能立即进入 NotifyFlow 项目。
- 易于设计实验和性能对比。
- 能快速测试课程模板是否有效。

每章依次经过 Research、Draft、Lab、Teach、Revise、Release。

## Stage 2：可靠通知主链路

5. Spring 事务与代理。
6. Redis 限流、缓存与幂等。
7. Kafka/RocketMQ 可靠消息。
8. Outbox、重试、死信与恢复。
9. 指标、压测和故障注入。

完成后发布课程 V0.1：可靠消息通知平台篇。

当前状态：

- Spring 事务章节已完成完整内容初稿，并映射到 NotifyFlow 短事务、Outbox、任务领取和供应商调用边界。
- Spring Framework 7.0.8 官方资料已核验。
- 实验已进入 TDD RED 阶段，但 Maven 写权限阻塞了实际执行；当前不能标记为 Lab Verified。
- Stage 2 的下一内容目标是 Redis 限流、缓存与幂等，同时保留 Spring 实验验证为最高优先补项。
- Redis 章节已完成完整内容初稿，覆盖缓存一致性、限流、短期幂等、高可用、Cluster、锁和 Spring Data Redis。
- Redis 八组实验仍未运行，不能标记为 Lab Verified；下一步与 Spring 实验权限恢复并行推进 Docker Redis 实验。
- Kafka 章节已完成完整内容初稿，Kafka 4.3.1 为主线，RocketMQ 5.0 用于事务/FIFO/延迟消息选型对照。
- 第七章已覆盖 Producer、ISR、Consumer Group、offset、rebalance、投递语义、Outbox、消费幂等、retry、DLT、Schema、监控与容量。
- 第七章九组实验仍为 Pending，不能标记为 Lab Verified；下一步固定 Kafka/Spring Kafka 镜像与兼容版本并运行故障时间线。
- 第八章恢复控制面已完成完整内容初稿，重点覆盖错误分类、单点重试预算、退避抖动、暂停熔断、DLT case、Unknown 对账、补偿、安全重放和权限审计。
- 第八章明确把 Agent 限制为证据检索、异常聚类和建议生成，高风险重放继续由确定性权限、审批和状态机控制。
- 第八章重试放大和 Full Jitter 两组实验已通过 Java 21 TDD 验证；其余六组仍为 Pending，章节整体不能标记为 Lab Verified。
- 第九章可观测性、压测与故障注入已完成完整内容初稿，覆盖 SLI/SLO、黄金信号、RED/USE、Micrometer、OpenTelemetry、k6 负载模型、容量和 JFR。
- 第九章四组基础验证、真实 `ThreadPoolExecutor` 拒绝路径和自定义 JFR 事件链路已通过；真实 Micrometer/k6、JFR GC/分配/锁分析及基础设施故障仍为 Pending，章节不能标记为 Lab Verified。
- Spring Boot 4.1.0/Micrometer 1.17.0 最小工程已准备，但精确依赖无缓存且联网审批 403，当前只能标记为运行 Pending。
- k6 开放/封闭/分阶段恢复脚本已准备并通过 Node.js 语法检查；本机未安装 k6，真实 threshold、报告和容量结论仍 Pending。
- Stage 2 V0.1 发布前必须把确定性模型升级为真实 Dashboard、开放负载 threshold、故障恢复和数据正确性证据。

## Stage 3：JVM 与生产工程

10. JVM 内存、GC 与排障。
11. 网络、连接池与超时。
12. Docker、测试和部署。
13. 系统设计与项目答辩。

完成后发布课程 V0.2：Java 后端工程篇。

当前状态：

- 第十章 JVM 内存、GC 与生产排障八件套已完成完整初稿。
- 已复用第九章 JFR Phase A 作为先修，但 GC、heap dump、NMT、Direct Memory、锁竞争、虚拟线程和 OOM 实验仍 Pending。
- 第十章进入实验设计与版本核验阶段，不能标记为 Lab Verified。
- 第十一章网络、连接池、超时与容量八件套已完成初稿，覆盖连接复用、分阶段超时、Little's Law、UNKNOWN、重试预算、SSE 断线恢复和端口/连接泄漏排查。
- 第十一章代理、Hikari/HTTP 池、网络故障、UNKNOWN、SSE 与资源耗尽实验全部 Pending，不能标记为 Lab Verified。
- 第十二章 Docker/Kubernetes 八件套已完成初稿，覆盖容器隔离、镜像工程、Compose、Kubernetes 对象、探针、资源、发布回滚、HPA 与故障排查。
- Docker Engine 和 Kubernetes 集群尚无运行证据，第十二章实验全部 Pending。
- 第十三章系统设计与项目答辩八件套已完成初稿，覆盖需求澄清、容量、API/数据、可靠性、多实例、观测、安全、Agent 边界、成本、ADR 和 5/15/45 分钟答辩。
- NotifyFlow 端到端演练、数据正确性验证、陌生评审和答辩录像全部 Pending。

## Stage 4：RAG 与 Agent 工程

14. LLM 原理、推理与 RAG 基础。
15. 企业文档处理、切分与 ingestion。
16. 混合检索、rerank 与向量数据库工程。
17. RAG 评测、引用与安全。
18. Tool、Memory 与 Agent Runtime。

完成后发布课程 V0.3：AI 应用后端篇。

当前状态：

- 第十四章 LLM 原理、推理、RAG 与 Agent 八件套已完成初稿。
- 覆盖 Transformer、Tokenizer、Embedding、KV cache、ANN、混合检索、评测、Tool、Memory、Agent Runtime 和 Java 后端边界。
- 模型、向量库、推理服务、评测集和 Tool 故障实验仍 Pending，不能标记为 Lab Verified。
- 第十八章 Tool、Memory 与可靠 Agent Runtime 八件套已完成初稿，覆盖状态机、Tool 合同、幂等、UNKNOWN、审批、Memory、SSE、崩溃恢复、多 Agent 和安全评测。
- Java Runtime、模型/工具集成、攻击样本和真实评测仍 Pending。
- 第十五章文档处理、切分与 ingestion 八件套已完成初稿，覆盖多格式解析、OCR 边界、结构保留、chunk、metadata/ACL、版本增量、删除、去重、幂等和失败恢复；实验 Pending。
- 第十六章混合检索、重排与向量数据库八件套已完成初稿，覆盖 sparse/dense、RRF、filter、rerank、HNSW/IVF、pgvector/Milvus、索引生命周期和租户隔离；实验 Pending。
- 第十七章 RAG 评测、引用与安全八件套已完成初稿，覆盖离线/在线评测、检索/生成指标、claim-level 引用、拒答、ACL、prompt injection、红队和回归门禁；实验 Pending。

## Stage 5：容器与多机分布式工程

### 容器篇

1. Docker 进程隔离、镜像层和资源限制。
2. Docker Compose 复现完整 NotifyFlow 依赖。
3. 非 root、健康检查、网络和日志。

### Kubernetes 篇

4. Pod、Deployment、Service、ConfigMap、Secret。
5. 资源、探针、滚动发布、回滚和 HPA。
6. 集群内服务发现、DNS、日志、事件和故障排查。

### 多机篇

7. 多实例任务领取与租约。
8. 分布式幂等、限流、锁和配额。
9. Outbox、消费者组、补偿和重试风暴。
10. TraceId、容量、故障注入和事故复盘。

完成后发布课程 V0.4：容器平台与分布式后端篇。

当前状态：

- 第十九章多实例与多机分布式 NotifyFlow 八件套已完成初稿。
- 覆盖 SKIP LOCKED、Kafka 分区、lease/fencing、幂等、分片、配额、backpressure、扩缩容、故障模型和数据正确性。
- 多进程/Pod、网络分区、时钟偏移、rebalance、全局限流和故障恢复实验全部 Pending。

## Stage 6：求职课程包

20. 简历事实台账与证据表达。
21. Java、项目与系统设计面试。
22. 算法、Teach-back 与学习验证。
23. 岗位策略、作品修订与课程发布。

完成后发布课程 V1.0。

当前状态：

- 第 20-23 章八件套均已完成初稿。
- 已建立大烨实习事实门禁、三类简历、技术追问树、96 题算法路线、模拟面试、JD 采样、投递实验、陌生读者和发布门禁。
- 真实简历逐句审计、算法完成记录、三轮模拟面试、实时 JD、投递反馈、陌生学习者完整试用和版权复核均为 Pending，因此不能发布 V1.0。

## 每周制作节奏

- 周一：问题地图、学习目标和资料研究。
- 周二：正文初稿与机制图。
- 周三：最小实验和代码。
- 周四：主项目应用与故障实验。
- 周五：练习、答案和面试题。
- 周六：试讲、录音和陌生读者测试。
- 周日：修订、版本发布和复盘。

## 单章工作量预算

- 资料研究：2-4 小时。
- 原理学习与实验：4-8 小时。
- 讲义写作：4-6 小时。
- 项目应用：3-6 小时。
- 练习、答案和试讲：2-4 小时。

深入章节预计需要 15-25 小时，不以每天批量生成章节为目标。

## 首次执行任务

第一章选择“线程池与异步通知任务”，因为它同时连接：

- 大烨消息通知业务。
- Java 并发核心能力。
- NotifyFlow 项目架构。
- 真实面经高频问题。
- 压测和故障实验。

第一章完成后，再根据模板缺陷调整整个课程生产系统。

第一章目录：`course/01-thread-pool-async-notification/`。

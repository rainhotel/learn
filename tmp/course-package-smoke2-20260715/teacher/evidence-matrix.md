# 第 01-23 章证据矩阵

## 1. 用途与判定口径

本矩阵回答三个问题：课程内容写到了什么程度，仓库中目前有什么强度的真实证据，以及下一步必须怎样验证。它是内部生产与发布控制表，不是完成度宣传页。

证据等级沿用 `product-spec.md`：

| 等级 | 含义 |
|---|---|
| L1 | AI 生成或个人理解草稿 |
| L2 | 官方文档核验 |
| L3 | 源码、配置或脚本静态检查 |
| L4 | 确定性模型实验 |
| L5 | 本地真实组件运行 |
| L6 | 故障注入、时间线和数据正确性验证 |
| L7 | 陌生环境复现与学习者独立完成 |

判定规则：

- “真实证据级别”记录当前仓库能够支持的最高等级，同时在括号中限定它证明的范围。
- 静态检查只能证明文件结构、语法或配置满足检查器，不能证明 Kafka、k6、Spring、Kubernetes 等运行成功。
- 确定性模型只能证明模型内的关系，不能替代真实吞吐、延迟、资源和故障恢复结果。
- “最低可展示证据”是允许做受限演示所需的最小证据，不等于 `Release Candidate` 或 `Released`。
- 所有章节还共同受学习者作业、Teach-back、陌生读者复现、版权与版本归档门禁约束。

## 2. 章节矩阵

| 章 | 内容状态 | 真实证据级别 | 主要 Pending | 最低可展示证据 | 下一验证动作 | 发布阻塞 |
|---|---|---|---|---|---|---|
| 01 线程池与异步通知 | 完整初稿 | L5：JDK 21 真实运行，线程池接收、拒绝、CallerRuns 和关闭实验通过；当前章节内未单列原始证据报告 | 学习者练习、三层追问、15 分钟 Teach-back；NotifyFlow 集成和过载时间线 | 保存 JDK 版本、编译/运行命令、`ALL_EXPERIMENTS_PASSED` 原始输出和源码版本，只展示受控线程池语义 | 重跑 `ThreadPoolLab.java`，把控制台输出、环境和限制落入证据文件；随后接入 NotifyFlow 做拒绝与数据正确性实验 | 缺可复核原始证据包、项目级故障验证和独立教学验证 |
| 02 HashMap 与集合选型 | 完整初稿 | L5：JDK 21 真实运行，key 契约、可变 key、树化、容量和 fail-fast 实验通过；当前章节内未单列原始证据报告 | 学习者练习与 Teach-back；源码版本边界和 NotifyFlow 集合选型复核 | JDK 版本、`--add-opens` 命令、成功输出、关键观察及“实现细节不等于接口保证”说明 | 重跑 `HashMapLab.java` 并保存原始输出；对照 JDK 21 源码记录树化和容量观察边界 | 缺独立证据文件、源码核验记录和陌生学习者复现 |
| 03 JMM、volatile 与 synchronized | 完整初稿 | L5：JDK 21 真实运行，可见性、volatile 复合操作、monitor happens-before、可重入和异常解锁实验通过；当前章节内未单列原始证据报告 | 学习者闭卷推理、反例辨析和 Teach-back；NotifyFlow 配置发布/状态保护集成 | JDK 版本、确定性交错方式、成功输出和每个实验“不证明什么”的边界 | 重跑 `JmmLab.java` 保存输出；增加闭卷 litmus 推理记录和 NotifyFlow 并发状态测试 | 缺原始证据包、项目集成证据和独立口述验证 |
| 04 MySQL 索引、事务与任务表 | 完整初稿 | L5：MySQL 8.0.40 真实运行，索引、`SKIP LOCKED`、隔离级别和死锁实验通过；章节保留 SQL，但未见独立原始结果包 | `EXPLAIN ANALYZE`、双会话时间线、锁/死锁原始输出归档；学习者复现与 Teach-back | 固定版本/数据量、建索引前后计划、双会话命令与时间线、死锁 victim、最终数据查询 | 在隔离实例重跑 10 个 SQL 脚本，保存计划、事务会话、死锁日志和最终状态快照 | 缺可复算的原始 SQL 证据、干净环境复现和学习者独立完成 |
| 05 Spring 事务、AOP 代理与业务边界 | 完整初稿 | L3：`pom.xml` 与事务测试骨架存在；Maven 未成功编译运行，不能主张 Spring runtime | 代理、自调用、异常回滚、`rollbackFor`、`REQUIRES_NEW`、rollback-only、新线程八组实验；MySQL/Testcontainers 复验 | 真实 `mvn test` 输出、依赖版本、八组断言和数据库前后状态；至少证明调用是否经过代理 | 解锁依赖下载/写入后运行 Spring 7.0.8 + H2 2.3.232 测试，再用 MySQL/Testcontainers 复验核心路径 | Maven 运行证据为零，传播与回滚语义未验证，缺学习者试讲 |
| 06 Redis 缓存、限流与短期幂等 | 完整初稿 | L2：讲义与官方资料核验；无 Redis runtime，实验均 Pending | 限流竞态、窗口突发、击穿、eviction 幂等、大 key、fencing、Sentinel、Spring Data Redis | 固定 Redis 版本/config，保存并发请求、Lua/事务结果、Redis 指标及 MySQL 唯一约束最终状态 | 启动隔离 Redis，先做“并发限流竞态 + eviction 后数据库幂等兜底”两组闭环实验 | 无组件运行、故障恢复和数据正确性证据；Docker/Redis 环境未就绪 |
| 07 Kafka 可靠消息与 Outbox | 完整初稿 | L3：Compose 解析和 PowerShell 脚本静态 RED/GREEN 通过；Kafka runtime 为零 | broker 启动、分区顺序、offset/lag、rebalance、poison/DLT、最小 ISR、Outbox 重复发布和 Java/Spring Kafka | Kafka 版本、broker/consumer 原始日志、消息 key/partition/offset、MySQL Outbox 与业务最终状态；静态报告只能作为附录 | 启动 Docker Engine，运行现有顺序和 offset/lag 脚本，保存容器日志与命令输出，再扩展 Outbox 故障实验 | 静态检查不能替代 Kafka；无容器、消息时间线、业务幂等和学习者证据 |
| 08 重试、DLT、对账与恢复控制面 | 完整初稿 | L4：Java 21 确定性模型已验证重试放大和 Full Jitter；2/8 实验通过，不是中间件 runtime | 错误分类、真实 DLT、timeout→UNKNOWN、回调乱序、安全重放、分阶段恢复六组实验 | 可展示现有 243 对 3、固定退避峰值 10000 对 Full Jitter 1044，但必须标注为确定性模型；章节完整展示还需一条真实 UNKNOWN→对账收敛时间线 | 实现零依赖错误分类后，接 Kafka/MySQL 做 timeout、DLT、查询/回调对账与最终状态验证 | 只有模型证据；无真实副作用、DLT、审计重放和恢复闭环；Teach-back Pending |
| 09 可观测性、压测与故障注入 | 完整初稿 | L5（受限）：确定性模型为 L4；真实有界 `ThreadPoolExecutor` 拒绝路径和单 JVM 自定义 JFR 事件链路为 L5；k6 静态检查仍是 L3 | Micrometer runtime、k6 threshold、GC/分配/锁 JFR、Dashboard、基础设施故障、UNKNOWN 和 soak | 可展示现有线程池 6 次提交/4 接受/2 拒绝、4 条自定义 JFR 事件及模型结果；必须声明不证明真实容量、GC 因果或分布式链路 | 解锁 Maven 运行 Micrometer 测试；安装 k6 后对真实服务执行开放负载和 threshold；再做 JFR Phase B | 缺真实服务压测、机器化门禁、完整 telemetry、故障后数据正确性与持续运行证据 |
| 10 JVM 内存、GC 与生产排障 | 八件套初稿 | L2：官方资料和实验设计；GC/OOM/NMT/JFR runtime 为零 | heap/native/thread/lock、GC 日志、OOM、NMT、heap dump、虚拟线程 pinning、容器预算、Runbook | 固定 JDK/堆/GC/负载，保存 GC 日志、JFR、NMT/线程/heap 证据与时间线；自定义 JFR 事件不能代替这些证据 | 先运行一组可控分配压力，对比 GC 日志、JFR 和 RSS；再分别制造 heap OOM 与锁竞争 | 全部实验 Pending，无诊断闭环、故障恢复和陌生环境复现 |
| 11 网络、连接池、超时与容量 | 八件套初稿 | L2：官方资料、矩阵和断言设计；网络/池 runtime 为零 | keep-alive/TIME_WAIT、Hikari 饱和、分阶段 timeout/deadline、UNKNOWN、retry budget、SSE 恢复、连接泄漏/端口耗尽 | 固定负载下的客户端与服务端时间线、socket/pool 指标、超时阶段、请求最终状态和清理后资源回落 | 搭建本地 Provider stub 与代理，先验证连接复用和 read timeout→UNKNOWN→查询收敛 | 无真实网络、连接池、资源和副作用证据；容量结论不可发布 |
| 12 Docker、Kubernetes 与部署基础 | 八件套初稿 | L2：官方资料和实验设计；无可执行清单或集群 runtime | 镜像构建、Compose、探针、Service/DNS、资源/OOM、滚动发布/回滚、节点故障 | 镜像 digest、构建日志、部署清单、Pod 事件/日志、readiness 切流、回滚和业务正确性 | 先为最小 NotifyFlow 服务创建可复现镜像与 Compose；再在 kind/minikube 运行探针和回滚实验 | Docker Engine/集群未运行；无镜像、部署清单运行、故障与恢复证据 |
| 13 系统设计与项目答辩 | 八件套初稿 | L2：设计方法、评审矩阵和演练合同；无端到端运行或外部评审 | NotifyFlow 端到端演示、容量实测校准、十组评审/演练、5/15/45 分钟答辩和陌生评审 | 一份版本化设计稿、容量假设、ADR、真实运行演示、一次故障时间线和评审记录 | 以 04/08/09 已有证据组装最小 NotifyFlow 评审包，邀请陌生评审者完成首轮 45 分钟答辩 | 纸面设计未被运行或评审验证；不能宣称生产级或答辩通过 |
| 14 LLM 原理、推理、RAG 与 Agent 基础 | 八件套初稿 | L2：原理与官方资料核验；模型、Embedding 和推理服务 runtime 为零 | tokenizer/attention、推理参数、量化、Embedding/ANN、基础 RAG、Tool 安全、Java Model Gateway | 固定模型/版本/硬件/输入，保存 token、延迟、内存、输出和参数；模型输出示例不能冒充质量结论 | 选择可获准的最小模型与 Embedding，运行同一提示的参数对照和 Java SSE/timeout 实验 | 未固定模型与环境，无推理、检索、评测和成本证据 |
| 15 企业文档处理、切分与 Ingestion | 八件套初稿 | L2：官方资料、实验合同和安全边界；无语料、实现或 runtime | 测试语料、解析/OCR、结构保留、清洗、chunk、metadata/ACL、版本删除、去重、幂等、失败恢复 | 固定且脱敏的语料 hash、逐页路由、解析/OCR 原始输出、chunk manifest、ACL 和版本状态、失败恢复时间线 | 先落盘小型 PDF/HTML/表格 gold corpus，实现确定性解析与 manifest，再注入 worker 中断验证续跑 | 无固定语料、Java 服务、质量指标、删除/权限和故障恢复证据 |
| 16 混合检索、重排与向量数据库 | 八件套初稿 | L2：官方资料、实验合同和指标定义；Verified 实验数 0 | 评测集、BM25/vector/hybrid、RRF、rerank、pgvector/Milvus、ACL、生命周期、Java 集成、容量 | 固定 corpus/query/gold 与索引版本，保存候选 ID/score、可复算 Recall/MRR/nDCG、ACL 负向断言和延迟分位数 | 在第 15 章固定语料上建立 BM25 与一个向量库基线，运行同快照 sparse/dense/hybrid 对照 | 无服务/索引运行、质量可复算结果、权限负向证据和容量边界 |
| 17 RAG 评测、引用与安全 | 八件套初稿 | L2：评测合同、红队矩阵和发布门禁；仓库尚无 `rag-eval` Runner | gold/forbidden 语料、人工标注、检索/生成指标、引用复取、拒答、ACL canary、prompt injection、敏感数据、回归/shadow | 固定 split/configHash，保存逐 case 检索/context/claim/citation、人工标签、硬失败清单和可复算汇总 | 先实现 `validate/run/compare` 最小 Runner 与 30–50 条脱敏集，完成引用 hash 和跨租户 canary | 无 Runner、评测集、人工 gold、红队与回归运行；任何质量百分比均不可发布 |
| 18 Tool、Memory 与可靠 Agent Runtime | 八件套初稿 | L2：官方资料、状态机和攻击矩阵设计；无 Java Runtime/模型/工具运行 | Tool schema/RBAC/幂等、UNKNOWN、审批、SSE 恢复、Memory 生命周期、攻击样本和多 Agent 消融 | 固定模型与工具版本，保存 run/step/toolCall 状态、授权决定、幂等键、审批、SSE 重连和副作用最终状态 | 先实现单 Agent Java 状态机和一个只读/一个副作用工具，注入重复调用与 timeout 做 UNKNOWN 对账 | 无运行态、权限负向测试、副作用恢复和评测；不能声称自动化成功率 |
| 19 多实例与多机分布式 NotifyFlow | 八件套初稿 | L2：分布式故障模型与断言设计；无多进程/多节点 runtime | `SKIP LOCKED`、lease/fencing、Kafka rebalance、分片、全局配额、网络分区、K8s 下线和 Agent 事故分析 | 至少两个独立进程/节点，保存 owner/lease/fence/offset、故障时间线、重复副作用计数和最终对账 | 先用两个本地 JVM + MySQL 验证租约过期与 fencing，再扩展 Kafka rebalance 和 K8s 终止 | 无多实例运行、分区/故障注入、全局正确性和容量证据 |
| 20 简历事实台账与证据表达 | 八件套初稿 | L2：事实边界和审计方法已设计；用户事实尚未逐句核验 | 大烨经历访谈、fact ledger、数字来源、三类简历、PDF/文本一致性和逐句追问 | 每条 claim 关联 `fact_id`、来源、归属、可公开证据和“不证明什么”；新增技术明确归为独立项目 | 以原简历和用户访谈建立事实台账，先审计大烨每一句，再生成一版 Java/Agent 组合简历 | 缺用户确认、量化 L5 证据、三版一致性、隐私处理和真实面试验证 |
| 21 Java、项目与系统设计面试 | 八件套初稿 | L2：题库、追问树、评分表和复测规则已设计；无真实作答 | 闭卷口述、错误前提纠正、项目证据追问、系统设计、三轮模拟和跨轮复盘 | 一次不看答案的完整录音/转写、逐题评分、被要求打开的代码/输出以及修订后复测 | 先做 60 分钟基线模拟面试，抽取 01–09 章和 NotifyFlow 五层追问，一周后复测 | 无学习者表现、录音、评分一致性和真实面试反馈 |
| 22 Java 算法、Teach-back 与学习验证 | 八件套初稿，96 题为计划配额 | L2：训练协议、证据合同和里程碑设计；无真实刷题/教学结果 | M0–M6、首次独立作答、边界测试、间隔复习、六次讲解、三次陌生读者测试、证据检查器 | 每题 first attempt、提示状态、编译/测试输出和复杂度；讲解需录音、反馈及 v1→v2 修订 | 先完成 M0 三题基线并保存原始记录，再实现证据检查器的最小统计功能 | 96 题、正确率、复习和 Teach-back 均未发生；无陌生学习者验证 |
| 23 岗位策略、作品修订与课程发布 | 八件套初稿 | L2：采样、投递和发布门禁已设计；无实时市场或发布验证 | 实时 JD、三类岗位样本、投递批次/反馈、作品干净环境复现、版权/隐私/Secret、勘误和 RC 审计 | 带日期和 URL 的原始 JD 样本、编码表、样本限制；一次干净环境作品复现和完整发布检查表 | 用户再次明确授权后用 Playwright 采集公开 JD；并行执行 NotifyFlow 干净环境复现与版权清单 | Playwright 访问曾因审批 403 未采样；无投递反馈、陌生复现、版权审计和 RC 证据 |

## 3. 当前可诚实对外展示的范围

当前只能做受限的内部 Alpha 展示：

- 第 01-04 章可以展示已运行实验及其教学设计，但应先补齐独立原始证据包，且不能称为已发布课程。
- 第 08 章只能把重试放大和 Full Jitter 结果称为“确定性模型实验”。
- 第 09 章只能把真实线程池拒绝路径和自定义 JFR 事件链路称为本地运行证据；负载、基数和容量模型不能称为 k6、Micrometer 或生产实测。
- 第 07 章只能展示静态实验包准备情况，不能展示为 Kafka 实验通过。
- 第 05-07、10-23 章目前主要可展示课程结构、讲义、项目设计、实验合同和来源核验，不能展示运行指标或通过率。

在全部章节完成对应实验、作业、Teach-back、陌生环境复现、版权与版本归档之前，课程状态保持内部 Alpha；没有任何章节达到 `Released`。

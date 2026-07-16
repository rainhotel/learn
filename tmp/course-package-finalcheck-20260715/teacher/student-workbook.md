# 十周求职核心版学习者执行手册

## 1. 使用方式

这份手册把课程章节转成每日行动和可验收成果。每天结束时必须留下文档、代码、实验输出、题目答案或 Teach-back 中至少一种证据。

本手册只对应 200-250 小时的求职核心版，不代表十周完成 23 章完整学时。完整进阶路线、选读边界和三类学习者入口见 [学习路线与学时合同](learning-tracks.md)。

默认投入：每周 20-25 小时，六天学习，一天复盘。若时间不足，减少并行主题，不降低实验和诚实门槛。

## 2. 每日四段式

### A. 回忆（20 分钟）

- 不看笔记回答昨天的三个核心问题。
- 画一张机制图或写一段关键伪代码。
- 标记“会解释、会应用、会排障、会教学”中的当前级别。

### B. 深学（60-90 分钟）

- 阅读一个讲义小节和对应一手资料。
- 写出为什么重要、适用条件、反例和未知点。
- AI 可以解释和追问，但不能替代学习者先作答。

### C. 实践（90-150 分钟）

- 先写假设、输入、断言和停止条件。
- 先观察 RED/失败，再实现或修正。
- 保存命令、版本、原始输出和边界。

### D. 输出（30-60 分钟）

- 完成一道练习、一个面试回答或一次 5 分钟 Teach-back。
- 把稳定知识整理进章节，把过程和失败写进 journal。
- 写明下一步最小动作。

## 3. 每周节奏

| 日 | 任务 | 必须产出 |
|---|---|---|
| 周一 | 问题地图与基线测试 | 本周目标、先修缺口、10 道预检题 |
| 周二 | 原理和机制 | 机制图、反例、术语卡片 |
| 周三 | 最小实验 | RED/GREEN 或真实 Pending 原因 |
| 周四 | NotifyFlow 应用 | DDL/API/状态机/时序图 |
| 周五 | 故障与面试 | 故障时间线、5 个追问 |
| 周六 | Teach-back 和项目演示 | 15 分钟录音/讲稿、听众问题 |
| 周日 | 复盘和间隔复习 | 周报、错题、下周计划 |

## 4. 证据等级

每项成果标记：

```text
R1 理解草稿
R2 官方资料核验
R3 静态代码/配置检查
R4 确定性模型实验
R5 真实组件运行
R6 故障注入与数据正确性
R7 陌生环境/学习者独立复现
```

简历量化描述至少要求 R5；课程发布候选尽量达到 R6-R7。

## 5. 第 0 周：基线与环境

目标：知道自己缺什么，不在工具安装和知识焦虑中反复切换。

任务：

- Java 80 题基线：语法、集合、并发、JVM、Spring、MySQL、Redis、MQ。
- 算法基线：数组、链表、哈希、二叉树、二分、DFS/BFS。
- 记录 JDK、Maven、Docker、Node、数据库版本。
- 建立大烨实习事实台账：真实职责、代码、结果、可公开证据和禁止扩写项。
- 创建 NotifyFlow 项目说明和一键启动目标。

退出标准：

- 选定 Java 为后端主语言。
- 写出前三个知识缺口和前三个项目缺口。
- 所有“待包装”内容区分实习事实与独立项目。

## 6. 第 1 周：集合、JMM 与线程池

章节：01、02、03。

核心产出：

- HashMap 扩容、冲突与并发风险图。
- happens-before 与 volatile/synchronized 对照。
- 有界线程池、拒绝策略、队列和 backpressure 实验。
- NotifyFlow 异步任务执行器设计。
- 15 分钟“线程池不是越大越好”Teach-back。

周测：闭卷解释 JMM、HashMap、线程池三个故障案例，80 分以上进入下一周。

## 7. 第 2 周：MySQL 与 Spring 事务

章节：04、05。

核心产出：

- 任务表、attempt、Outbox、幂等记录 DDL。
- 索引、锁、隔离级别、死锁与 SKIP LOCKED 报告。
- Spring AOP 代理、自调用、rollback、propagation 机制图。
- “事务内不调用 Provider”项目时序图。

环境被 Maven 阻塞时：完成测试设计和数据库层实验，但 Spring 章节保持 Pending。

## 8. 第 3 周：Redis 缓存、限流与短期幂等

章节：06。

核心产出：

- 缓存一致性、击穿、穿透、雪崩案例。
- Lua/原子操作限流设计。
- 短期幂等、fencing 和锁的边界。
- 大 key、hot key、eviction 和 Sentinel 故障矩阵。
- NotifyFlow 租户/渠道/Provider 分层限流表。

周测：给出“Redis 锁失效后旧 owner 恢复”的反例，不能只说 set NX。

## 9. 第 4 周：Kafka、Outbox 与可靠消息

章节：07。

核心产出：

- partition、offset、consumer group、rebalance 和 ISR 图。
- Transactional Outbox 发布/重复/幂等时间线。
- poison message、retry、DLT、Schema 和 lag 设计。
- Kafka 与 RocketMQ 的业务选型对照。

Docker 未启动时只标静态验证，不填写消息实验通过。

## 10. 第 5 周：恢复控制面

章节：08。

核心产出：

- retry amplification 和 jitter 结果复现。
- 错误分类表：retryable、permanent、unknown、systemic。
- UNKNOWN 对账、回调乱序、安全 replay 状态机。
- Preview、审批、RBAC、审计和分阶段恢复设计。
- Agent 在恢复中的只读边界。

周测：解释 timeout 为什么不等于 failed，并设计无重复副作用的恢复。

## 11. 第 6 周：可观测性、压测与 JFR

章节：09、10。

核心产出：

- NotifyFlow SLI/SLO、error budget 和指标字典。
- 低基数 metric、日志和 Trace 关联设计。
- 开放/封闭负载、coordinated omission 和 threshold。
- ThreadPoolExecutor/JFR 自定义事件复现。
- JVM heap/native/thread/lock/OOM Runbook。

至少完成一次 15 分钟事故复盘：症状、证据、假设、反例、修复、残余风险。

## 12. 第 7 周：网络、Docker 与 Kubernetes 入门

章节：11、12；第 19 章留给完整进阶版。

核心产出：

- 非 root、多阶段 Java 镜像草案。
- Compose 服务依赖和健康检查。
- Deployment、Service、探针、资源、滚动回滚设计。
- DNS、连接复用、分阶段 timeout、连接池和 UNKNOWN 图。
- 一个连接池饱和或慢依赖实验设计。

周测：区分 acquire/connect/read/overall deadline，并解释连接池为何不是越大越好。

## 13. 第 8 周：LLM 与最小 RAG 证据链

章节：14；15-17 只完成最小纵切选读。

核心产出：

- Tokenizer -> Transformer -> logits -> sampling 流程图。
- prefill/decode/KV cache 预算表。
- Chunk、metadata、Embedding、ANN、hybrid、rerank 设计。
- 包含 hard negative、无答案、权限和过期文档的评测集草案。
- Recall@k、MRR、nDCG、citation、faithfulness、拒答、成本指标。
- 用固定小语料完成 ingestion -> retrieval -> citation -> refusal 的实验合同；未运行则保持 Pending。

周测：解释 Recall@k 高但答案仍错的至少四种原因。

## 14. 第 9 周：Tool、Memory 与 Agent Runtime

章节：18；第 19 章多机分布式为完整进阶内容。

核心产出：

- Agent 有限状态机和预算。
- 只读/低风险/高风险 Tool schema。
- 幂等、UNKNOWN、人工审批、审计和 kill switch。
- 会话、working memory、长期知识和业务状态分层。
- SSE、取消、Last-Event-ID 和崩溃恢复设计。
- Prompt injection、跨租户、Secret、恶意工具结果攻击集。

周测：模型超时和工具超时分别如何恢复；Agent 为什么不能直接重放。

## 15. 第 10 周：求职发布

目标：把技术成果转成可追问、可验证的求职作品。

章节：13、20、21、22、23 的核心任务，不要求一周完成这些章节的完整建议学时。

任务：

- 一页 Java/Agent 后端简历。
- NotifyFlow 五分钟和十五分钟项目介绍。
- 三张图：架构、可靠消息、Agent Runtime。
- 三份证据：数据库/MQ、可观测性/JFR、RAG/Agent 安全。
- 保持每天 30-45 分钟算法训练，记录题目、首次结果、错误类型和复测日期；不得把第 22 章的 96 题规划写成已完成。
- 三次模拟面试：Java、项目、系统设计。
- 复盘所有“已验证/Pending”状态，删除无法追溯的数字。

退出标准：任何简历项目句子都能定位到代码、文档、实验或事实台账。

## 16. 每周复盘模板

```text
本周目标：
完成证据：
真实运行结果：
Pending 与原因：
最重要的三个机制：
最危险的三个误区：
Teach-back 听众问题：
项目修订：
面试失分点：
下周最小动作：
```

## 17. 每日 journal 模板

```text
今天要解决的问题：
先验答案：
查阅来源：
实验假设/断言：
命令和环境：
原始结果：
结论与边界：
我能否教会别人：
下一步：
```

## 18. Teach-back 评分

| 维度 | 分值 |
|---|---:|
| 为什么重要 | 10 |
| 机制准确 | 25 |
| 场景与反例 | 20 |
| 实验证据 | 20 |
| 边界和诚实 | 15 |
| 表达与问答 | 10 |

低于 80 分需要重讲；没有实验证据时必须明确讲“设计/Pending”。

## 19. 面试复习方法

每个主题准备四层答案：

1. 30 秒定义。
2. 2 分钟机制。
3. 5 分钟项目应用。
4. 故障追问、证据和边界。

面试错题在 1 天、3 天、7 天、14 天复习；若只能背标准答案，重新做实验或 Teach-back。

## 20. AI 协作规则

AI 可以：出题、追问、审阅设计、生成反例、解释日志、整理证据。

学习者必须亲自：先作答、运行实验、判断结果、做最终项目选择、完成 Teach-back。

禁止：复制 AI 结论冒充实验结果、让 AI 编造实习技术或生成无法解释的简历数字。

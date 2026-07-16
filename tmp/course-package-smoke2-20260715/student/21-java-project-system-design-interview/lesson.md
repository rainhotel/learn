# 第 21 章讲义：把知识、项目和证据组织成可追问的回答

## 学习目标

完成本章后，学习者能够：

1. 用统一协议完成 30、90、180 秒闭卷回答。
2. 把一道题展开为机制、实现、故障、权衡和证据追问树。
3. 将 Java、Spring、中间件、网络、容器与 NotifyFlow 真实场景连接。
4. 在项目答辩中区分真实实现、确定性实验、静态设计与后续计划。
5. 为 RAG/Agent 讨论检索质量、Tool 权限、可靠执行和评测。
6. 用可重复评分和延迟复训纠正“以为自己会”的错觉。

## 一、面试真正观察什么

高质量技术面试不只检查术语。面试官通常同时观察：

- correctness：核心结论是否正确。
- mechanism：能否解释状态如何变化，而不只是背定义。
- transfer：能否把原理用于新场景。
- diagnosis：出现慢、错、重复、丢失、泄漏时如何定位。
- trade-off：知道方案的代价和不适用条件。
- evidence：项目数字和贡献是否可以复核。
- communication：能否先给主线，再根据追问展开。
- integrity：是否诚实区分做过、设计过和尚未验证。

因此，答案长不等于答案好。一个两分钟答案若没有机制、失败路径和边界，只是更长的关键词列表。

## 二、统一回答协议：结论、机制、场景、失败、证据、边界

本课程使用 C-M-S-F-E-B 六段协议：

1. Conclusion：先用一句话回答问题。
2. Mechanism：说明关键状态、顺序或 happens-before。
3. Scenario：连接到 NotifyFlow 或具体业务。
4. Failure：说明最危险的错误用法和失败表现。
5. Evidence：指出代码、实验、SQL、日志、指标或官方规范。
6. Boundary：说明结论适用范围、替代方案和当前未验证项。

### 30 秒版本

只保留结论、一个机制点和一个边界。

示例：volatile 能保证什么？

> volatile 写与之后读同一变量建立 happens-before，因此适合发布状态或可见性标志；它不把 value++ 变成原子操作。复合不变量仍需要锁、原子类或单线程所有权。

### 90 秒版本

加入场景、失败和证据。

> volatile 解决可见性和特定有序性。写线程在 volatile 写之前的普通写，对随后读到该值的线程可见。NotifyFlow 的停止标志可以使用它，但并发领取计数不能直接用 volatile int 自增，因为读、计算、写是多个动作。第 03 章用固定交错展示了 volatile 自增丢失，并用 synchronized 保护复合递增。实验依赖 JDK 21 和受控调度，不外推为所有性能结论。

### 180 秒版本

加入替代方案、内存语义细节和一次反问澄清。超过三分钟仍未给出主线，通常说明答案结构失控。

## 三、不要背答案，要建知识图

每个主题至少建立六个节点：

~~~text
定义
  -> 工作机制
      -> 代码与配置
          -> 失败模式
              -> 可观测证据
                  -> 替代方案与边界
~~~

例如线程池：

~~~text
为什么使用线程池
  -> core/max/queue 如何接收任务
      -> 为什么 maximumPoolSize 可能长期不生效
          -> 无界队列怎样隐藏过载
              -> queue/rejected/P99/throughput 看什么
                  -> CallerRuns、Abort、限流或虚拟线程如何选择
~~~

若只能回答第一个节点，掌握程度仍停留在术语识别。

## 四、追问树：预测方向，不预测原题

一道根问题通常沿八条边展开：

1. 定义：它是什么？
2. 机制：内部怎样工作？
3. 实现：在 Java 或框架中怎样落地？
4. 边界：什么情况下不成立？
5. 故障：错用后如何表现？
6. 替代：为什么不用另一个方案？
7. 项目：你在哪里使用，个人做了什么？
8. 证据：如何证明，而不是“感觉有效”？

### 追问树构造方法

- 根节点必须是一句可回答的问题。
- 每层只引入一个新变量，避免一次追问五个方向。
- 至少有一条失败路径和一条证据路径。
- 叶节点不能只写名词，要写判断条件。
- 对尚未运行的内容，叶节点写验证计划，不预填结果。

### 面试中如何使用

不要把整棵树一次性倒给面试官。先回答根节点，停下来让对方选择方向。追问树是脑内索引，不是朗诵稿。

## 五、证据引用协议

### 5.1 证据编号

建议使用：

~~~text
E<章节>-<类型>-<序号>
~~~

类型示例：

- CODE：源码或测试。
- RUN：真实运行输出。
- SQL：DDL、执行计划和数据正确性查询。
- JFR：JFR 记录及分析。
- LOAD：负载模型或压测。
- DESIGN：架构、ADR 或静态方案。
- REVIEW：模拟面试、试讲或陌生评审。

### 5.2 最小证据卡

每张证据卡记录：

| 字段 | 内容 |
|---|---|
| claim | 这份证据支持什么，不支持什么 |
| artifact | 文件路径或可访问记录 |
| environment | JDK、组件版本、机器和配置 |
| command | 如何复现 |
| observation | 原始观察，不先写因果 |
| conclusion | 在什么边界内成立 |
| limitation | 仍缺什么证据 |

### 5.3 说数字的规则

一个数字只有同时回答以下问题，才适合在面试中使用：

1. 哪个版本？
2. 什么机器与配置？
3. 什么输入和负载模型？
4. 指标怎样统计？
5. 原始输出在哪里？
6. 能证明什么，不能证明什么？

例如“吞吐提升 50%”若没有 baseline、变量控制和原始输出，应删除。可以改成“设计了开放负载和 threshold，真实运行仍 Pending”。

### 5.4 证据强度与措辞

| 证据 | 安全措辞 |
|---|---|
| 只读过资料 | 理解、梳理、对比 |
| 写了设计 | 设计、定义、制定验证计划 |
| 静态检查 | 实现草案通过编译或静态检查 |
| 确定性模型 | 在固定模型中观察到 |
| 本地组件运行 | 在注明版本和环境中验证 |
| 故障演练 | 注入指定故障并验证恢复与正确性 |
| 生产经历 | 只按真实工作事实和授权范围描述 |

## 六、Java、JMM 与并发：从语义到过载

### 6.1 集合与对象合同

面试 HashMap 时，不要只背数组、链表、红黑树。主线应是：

- key 的 hashCode 与 equals 合同决定逻辑相等。
- 容量、负载因子和冲突影响查找与扩容。
- 可变 key 会让条目逻辑上“失联”。
- HashMap 不提供并发安全；ConcurrentHashMap 也不自动保护跨多个操作的不变量。

项目连接：NotifyFlow 的本地模板缓存或测试数据结构可以使用 Map，但任务真相不能保存在单实例内存 Map。

### 6.2 happens-before

happens-before 是可见性和合法执行推理规则，不等于墙上时钟先后。重点掌握：

- 程序次序规则。
- monitor unlock 到后续 lock。
- volatile 写到后续读。
- 线程 start 与 join。
- 传递性。

若两个冲突访问之间没有合适同步，不能用“CPU 最终会刷新”解释正确性。

### 6.3 volatile、synchronized、Lock 与原子类

- volatile：状态发布、标志、独立读写；不保护复合不变量。
- synchronized：互斥、可见性、可重入、异常释放；语义简单。
- ReentrantLock：可中断获取、超时、公平选项和多个 Condition；必须 finally 解锁。
- Atomic 类：适合单变量 CAS 状态更新；复杂跨字段不变量仍需更高层协议。

选择顺序应从不变量开始，而不是从“哪个更快”开始。

### 6.4 CAS 与 ABA

CAS 比较观察值并条件更新。失败线程通常重试，因此高竞争下会浪费 CPU；涉及外部副作用时，不能把 CAS 循环当成副作用重试。ABA 可通过版本戳、不可复用标识或重新设计状态转换处理。

### 6.5 线程池

ThreadPoolExecutor 的关键不是七个参数名字，而是任务接收过程：

~~~text
worker < core
-> 创建核心 worker
否则尝试入队
-> 队列满且 worker < max 时扩容
-> 仍不能接收则拒绝
~~~

常见追问：

- 为什么用了无界队列后 maximumPoolSize 看似无效？
- CallerRuns 如何把压力反馈给提交者，又会阻塞什么线程？
- shutdown 与 shutdownNow 的合同是什么？
- 中断为何是协作式取消？
- 怎样用 active、queue、rejected、completion rate 和 P99 判断饱和？

### 6.6 CompletableFuture 与结构化取消

需要说明：

- 默认 common pool 是否适合阻塞 I/O。
- thenApply 与 thenCompose 的差别。
- 异常如何在链路中传播和恢复。
- 超时是否真正取消底层任务。
- 多个异步层各自重试为什么会放大。

Java 21 虚拟线程降低了大量阻塞任务的线程成本，但不会增加数据库连接、Provider 配额或 CPU。即使每请求一个虚拟线程，仍需要 deadline、连接池、限流和背压。

## 七、JVM：从症状到证据

### 7.1 运行时数据与对象生命周期

回答内存区域时要连接故障：

- heap：对象与 GC，可能出现 Java heap space。
- thread stack：栈帧与递归，可能 StackOverflowError；大量线程也消耗本地内存。
- metaspace：类元数据，动态类加载或类加载器泄漏可能增长。
- direct/native memory：NIO、压缩库、JVM 结构和本地分配，不一定体现在 heap。
- code cache：JIT 编译代码。

“容器 OOMKilled”不能直接等同于 Java heap OOM。

### 7.2 GC 回答框架

先问：目标是吞吐、停顿、占用还是成本？再讨论：

- 分配速率和对象存活分布。
- young/old collection 与晋升。
- collector 的并发、并行和停顿阶段。
- heap、容器内存和 headroom。
- GC log、JFR 和应用 P99 的时间关联。

不要只凭一次 Full GC 就得出“内存泄漏”；也不要仅凭平均停顿判断用户体验。

### 7.3 排障顺序

~~~text
确认用户症状与时间窗
-> 保存变更和流量背景
-> 判断 CPU、内存、锁、I/O 或依赖方向
-> 用线程 dump、JFR、GC log、heap histogram 等收集证据
-> 建立多个假设
-> 用时间线和对照排除
-> 止血、恢复、验证数据正确性
~~~

命令名称不是答案。必须解释为什么在这个症状下使用，以及采集动作本身的风险。

## 八、Spring：代理边界就是事务边界

### 8.1 事务代理

Spring 声明式事务通常由代理拦截外部调用。高频失败包括：

- 同类自调用绕过代理。
- 方法不可代理或 Bean 不由容器管理。
- 异常被吞掉，代理看不到回滚条件。
- 默认只对特定异常回滚，与业务预期不一致。
- 异步线程、消息监听或远程调用跨出本地事务。
- 长事务把网络调用包在数据库锁内。

### 8.2 传播与回滚

不要背传播枚举列表。先画调用链和资源边界，说明每层是否：

- 加入当前事务。
- 新建独立事务。
- 允许无事务执行。
- 对内层回滚如何传递。

Transactional Outbox 解决数据库事实与待发布事件同事务提交，不使数据库、Kafka 和 Provider 形成全局事务。

### 8.3 Web 请求链路

能描述 Gateway/Filter、DispatcherServlet、Interceptor、Controller、Service、Repository、异常映射和 Trace context 的责任。认证、租户绑定和幂等校验应在进入业务副作用前完成。

## 九、MySQL、Redis 与 Kafka：回答数据真相和失败语义

### 9.1 MySQL

高频主线：

- B+Tree 索引降低需要访问的记录范围，但是否使用取决于查询、统计和成本。
- 联合索引服务于具体过滤、排序和覆盖需求，不是“字段越多越好”。
- MVCC 解释一致性读版本；当前读、锁和隔离级别另行讨论。
- 死锁是并发事务锁依赖环，数据库选择 victim；应用需要重试整个事务并保持幂等。
- SKIP LOCKED 适合多 worker 跳过已锁任务，但可能牺牲严格公平，仍需状态条件和幂等。

### 9.2 Redis

先按用途分类：

- cache：可重建，故障时保护数据库。
- rate limit：需要明确 fail-open/fail-closed。
- short-lived idempotency accelerator：最终约束仍在数据库。
- distributed coordination：必须讨论租约、owner、续约、fencing 和故障模型。

缓存一致性回答需要给出 source of truth、失效策略、并发窗口和业务可接受陈旧度。

### 9.3 Kafka

必须限制承诺范围：

- Producer、broker、consumer 各有失败点。
- consumer group 提供分区级并行，rebalance 期间需要正确处理 in-flight。
- key 只保证分区内顺序。
- offset 提交与外部 Provider 副作用不是原子操作。
- Kafka EOS 不能外推成外部副作用 exactly-once。
- DLT 是隔离和处理入口，不是问题自动消失。

NotifyFlow 的端到端策略是至少一次传递、业务幂等、UNKNOWN、对账和受控重放组合。

## 十、网络、连接池与容器

### 10.1 超时分层

需要区分：

- DNS deadline。
- pool acquire timeout。
- connect timeout。
- TLS handshake timeout。
- write timeout。
- response/read timeout。
- overall request deadline。

只配置 read timeout 可能在取连接或 DNS 阶段已经耗尽预算。客户端超时还可能意味着结果 UNKNOWN，而不是服务端未执行。

### 10.2 连接池

连接池容量由下游安全并发、服务时间和 deadline 约束。池过大可能把数据库或 Provider 压垮；池过小产生 acquire queue。面试时同时谈 active、idle、pending、timeout、连接寿命和泄漏。

### 10.3 Docker

需要解释：

- image 是只读层和元数据，container 是带可写层的运行实例。
- namespace 提供视图隔离，cgroup 提供资源记账与限制。
- 容器共享宿主机内核，不是轻量虚拟机的简单同义词。
- PID 1、信号、只读文件系统、非 root、Secret 和镜像供应链影响生产行为。

### 10.4 Kubernetes

至少掌握：

- Deployment、Pod、Service、ConfigMap、Secret 的责任。
- readiness 决定是否接流量；liveness 用于处理无法自愈的卡死，不能因下游短暂失败制造重启风暴。
- terminationGracePeriod、preStop、停止领取和有界等待构成优雅下线。
- requests/limits、HPA 与应用并发/外部配额必须一致。

## 十一、NotifyFlow 项目答辩

### 11.1 三层事实

任何项目答案先确定属于哪一层：

1. 已实现并有真实运行证据。
2. 已完成静态设计、代码或确定性模型，但基础设施实验未运行。
3. 下一阶段演进计划。

三层可以同时出现，但不能混写成同一完成度。

### 11.2 项目介绍顺序

~~~text
业务问题与用户
-> 规模和可靠性约束
-> 自己负责的边界
-> 主链路和数据真相
-> 最难的失败语义
-> 关键取舍
-> 可复核证据
-> 当前限制与下一步
~~~

不要从“用了 Spring Boot、MySQL、Redis、Kafka”开始。组件不是项目价值。

### 11.3 高频深挖

- 为什么不能直接写库后发 Kafka？
- timeout 为什么进入 UNKNOWN？
- 幂等键的作用域、保留期和请求摘要是什么？
- 重试、DLT、对账和 replay 分别解决什么？
- 数据库、Redis、Kafka 谁是真相源？
- 多实例如何领取、接管并阻止旧 owner 写？
- 怎样证明没有重复副作用和数据不一致？
- 线程池、连接池和 Provider 配额怎样共同限流？
- Agent 为什么默认只读？
- 你个人完成了哪些代码、实验和文档？

## 十二、RAG 与 Agent 面试

### 12.1 RAG 链路

~~~text
ingestion
-> parse/OCR/clean/chunk/version/ACL
-> sparse + dense retrieval
-> filter/fusion/rerank
-> context selection
-> generation
-> claim-citation validation
-> evaluation and audit
~~~

回答“如何优化 RAG”时，必须先定位失败阶段。盲目换更大模型可能无法修复权限、切分、召回或过期文档问题。

### 12.2 Agent 与工作流

- workflow：路径和状态转换主要由代码决定。
- tool-using agent：模型在限定策略内选择下一步。
- multi-agent：引入角色拆分，同时增加通信、状态和错误放大成本。

模型不能成为业务状态唯一存储。Tool 需要结构化 schema、权限、超时、幂等、副作用等级和审计。

### 12.3 Agent 可靠性

需要讨论：

- 模型/Tool timeout 和 overall budget。
- UNKNOWN 与重复工具调用。
- 中断、取消和崩溃恢复。
- 人工审批与确定性执行器。
- prompt injection、跨租户和敏感数据。
- 完成率、引用、安全、延迟、token 与成本评测。

## 十三、系统设计面试

### 13.1 固定推进顺序

1. 澄清用户、功能、规模、SLO、重复/顺序、保留、合规和成本。
2. 写明假设与非目标。
3. 估算平均/峰值 QPS、扇出、并发、积压和存储。
4. 定义 API、幂等合同、状态机和数据真相。
5. 画正常写入、异步处理、回调与恢复时序。
6. 讨论分区、热点、背压、多租户和多实例。
7. 注入数据库、缓存、消息、Provider 和网络故障。
8. 定义 SLI、告警、日志、Trace 和数据正确性查询。
9. 讨论权限、隐私、审计、成本和阶段演进。
10. 给出验证计划，区分设计目标与实测事实。

### 13.2 容量公式

~~~text
average_qps = daily_requests / 86400
peak_qps = average_qps * peak_factor
delivery_qps = request_qps * average_fanout
in_flight = throughput * average_service_time
drain_time = backlog / (safe_capacity - incoming_rate)
storage = records_per_day * bytes_per_record * retention_days
~~~

公式只是初估。流量分布、P99、重试、索引、复制、Provider 配额和安全水位会改变设计。

### 13.3 面对陌生题

如果不知道某个组件内部细节：

- 先承认边界。
- 回到输入、状态、输出和故障模型。
- 给出两种可能设计及验证方式。
- 不编造配置项、复杂度或生产数字。

推理清晰且诚实，比错误地表现“什么都知道”更可靠。

## 十四、模拟面试评分

### 14.1 单题 10 分

| 维度 | 分值 |
|---|---:|
| 结论正确且先回答问题 | 2 |
| 机制和状态变化清楚 | 2 |
| 能连接具体场景 | 1 |
| 能处理失败与反例 | 2 |
| 有证据或可执行验证 | 2 |
| 边界与表达控制 | 1 |

事实性硬错、伪造数字、把设计冒充运行事实时，该题最高 4 分。

### 14.2 综合 100 分

| 维度 | 分值 |
|---|---:|
| Java/JMM/并发/JVM | 20 |
| Spring 与数据中间件 | 20 |
| 网络、容器与生产排障 | 10 |
| NotifyFlow 项目答辩 | 20 |
| RAG/Agent | 10 |
| 系统设计 | 15 |
| 证据诚实性与沟通 | 5 |

硬门禁：

- 跨租户、安全或数据正确性原则出现严重误判。
- 把 Kafka EOS 说成外部 Provider exactly-once。
- 把 timeout 直接等同于失败并建议盲目重复副作用。
- 把未运行实验或课程设计描述成实习/生产事实。

出现硬门禁时，本轮不得评为通过。

## 十五、纠错循环

### 15.1 错误标签

| 标签 | 含义 | 修正动作 |
|---|---|---|
| K | 不知道概念或结论 | 回到一手资料和最小实验 |
| M | 会结论，不会机制 | 画状态/时序并闭卷复述 |
| T | 不能迁移到场景 | 写 NotifyFlow 应用与反例 |
| F | 忽略失败和恢复 | 补故障时间线与正确性查询 |
| E | 没有证据或数字失真 | 建证据卡，降级措辞 |
| B | 忽略边界和替代方案 | 写适用条件与 ADR |
| C | 表达失控 | 重做 30/90/180 秒版本 |

### 15.2 五步闭环

1. 在计时条件下录音，禁止边说边查。
2. 转写后只标事实、机制、遗漏和无效铺垫。
3. 为每个错误选择一个根因标签，不用“紧张”掩盖知识缺口。
4. 不看原稿重新回答，并让追问继续一层。
5. 48 小时和 7 天后随机重答，确认长期保持。

### 15.3 纠错卡

~~~text
question:
first_answer_summary:
error_tag:
wrong_or_missing_claim:
correct_mechanism:
evidence_id:
failure_case:
boundary:
90_second_reanswer:
next_review_date:
~~~

纠错卡只保留一个核心错误。把十个问题塞进一张卡会失去可复训性。

## 十六、训练节奏

### 每日 60-90 分钟

- 10 分钟：随机抽题。
- 25 分钟：6 道 90 秒闭卷回答。
- 15 分钟：对两道题追加三层追问。
- 20 分钟：核验证据并制作纠错卡。
- 10 分钟：重答昨天的错误题。

### 每周

- 一次 45 分钟综合模拟。
- 一次 15 分钟 NotifyFlow 答辩。
- 一次系统设计白板练习。
- 一次错误分布复盘，减少重复犯错而不是追求题量。

## 十七、事实与合规边界

- 大烨实习只描述真实职责，不新增未做过的 MySQL、Redis、Kafka 或 RAG 工作。
- NotifyFlow 作为独立工程项目描述，运行结果附环境和证据。
- 社区面经可用于发现题型，不作为机制、岗位数量或录用标准证据。
- 不记录、传播受保密协议约束的面试题、公司内部信息或他人个人数据。
- 模拟面试录音必须取得参与者同意，并限制访问和保存期限。

## 本章小结

面试准备的最小闭环不是“看完题库”，而是：

~~~text
闭卷回答
-> 承受追问
-> 引用证据
-> 发现错误
-> 修正机制
-> 延迟重答
-> 更新项目和课程
~~~

只有当知识能被口述、被追问、被证据支持并教给别人时，它才真正转化为求职能力。


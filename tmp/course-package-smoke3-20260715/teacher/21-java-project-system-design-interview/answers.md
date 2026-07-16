# 第 21 章参考答案与评分锚点

## 使用说明

参考答案用于核对机制和评分，不是背诵稿。学习者的首答必须在阅读本文件前完成。只要结论、机制、证据和边界正确，表达不必与示例一致。

## 总评分规则

单题 10 分：

| 维度 | 分值 |
|---|---:|
| 结论 | 2 |
| 机制 | 2 |
| 场景 | 1 |
| 失败与反例 | 2 |
| 证据与验证 | 2 |
| 边界和表达 | 1 |

出现事实性硬错、虚构运行数字或把设计冒充实习/生产事实时，单题最高 4 分。

## A. 闭卷机制题

### 1. volatile 与 happens-before

高分要点：

- volatile 写 happens-before 后续读到该变量的线程，提供状态发布所需的可见性和有序性。
- value++ 是读、加、写三个逻辑动作，两个线程可读到同一旧值，因此 volatile 不提供复合原子性。
- 适用：停止标志、不可变对象引用发布、独立状态位。
- 不适用：跨字段余额不变量、领取计数、check-then-act。
- 可引用 E03-RUN-01，并说明受控调度实验不证明通用性能。

只回答“volatile 保证可见性，不保证原子性”但不会解释交错，最高 6 分。

### 2. 线程池饱和

高分要点：

- worker 少于 core 时建 worker；否则入队；队列满且 worker 少于 max 扩容；仍失败则拒绝。
- 无界队列通常持续接收任务，流程很难走到扩到 max，过载转成内存和排队风险。
- CallerRuns 让提交线程执行，形成反馈式降速；若提交线程是请求线程、事件循环或关键 consumer，会把延迟和阻塞向上游传播。
- 同时观察 active、queue、rejected、completed throughput、P99、任务 age 和下游配额。
- E01-RUN-01 支持接收顺序和 CallerRuns；E09-LOAD-01 支持容量拐点模型。

仅背七个参数，最高 5 分。

### 3. 中断、取消与虚拟线程

高分要点：

- interrupt 设置中断状态或让可中断阻塞抛 InterruptedException；任务代码必须检查、传播或恢复中断。
- cancel(true) 只是请求中断正在运行的任务；shutdownNow 尽力中断 worker 并返回未启动任务。
- 调用方超时返回不一定停止后台计算或网络请求。
- 虚拟线程降低阻塞线程成本，不取消 Provider 配额、数据库连接和外部副作用。
- 外部副作用使用 deadline、稳定幂等键、UNKNOWN、查询/回调和对账。

把中断说成“强制杀死线程”，为硬错。

### 4. JVM 事故分类

可能假设：

- 业务热点或重试风暴导致 CPU 计算增加。
- 锁竞争/自旋或线程调度。
- GC 并非由高 heap 占用触发，也可能是高分配率导致频繁 young GC。
- 序列化、正则、压缩、加密或日志热点。
- 下游慢引起线程堆积，但 CPU 高需要进一步区分。

证据顺序：用户 SLI/变更/流量 -> 容器 CPU throttling 与线程数 -> JFR/线程 dump/热点方法 -> GC log 与分配 -> 依赖和重试指标。止血要按证据选择限流、关闭问题路径、回滚或降低重试。heap 60% 不能支持“加大堆”，容器 CPU 与代码热点也不会因增 heap 自动消失。

### 5. Spring 事务失效

典型调用：

~~~text
external -> OrderService.outer()
outer() -> this.createNotification()
createNotification() 标注 @Transactional
~~~

外部调用进入代理，但 this 调用没有再次穿过代理，内层事务拦截器不执行。

可选修复：

- 把事务方法拆到另一个 Bean：边界清楚，但增加组件。
- 让外部入口方法承担事务：简单，但事务范围可能过大。
- 使用 TransactionTemplate：显式可控，但侵入业务代码。

还应检查异常回滚、方法可见性、Bean 管理和异步边界。仅回答“加注解”不得分。

### 6. MySQL 任务领取

高分方案：

- 索引以查询为中心，例如 state、next_attempt_at、id，并按租户/分片需求调整。
- 短事务 select ... for update skip locked limit N，立即更新 owner/lease/state 后提交。
- Provider 网络调用不放在领取事务中。
- 崩溃后由 lease 到期接管；旧 owner 的最终写使用版本或 fencing 拒绝。
- 死锁重试整个本地事务，使用稳定幂等键避免重复副作用。
- E04-SQL-01 支持 SKIP LOCKED 和死锁现象，不证明完整多实例 fencing。

### 7. Redis 故障

- 模板缓存：降级到数据库或本地已知版本，使用限流和请求合并防止击穿。
- 限流：按业务风险定义 fail-open/fail-closed；高成本或高风险渠道倾向保守，本地/网关可有小配额兜底。
- 短期幂等加速：允许失效，但 MySQL 唯一约束仍保护最终幂等。
- 任务状态、审批、审计、Outbox 和最终配额合同不能只在 Redis。

回答必须区分用途；一句“Redis 挂了走数据库”最高 5 分。

### 8. Kafka、Outbox 与 exactly-once

- MySQL commit 成功而 send 失败会漏消息；send 成功而事务回滚会出现幽灵事件。
- Outbox 把业务事实与待发事件放在同一个本地事务。
- Publisher 在崩溃窗口会重复发布，consumer 仍需幂等。
- offset 与 MySQL/Provider 不原子；Provider 接收后 consumer 崩溃可能重复调用，响应丢失则 UNKNOWN。
- 安全承诺：指定边界内至少一次传递，加业务幂等、状态机、对账和审计；不能承诺所有外部副作用 exactly-once。

D07-DESIGN-01 为设计证据，Kafka runtime Pending。

### 9. 网络超时

- connect timeout 限制建立连接；read timeout 限制等待读取；overall deadline 覆盖整个调用预算，理想情况下还包含 DNS、pool acquire 和 TLS。
- response 丢失时 Provider 可能已经执行，本地只能知道结果未知。
- 重用稳定 provider request id；本地写 UNKNOWN；禁止普通盲重试；通过 Provider 查询、回调或对账收敛。
- 若 Provider 无幂等/查询能力，需暴露重复风险并提高人工确认级别。

把 timeout 直接落 FAILED 并立即换新 key 重试，为硬错。

### 10. Docker/Kubernetes 下线

推荐顺序：

1. readiness 失败，从 Service endpoint 移除。
2. 应用停止领取新任务或 pause consumer。
3. 有界等待 in-flight 到 deadline。
4. 将未完成任务留在可接管状态，安全提交 offset/状态。
5. 关闭连接池、线程池和客户端后退出。

grace period 要大于应用停止领取与有界收尾预算，但不能无限。liveness 若把所有下游失败当成本进程死亡，会在共同依赖故障时制造重启风暴。D12 当前是设计 Pending。

### 11. RAG 质量定位

合格树：

~~~text
语料是否正确解析、版本和 ACL 是否正确
-> gold evidence 是否被 sparse/dense 召回
-> fusion/rerank 是否压低相关证据
-> context 是否截断、冲突或越权
-> 模型是否忠实、引用是否支持 claim
-> 是否本应拒答
-> 离线 case 与线上 trace/ranking 是否一致
~~~

应保存 corpus/config hash、完整 ranking、context、response、citation 和身份范围。直接回答“换更大模型”，最高 3 分。

### 12. Agent 工具安全

只读 Tool 输入至少包含 identity context、tenant、channel、from/to、允许的指标集合和结果上限；服务端重新授权，不能信任模型提供的 tenant。输出使用结构化聚合，不返回 Secret 或无限日志。设置 Tool timeout、overall budget、审计 correlation id 和脱敏。

检索到的日志/Runbook 是不可信数据，不能改变系统指令或权限。Agent 只提出 replay preview 建议；审批和执行由独立确定性控制面完成。

## B. 追问树作业

### 13. 并发追问树

至少应出现：

~~~text
保护什么不变量
-> synchronized 的 monitor/HB/异常释放
-> Lock 的 interruptible/tryLock/Condition/fairness
-> 谁负责 finally unlock
-> 竞争、饥饿、死锁如何观测
-> 为什么不先比较微基准性能
-> NotifyFlow 哪个临界区需要它
-> 是否可用单线程所有权或无锁状态机替代
~~~

十二个节点齐全 6 分；失败、替代和证据各 1 分；结构清晰 1 分。

### 14. 数据一致性追问树

必须覆盖：

- tenant 作用域 API 幂等与 request hash。
- task/outbox 同事务。
- publisher 重复与 consumer 幂等。
- Provider 稳定 request id。
- timeout -> UNKNOWN。
- callback 唯一键与单调状态机。
- reconciliation 与审批 replay。
- 不支持幂等的 Provider 风险。

若根结论是“Kafka exactly-once 保证不重复”，不得通过。

### 15. RAG/Agent 追问树

高分树先比较确定性 workflow 与模型决策自由度，再进入 Tool schema、Memory 分层、持久状态、timeout/UNKNOWN、权限/审批、prompt injection、评测和成本。多 Agent 必须作为有代价的可选项，而非默认升级。

### 16. 证据追问树

必须能回答：

- 环境和固定版本。
- 开放还是封闭负载。
- 服务时间和容量假设。
- queue/rejected/P99/throughput 的原始输出。
- 是否存在 coordinated omission。
- 是否只验证模型而非真实组件。
- 如何复现和改变一个变量。
- 结论不能外推到哪些场景。

## C. NotifyFlow 项目答辩

### 17. 三档项目介绍

评分：

| 项目 | 分值 |
|---|---:|
| 三个版本主线一致 | 2 |
| 先业务与约束，后组件 | 2 |
| 个人贡献具体 | 2 |
| 有证据且能打开 | 2 |
| Pending 与限制明确 | 2 |

三个版本出现相互冲突的数字或完成度时，需返回事实台账修订。

### 18. 事实与证据审计

高风险动词包括“负责、主导、落地、优化、解决、保证、上线、支撑”。每个动词需回答主语、范围、动作、产物和证据。

数字若只有设计输入，安全措辞是“按某假设估算”；确定性模型写“在固定模型中观察”；真实组件运行写明版本和环境。没有证据的量化提升删除。

### 19. Provider UNKNOWN 事故

错误实现示例：timeout 后将原 attempt 标 FAILED，生成新 provider request id 立即重试。

正确性验证思路：

- 按 tenant + logical delivery 聚合 Provider request id 和 provider message id。
- 比较本地 attempt、回调和 Provider 查询结果。
- 查找同一逻辑 delivery 的多个成功副作用。
- 确认 UNKNOWN 收敛过程没有绕过状态机。

真实 Provider 故障演练仍 Pending，答案应明确。

### 20. 性能数字追问

合格回答应：

1. 直接说明目前没有端到端 QPS 和提升比例。
2. 展示已有线程池、负载模型和 JFR 证据。
3. 解释这些证据支持与不支持的结论。
4. 给出真实压测计划：固定版本、开放负载、threshold、服务端指标、故障注入和数据正确性。
5. 不因没有数字而虚构结果。

诚实不是一句“没测过”，还要展示测量能力。

### 21. 十倍流量

必须重新计算峰值投递 QPS 和 in-flight，至少检查 API/DB 写、Outbox、Kafka 分区、Worker/连接池、Provider 配额。恢复预算要将 normal、retry、reconciliation 和 replay 隔离。若 safe capacity 不大于 incoming rate，积压不会被清空。

### 22. 删除组件

- 删除 Redis：以 DB/本地小缓存承担，吞吐或保护能力下降，但系统真相不应丢失；低规模可更简单。
- 删除 Kafka：可用 DB task/outbox 扫描器直接驱动 Worker，减少运维复杂度但削弱事件解耦和大规模消费能力。
- 删除 Agent：保留 Dashboard、查询、Runbook 和人工诊断；可靠通知主链路不应依赖模型。

高分答案说明组件是为需求服务，而不是架构身份。

### 23. 先进制造迁移

必须加入设备/站点身份、边缘断线缓冲、事件时间与重复、告警优先级、网络分区、工单人工确认、控制命令高风险审批和 Agent 禁止直接控制设备。不能把练习描述成真实行业生产经验。

## D. 模拟面试与纠错

### 24. 40 题闭卷抽测

通过建议：

- 总分至少 320/400。
- 每个能力域至少 70%。
- 无硬门禁错误。
- 证据定位中位数不超过 60 秒。
- 48 小时复训不是靠背原句，而能应对变体。

这些是课程门槛设计，不是已取得成绩。

### 25. 45 分钟综合模拟

完整记录应包含：题目、首答、追问、时间、评分依据、错误标签、评审者原话和修订。只保存总分无法形成纠错证据。

### 26. 纠错卡

合格卡只处理一个根因，包含正确机制、失败案例、证据、边界和新的 90 秒回答。写“紧张、表达不好、下次注意”但不指出错误 claim，判为无效。

### 27. 延迟复训

比较的是机制迁移：

- 结论是否仍正确。
- 新场景下不变量是否识别。
- 是否选择新的证据或验证计划。
- 是否避免背诵原题措辞。

如果换名词就不会，说明知识仍是题目索引而非模型。

### 28. 反向追问

高质量问题示例：

- 团队当前 Agent 的职责边界是检索、编排还是允许副作用执行？
- 新人前三个月通常负责哪类可独立验收的交付？
- 服务发生依赖故障时，团队怎样做发布回滚和数据正确性验证？
- 当前最希望改善的可靠性或工程效率问题是什么？
- 后端、算法和平台同学如何共同维护 RAG/Agent 评测与线上反馈？

问题应体现岗位理解，不假设对方必须透露内部架构。

### 29. 模拟面试官训练

追问质量评分：

- 是否基于上一答案，而非机械换题。
- 是否逐次只改变一个变量。
- 是否追到失败、证据和个人贡献。
- 是否避免暗示答案。
- 是否给出可执行反馈。

### 30. 合规审计

任一参与者未同意录音、材料含保密信息、权限不明或把模拟题标为真实面试题时，不得进入课程证据包。

## E. 发布作业

### 31. 面试能力档案

档案重点是能力边界和趋势，不是包装满分。至少显示：

- 能力域覆盖是否失衡。
- 哪些错误重复出现。
- 哪些 claim 已有强证据。
- 哪些能力仍只能描述为学习/设计。
- 下一轮训练如何调整。

### 32. 陌生读者验证

评审者必须不依赖作者口头补充，能找到证据并理解 Pending 边界。其困惑要进入课程修订记录。没有陌生评审，不得声称本章已达到可售卖发布质量。


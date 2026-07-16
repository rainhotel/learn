# 求职证据、简历与面试交付包

## 1. 使用边界

这份文件把课程成果转成求职材料。它不替代当前招聘网站的实时核验；每次投递前必须保存岗位链接、日期、城市、经验要求、技术关键词和岗位原文摘要。

实时 JD 研究状态和来源日志见 `../../../06-research/job-jd-agent-java-2026/README.md`。

不得把课程设计、模拟实验或 AI 生成内容写成大烨实习事实。

## 2. 四类目标岗位

| 岗位族 | 常见工作 | 重点课程证据 |
|---|---|---|
| Java 后端开发 | API、事务、缓存、MQ、并发、排障 | 01-10、19 |
| Agent/AI 应用后端 | Model Gateway、RAG、Tool、SSE、评测、安全 | 09、14-18 |
| 平台/基础设施后端 | 容器、K8s、可观测性、发布、容量、分布式 | 09、10、12、19 |
| 先进制造数字化 | 设备/工单/质量/供应链数据、集成、权限、可靠任务 | 04-09、12、19 |

同一项技术在不同岗位的表达不同：大厂强调规模、延迟、稳定性和复杂协作；制造业强调数据链路、设备/业务集成、可追溯、权限和长期运维。

## 3. JD 采样表

每周从 Boss、企业招聘官网和公开岗位页采样 5-10 条，填写：

```text
company:
job_title:
url:
captured_at:
city/remote:
experience:
language:
backend_framework:
data/mq/cache:
cloud/container:
AI/Agent/RAG:
testing/observability:
business_domain:
must_have_keywords:
nice_to_have_keywords:
interview_signals:
source_notes:
```

只统计多条 JD 重复出现的能力，避免被单条岗位的偶然词汇带偏。岗位变化时更新 `source-log`，不要覆盖历史样本。

## 4. 能力到证据矩阵

| JD 关键词 | 课程章节 | 最低证据 | 简历可写条件 |
|---|---|---|---|
| Java 并发/线程池 | 01-03、09 | JDK 实验 + 解释拒绝/可见性 | 能展示代码和输出 |
| MySQL/事务 | 04-05 | SQL/事务实验和数据正确性 | 明确版本与场景 |
| Redis/限流/幂等 | 06 | 竞态/缓存实验 | 说明一致性取舍 |
| Kafka/Outbox | 07-08 | offset/lag/重复/恢复证据 | 不写虚构消息量 |
| 监控/压测/JFR | 09-10 | Dashboard、k6/JFR 或明确 Pending | 量化结果可追溯 |
| Docker/K8s | 12 | 镜像/部署/探针/回滚运行证据 | 写清环境和限制 |
| RAG/Embedding | 14-17 | ingestion、检索、评测集、Recall/citation/失败样本 | 不能只写“接入向量库” |
| Agent/Tool | 18 | schema、RBAC、幂等、攻击/恢复 | 写安全边界和评测 |
| 分布式 | 19 | 多实例、lease/fencing、对账 | 说明故障模型 |

## 5. 事实台账

为每个简历句子保留：

```text
claim_id:
claim:
source_type: internship | independent_project | course_lab | design_only
source_file_or_link:
run_date:
environment:
raw_evidence:
what_I_did:
what_I_did_not_do:
safe_wording:
```

`design_only` 只能写“设计/规划/实现草案”；`course_lab` 要标出是课程实验；只有真实运行证据才可写具体结果。

## 6. 简历项目模板

### 6.1 事实可靠版本

```text
NotifyFlow 可靠通知与 Agent 事故助手（独立工程项目）
- 使用 Java 21 设计任务表、幂等、Transactional Outbox、Kafka 消费、重试/DLT/UNKNOWN 对账和安全恢复控制面。
- 设计低基数 Metrics、开放/封闭负载、JFR 事件证据和 Agent 只读事故分析边界。
- [仅在真实运行后填写] 在固定环境下完成 X 次请求/故障演练，P95/P99/lag/数据正确性结果见报告。
```

### 6.2 不能直接写

- “支撑 10 万 QPS”但没有真实压测报告。
- “将延迟降低 80%”但没有基线、样本和环境。
- “负责公司 Redis/Kafka/RAG”但实际只在课程中设计。
- 把大烨实习未做过的 MySQL、Redis、MQ、RAG 写成实习职责。

## 7. 项目答辩结构

使用 5-15-45 分钟三版：

1. 业务和约束：通知可靠性、Provider 不稳定、多租户和恢复。
2. 核心状态：task/attempt/outbox/lease/UNKNOWN。
3. 关键取舍：事务边界、消息语义、幂等、退避、限流。
4. 证据：实验、指标、日志、Trace、JFR、SQL、故障时间线。
5. 边界：哪些是模型、哪些本地运行、哪些 Pending。
6. 下一步：真实环境、容量、成本和安全改进。

## 8. 面试准备包

### Java 基础

- 30 道并发/集合/JMM 题。
- 20 道 MySQL/事务/Redis/MQ 题。
- 10 道 JVM/网络/容器题。

### 项目追问

- 为什么事务内不调用 Provider？
- Outbox 重复如何处理？
- timeout 为什么进入 UNKNOWN？
- 多实例旧 owner 如何被 fencing？
- Agent 为什么不能自动 replay？
- 你哪些数字是真实运行，哪些是设计或 Pending？

### 系统设计

每题必须写：需求、容量假设、数据模型、API、状态机、依赖、失败、可观测性、安全、扩展和取舍。

## 9. 投递节奏

每周：

- 采样 5-10 条 JD，更新关键词频次。
- 针对 2 个岗位改写简历摘要和项目证据。
- 做 1 次项目模拟面试和 1 次算法练习。
- 记录投递版本、反馈、追问和下一次修订。

不要同时维护十份完全不同的简历；维护一份事实主简历和三种岗位摘要：Java 后端、Agent 后端、制造业数字化。

## 10. 岗位匹配评分

每个 JD 100 分：

| 维度 | 分值 |
|---|---:|
| Java/后端基础匹配 | 25 |
| 数据/消息/缓存 | 20 |
| Agent/RAG/AI | 20 |
| Docker/K8s/分布式 | 15 |
| 业务域与实习事实 | 10 |
| 城市/经验/时间可行性 | 10 |

70 分以上优先投递；50-69 分先补证据；低于 50 分只作为学习目标，不作为主攻岗位。

## 11. 真实面试反馈归档

```text
date/company/job:
question:
my_answer:
interviewer_follow_up:
evidence_I_should_show:
wrong_or_weak_part:
chapter_to_review:
next_simulation_date:
```

反馈必须回写章节练习或面试题，不只留在聊天记录里。

## 12. 发布前求职门禁

- 简历每个量化数字都有证据链接或明确环境。
- 实习和独立项目边界清晰。
- 至少一份项目报告、一份故障时间线和一份 RAG/Agent 安全说明。
- 完成三次模拟面试和至少一次陌生听众 Teach-back。
- 能在 5 分钟讲清项目，在追问中承认未知和 Pending。
- 投递前重新核对岗位原文日期，不使用过期 JD 代替当前要求。

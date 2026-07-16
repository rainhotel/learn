# 第 20 章项目应用：NotifyFlow 求职证据包

## 一、项目定位

NotifyFlow 与 Agent 事故助手属于独立工程项目，不属于大烨实习。项目经历必须显式标注来源，并把“已运行”“静态检查”“设计中”分开。

## 二、证据地图

为项目创建以下映射：

| claim 主题 | 可引用章节 | 需要的证据 | 当前允许表达 |
|---|---|---|---|
| Java 并发与线程池 | 01、03、09 | 源码、JDK 输出、负载参数 | 在固定 JDK 环境完成实验并报告结果 |
| MySQL 任务表与事务 | 04、05 | DDL、SQL、事务用例、版本 | 已运行部分写结果；其余写设计或实验状态 |
| Redis 缓存/限流/幂等 | 06 | 配置、并发竞态、运行输出 | 未运行时只写设计，不写验证效果 |
| Kafka/Outbox/恢复 | 07、08 | 重复、offset、DLT、对账证据 | 静态设计与真实运行严格分开 |
| 可观测性/JFR | 09、10 | 指标、JFR、日志、环境 | 只引用真实执行过的实验数值 |
| Docker/Kubernetes | 12 | 镜像、Manifest、事件、探针、回滚 | 无集群运行时只写部署设计 |
| RAG 与向量检索 | 14-17 | 数据集、版本、Recall、引用、红队 | 没有真实模型/向量库运行就不能写效果 |
| Agent Runtime | 18 | Tool Schema、状态机、权限、攻击评测 | 可写架构设计；运行结果需独立证据 |
| 多实例分布式 | 19 | 多进程、lease/fencing、对账 | 无多实例实验时不写自动故障转移 |

## 三、事实台账示例

以下是记录格式示范，不是可直接复制的个人事实：

```text
fact_id: NF-JFR-001
source_type: course_lab
organization_or_project: NotifyFlow
time_range: 2026-07
task_or_problem: 观察线程池任务执行事件
my_actions: 编写并运行自定义 JFR 事件实验
technology_actually_used: Java 21, JFR, ThreadPoolExecutor
result: 以实验原始输出为准
environment: 本地 JDK 21 环境
evidence_refs: 对应实验 README、源码、输出和 recording 文件
what_i_did_not_do: 未证明完整 GC/锁/P99 因果，未在生产运行
allowed_wording: 在本地 Java 21 环境实现并运行自定义 JFR 事件链路
forbidden_wording: 建设生产级全链路 JVM 监控并将 P99 降低某百分比
```

## 四、三类项目摘要

以下模板必须由事实台账填充，方括号中的选择只保留证据成立的部分。

### Java 后端版

```text
NotifyFlow 可靠通知平台（独立工程项目）
- 围绕通知任务设计 Java 21 后端链路，覆盖任务状态、事务边界、幂等、异步消息与故障恢复。
- [设计/实现/运行验证] Transactional Outbox、重试/DLT、UNKNOWN 对账和低基数可观测性，并记录版本、环境和原始证据。
- 使用线程池、JFR 与负载模型分析队列、拒绝和长尾问题；所有量化结果限定在对应实验环境。
```

### Agent 后端版

```text
NotifyFlow Agent 事故助手（独立工程项目）
- 设计面向通知事故的 Tool Schema、权限、幂等键、运行状态机和审计记录，使模型建议与高风险执行分离。
- [设计/实现/运行验证] 文档 ingestion、混合检索、引用验证、拒答和红队评测管线。
- 将 timeout、UNKNOWN、retry、SSE 断线和人工审批纳入 Agent 后端可靠性边界。
```

### 先进制造数字化版

```text
NotifyFlow 可靠任务与异常协同（独立工程项目）
- 以可追溯任务状态、异步通知、幂等和异常对账为核心，设计适用于长链路业务集成的可靠处理方案。
- 通过状态机、审计、租户权限和 Runbook 约束自动化操作，保留人工审批和恢复入口。
- [设计/实现/运行验证] 容器部署、探针、观测和多实例接管；不声称真实工厂、MES 或设备接入经验。
```

## 五、大烨实习与独立项目隔离表

| 字段 | 大烨实习 | NotifyFlow/Agent 独立项目 |
|---|---|---|
| 组织归属 | 真实公司实习 | 个人/课程项目 |
| 时间 | 真实入离职日期 | 实际开发日期 |
| 技术 | 当时真实使用 | 后续学习和实现 |
| 数据 | 合法可描述的真实事实 | 本地/模拟/公开数据 |
| 结果 | 可核验实习交付 | 固定实验环境结果 |
| 禁止迁移 | 后学技术、课程实验数字 | 公司业务规模、客户和生产结果 |

简历排版相邻不代表事实可以合并。面试回答时也要主动说清来源。

## 六、证据入口设计

项目仓库首页应提供：

1. 项目问题与非目标。
2. 架构图和核心状态机。
3. 可复现环境和版本。
4. 已验证、静态检查和 Pending 三类状态。
5. 实验报告、原始输出和限制。
6. 安全与权限边界。
7. 演示路径和常见追问。

简历链接指向稳定入口，不直接指向本地文件、临时网盘或需要权限的公司材料。

## 七、项目答辩卡

每条项目 bullet 配一张答辩卡：

```text
claim_id:
90_second_story:
business_problem:
my_exact_contribution:
key_design_choice:
alternative_rejected:
failure_or_revision:
evidence_demo_path:
metric_definition:
limitations:
next_step:
```

答辩时先给结论，再展开证据；不确定时明确说出未验证边界，不使用模糊话术掩盖缺口。


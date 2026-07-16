# 第 18 章讲义：把 Agent 变成可靠后端系统

## 一、Agent 不是“模型一直想”

可靠 Agent 是一个由确定性代码管理的状态机。模型可以提出计划和工具参数，但权限、预算、状态迁移、超时、重试、审批、审计和停止条件由 Runtime 控制。

```text
RECEIVED -> CONTEXT_READY -> PLAN_READY -> TOOL_PROPOSED
-> AUTHORIZED -> EXECUTING -> VERIFYING -> RESPONDED
                         |         |
                       DENIED    UNKNOWN/RECOVERING
```

无限循环、无限 token、无限工具调用都不是智能，而是没有预算控制。

## 二、工作流、RAG 和 Agent

- 工作流：步骤和分支由代码确定，最容易验证。
- RAG Chatbot：检索证据后生成回答，通常不执行副作用。
- Agent：模型在受限集合中决定下一步或工具，但执行仍受状态机约束。
- 多 Agent：多个角色/模型协作，只有在任务可分解、权限分离或上下文隔离带来明确收益时使用。

优先用工作流解决确定问题，再为真正需要动态选择的步骤引入 Agent。

## 三、Tool 合同

Tool 不是一个方法名，至少包含：

```text
name/version
description
input/output JSON schema
tenant/authz
sideEffect: READ_ONLY | WRITE_LOW_RISK | WRITE_HIGH_RISK
idempotency
timeout/retry policy
rate/cost budget
audit fields
preview/approval/kill switch
```

模型只能生成结构化候选参数。Runtime 先做 schema、allowlist、租户、权限和业务状态校验，再执行。

## 四、可靠执行

### 4.1 幂等

每个有副作用 Tool 使用业务幂等键和唯一约束。模型重试不能产生第二个真实操作。

### 4.2 timeout 与 UNKNOWN

工具超时不等于失败。若下游可能已经执行，状态进入 UNKNOWN，禁止普通重试；通过查询、回调或对账收敛。

### 4.3 重试

只有明确可重试错误进入有限预算、指数退避和 jitter。模型层、Runtime 层、HTTP 客户端和下游不能同时各自无限重试。

### 4.4 崩溃恢复

每次状态迁移和工具尝试写入事件/数据库；进程重启后根据状态继续验证或恢复，不依赖模型记忆。

## 五、Memory 分层

- Conversation context：当前消息窗口，可丢弃和截断。
- Working memory：计划、当前步骤、工具结果引用，有 TTL。
- Long-term knowledge：经过权限和版本管理的 RAG 文档。
- Episodic memory：历史执行摘要和经验，需要评测是否带来收益。
- Business state：订单、通知、审批、工具执行结果，必须存数据库/事件系统。

摘要不是事实源。长期 memory 写入要有来源、租户、版本、删除和污染防护。

## 六、计划与循环

常见模式：ReAct、plan-and-execute、router、reflection、critic。Reflection 可能修复错误，也可能增加 token、延迟和自信错误；必须通过评测决定是否启用。

循环停止条件：最大步骤、最大 token、最大成本、最大工具次数、墙钟超时、用户取消、权限拒绝、重复状态和人工接管。

## 七、多 Agent

多 Agent 不是默认升级。需要明确：

- 每个角色的输入、输出、权限和责任。
- 协调者如何防止循环委派和消息爆炸。
- 共享状态如何版本化、去重和解决冲突。
- 单 Agent/工作流基线是否已经足够。

多 Agent 评测必须扣除额外成本、延迟、错误传播和观察难度。

## 八、安全威胁

- Prompt injection：文档/工具结果诱导覆盖系统规则。
- Tool injection：恶意工具描述或返回值要求执行额外动作。
- Data exfiltration：跨租户、Secret、日志、上下文泄露。
- Excessive agency：模型拥有过大权限和自动执行范围。
- Insecure output handling：模型输出直接进入 SQL/Shell/HTML。
- Memory poisoning：错误/恶意信息进入长期记忆。

防护：不信任模型和检索内容；最小权限；schema/allowlist；隔离执行；敏感字段清理；审批；审计；速率/成本限制；可终止。

## 九、Java Agent Runtime

核心组件：

- `AgentRunService`：创建 run、状态机和取消。
- `ModelGateway`：模型路由、超时、SSE、token/cost、错误分类。
- `ContextService`：权限检索、token 预算和引用。
- `ToolRegistry`：版本化 schema 与 side effect 元数据。
- `ToolExecutor`：RBAC、幂等、审批、执行、验证和审计。
- `RunEventRepository`：事件日志、崩溃恢复和重放。
- `EvaluationService`：任务、引用、安全、成本和失败样本。

SSE 只负责向客户端传递事件；最终 run 状态必须持久化，客户端断开不能让工具执行变成未知黑盒。

## 十、NotifyFlow Incident Agent

默认只读能力：查询任务时间线、聚合错误分类、检索 Runbook、生成带引用假设。高风险动作（重放、扩容、回滚、清理）只生成 preview/approval request，由确定性控制面执行。

Agent 的建议必须包含 evidence、confidence、alternatives、risk、requiredApproval 和 nextReadOnlyQuery。

## 十一、评测

| 维度 | 指标示例 |
|---|---|
| 任务 | completion rate、step accuracy、tool selection |
| RAG | Recall@k、citation、groundedness、拒答 |
| Tool | schema success、idempotency、UNKNOWN 收敛 |
| 安全 | 越权阻断、prompt injection、敏感数据泄露 |
| 可靠性 | 崩溃恢复、取消、超时、重复事件 |
| 性能 | TTFT、总时延、token、工具次数、成本 |

只用“回答看起来不错”不能发布 Agent。

## 十二、实验全部 Pending

计划验证 Tool schema、权限、幂等、UNKNOWN、人工审批、崩溃恢复、Memory 污染、prompt injection、单/多 Agent 消融、SSE 断开和成本预算。未运行前不得写成功率、节省时间或线上自动化比例。

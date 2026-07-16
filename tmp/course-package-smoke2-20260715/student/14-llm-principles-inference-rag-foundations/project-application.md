# NotifyFlow Knowledge Assistant 与 Agent Runtime

## 1. 目标

让工程师询问“为什么通知积压/失败/延迟上升”，系统返回带证据引用的诊断摘要和只读下一步，而不是直接执行恢复动作。

## 2. 数据分层

| 数据 | 存储 | 是否进入向量库 | 约束 |
|---|---|---|---|
| Runbook/课程文档 | 对象存储 + metadata DB | 是 | 版本、租户、权限 |
| 指标时间线 | Metrics backend | 摘要/窗口化 | 不存高基数原始 tag |
| 日志/Trace | Log/Trace backend | 脱敏摘要 | 保留访问审计 |
| 任务状态 | MySQL/Event | 否，按 ID 查询 | 状态机是真实来源 |
| Agent 会话 | 会话库 | 否 | TTL、租户、敏感字段 |

## 3. 查询链路

```text
用户问题
-> 身份/租户/权限
-> query rewrite（可选）
-> keyword + vector retrieval
-> metadata/time filter
-> rerank
-> context budget
-> LLM structured answer
-> citation/claim validator
-> 只读建议与审计
```

## 4. Tool 合同示例

```json
{
  "name": "query_notify_timeline",
  "version": "1",
  "sideEffect": "READ_ONLY",
  "input": {"taskId": "string", "from": "instant", "to": "instant"},
  "authz": ["notify:read:timeline"],
  "timeoutMs": 2000,
  "maxRows": 200
}
```

重放工具必须单独建模为 `WRITE_HIGH_RISK`，需要审批、幂等键、preview、状态机和审计，不能因为模型“判断应该重放”就执行。

## 5. Java 服务边界

- `RetrievalService`：权限过滤、混合检索和 rerank。
- `ContextBuilder`：token 预算、去重、引用映射和敏感字段清理。
- `ModelGateway`：超时、流式、成本、重试和 provider fallback。
- `ToolExecutor`：schema、RBAC、幂等、审计、限时和结果校验。
- `AgentStateRepository`：状态机、事件、重试和人工审批。

## 6. 正确性与安全

- 任何回答 claim 都要关联 evidence IDs；无证据时输出“不确定”。
- 检索过滤必须在向量查询前执行，避免越权 chunk 进入模型上下文。
- prompt injection 视为不可信文档内容，不能覆盖系统权限和工具合同。
- Tool 结果不能直接拼成 SQL/Shell；使用结构化参数和 allowlist。
- 失败重试不重复执行有副作用工具；UNKNOWN 进入查询/对账。

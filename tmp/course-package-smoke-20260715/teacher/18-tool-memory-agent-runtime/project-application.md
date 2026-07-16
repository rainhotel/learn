# NotifyFlow Agent Runtime 项目设计

## 1. 状态表

```text
agent_run(id, tenant_id, state, model, budget, created_at, updated_at)
agent_event(run_id, seq, type, payload_ref, created_at)
tool_attempt(id, run_id, tool, idempotency_key, state, request_ref, result_ref)
approval(id, run_id, action, requester, approver, state, reason)
```

大 payload、模型原文和工具结果放对象存储并加权限/TTL；数据库保存引用、hash 和审计字段。

## 2. Tool 风险分级

| Tool | 等级 | 控制 |
|---|---|---|
| query_timeline | READ_ONLY | 租户/RBAC/行数/时间窗 |
| query_metrics | READ_ONLY | allowlist/低基数/脱敏 |
| create_replay_preview | WRITE_LOW_RISK | 幂等/审计/不执行 |
| execute_replay | WRITE_HIGH_RISK | 双人审批/速率/kill switch |
| scale_consumer | WRITE_HIGH_RISK | 平台审批/容量/回滚 |

## 3. 运行时 API

```text
POST /agent-runs
GET  /agent-runs/{id}
GET  /agent-runs/{id}/events  (SSE)
POST /agent-runs/{id}/cancel
POST /agent-runs/{id}/approvals
POST /agent-runs/{id}/resume
```

所有写 API 需要 idempotency key；SSE 重连使用 Last-Event-ID，从持久化事件继续发送。

## 4. 故障流程

```text
Tool timeout
-> attempt UNKNOWN
-> run WAITING_RECONCILIATION
-> query/callback reconciliation
-> SUCCEEDED | FAILED_PERMANENT | MANUAL_REVIEW
```

Runtime 崩溃后扫描非终态 run，按租约领取并恢复，不能让两个实例同时执行同一高风险工具。

## 5. 简历边界

未真实实现和评测前只能写“设计 Agent Runtime 和安全工具协议”。只有保存代码、运行输出、攻击样本和恢复证据后，才能写成功率、延迟或自动化效果。

# 第 18 章实验：可靠 Agent Runtime

## 当前状态

- 状态：Pending
- 已完成：实验矩阵、状态断言、攻击样本和证据目录设计
- 未完成：Java Runtime、模型、工具、状态库和评测运行

## 实验矩阵

1. JSON Schema 和 Tool 参数校验。
2. RBAC/租户/side effect 风险分级。
3. 幂等键、重复请求和唯一约束。
4. Tool timeout、UNKNOWN、查询/回调对账。
5. 人工 preview/approval/reject/expire。
6. Runtime 崩溃、租约接管和事件恢复。
7. SSE 断开、Last-Event-ID、取消和最终状态。
8. Working/long-term memory 污染和删除。
9. Prompt injection、跨租户和敏感数据泄露。
10. 工作流/单 Agent/多 Agent 消融。
11. token、步骤、工具、时间和成本预算。
12. 高风险 replay/scale/rollback 的 kill switch。

## 证据

```text
evidence/<experiment>/
  environment.md
  tool-schema.json
  run-events.jsonl
  audit.jsonl
  attack-cases.jsonl
  metrics.csv
  correctness.md
  failure-cases.md
  conclusion.md
```

## 发布门槛

- 每个副作用工具有幂等和 UNKNOWN 恢复证据。
- 每个高风险工具有审批、审计、速率和 kill switch。
- 攻击样本包含 prompt injection、跨租户和 Secret 外泄。
- 评测同时报告完成率、安全、延迟、token、成本和失败样本。
- 未运行前不得填写自动化比例、成功率或节省时间。

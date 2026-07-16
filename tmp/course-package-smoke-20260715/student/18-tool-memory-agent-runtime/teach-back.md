# 第 18 章 Teach-back

## 5 分钟：Agent 是状态机

画出 plan、tool proposal、authz、execute、verify、unknown 和 approval，说明模型与 Runtime 的责任边界。

## 15 分钟：一次 Tool timeout

从工具候选、权限、幂等、执行超时、UNKNOWN、对账到最终审计，解释为什么不能直接重试。

## 45 分钟：NotifyFlow Incident Agent

RAG 证据 -> 只读工具 -> 状态机 -> SSE -> 高风险 preview/审批 -> 崩溃恢复 -> 安全攻击与评测。

## 验收

- 能区分 Memory 和业务状态。
- 能写高风险 Tool 的控制清单。
- 能回答客户端断线后工具是否继续、如何查询最终状态。
- 能明确 Agent 不直接清队列、扩容、回滚或读 Secret。

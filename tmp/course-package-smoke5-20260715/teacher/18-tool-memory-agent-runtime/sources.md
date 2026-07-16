# 第 18 章资料与验证状态

## 论文与安全资料

1. ReAct: Synergizing Reasoning and Acting：<https://arxiv.org/abs/2210.03629>
2. Toolformer：<https://arxiv.org/abs/2302.04761>
3. Reflexion：<https://arxiv.org/abs/2303.11366>
4. Plan-and-Solve Prompting：<https://arxiv.org/abs/2305.04091>
5. MemGPT：<https://arxiv.org/abs/2310.08560>
6. OWASP Top 10 for LLM Applications：<https://owasp.org/www-project-top-10-for-large-language-model-applications/>
7. NIST AI Risk Management Framework：<https://www.nist.gov/itl/ai-risk-management-framework>
8. Model Context Protocol specification：<https://modelcontextprotocol.io/specification/>

## 使用规则

- 论文展示方法，不证明在 NotifyFlow 上有效；必须做自己的评测。
- MCP/工具协议只解决交互格式的一部分，不替代业务权限、幂等和审计。
- 安全清单必须映射到真实 Tool、租户、Secret、日志和恢复控制面。

## 当前状态

| 项目 | 状态 | 证据 |
|---|---|---|
| Agent/Tool/Memory 原理 | 资料核验/讲义初稿 | 论文与规范 |
| Java Agent Runtime | 设计初稿 | 尚无运行实现 |
| Tool 幂等/UNKNOWN/审批 | 设计初稿 | 需复用第 08 章机制并实验 |
| Prompt injection/越权 | Pending | 尚无攻击评测输出 |
| 多 Agent 消融 | Pending | 尚无成本/质量数据 |

本章不能标记为 Lab Verified、Release Candidate 或 Released。

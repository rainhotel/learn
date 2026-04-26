# AI 厂商长期记忆实现调研 Working Notes

## Hypotheses

- 假设 1：商业产品里的“长期记忆”已经分化为不同层级，而不是单一能力。
- 假设 2：coding agent 的长期记忆比通用聊天产品更强调可审计、可编辑、本地化。

## Observations

- `ChatGPT`、`Gemini`、`Microsoft Copilot` 都把记忆分成“显式保存”与“历史推断”两层，只是命名不同。
- `Claude Code` 的特点不是只有 `CLAUDE.md`，而是把自动记忆做成了本地 markdown 目录，这让它具备更强的可见性和可维护性。
- `Codex` 目前公开文档里最稳定的持久层是 `AGENTS.md`，长任务连续性则更多依赖 compaction，而不是公开的 auto memory。
- `Codex` 的设计重心看起来更偏“跨长任务不丢状态”，`Claude Code` 更偏“跨会话持续积累项目经验”。

## Comparisons

- `Codex` vs `Claude Code`：
  - `Codex` 强在指令链和长任务上下文压缩。
  - `Claude Code` 强在项目级自动记忆与人工可审计。
- 消费级 assistant vs coding agent：
  - 前者重点是“记住用户”。
  - 后者重点是“记住项目与工作方式”。
- 论文范式 vs 商业实现：
  - 论文更激进，强调 memory stream、reflection、tiered memory。
  - 商业实现更克制，强调权限、删除控制、局部可见、成本可控。

## Open Threads

- `Codex` 未来是否会公开 `MEMORY.md` 风格的自动项目记忆层。
- `Claude Code auto memory` 的召回策略是否还有未公开的排序或衰减逻辑。
- 若自研 coding agent，是否应把“规则层”和“经验层”分文件管理。

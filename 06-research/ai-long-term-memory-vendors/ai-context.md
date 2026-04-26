# AI 厂商长期记忆实现调研 AI Context

## Research State

- Current stage: synthesized initial comparison
- Confidence: medium-high
- Last updated: 2026-03-30

## Evidence Map

- 已确认事实：
  - `Claude Code` 官方文档明确区分 `CLAUDE.md` 与 `auto memory` 两套机制，并公开了存储位置、加载规则、尺寸限制与 `/memory` 管理方式。
  - `Codex` 官方文档明确支持 `AGENTS.md` 指令链，并公开了 `compaction` 机制来支撑长时任务。
  - `ChatGPT`、`Gemini`、`Microsoft Copilot` 都公开支持某种“保存记忆 + 历史引用/推断”的个性化机制。
  - 相关论文里，长期记忆主要落在四类思路：显式记忆库、分层内存、递归摘要、反思与检索。
- 待验证说法：
  - `Codex` 是否存在未公开的自动项目记忆功能。
  - 各家在服务端是否使用统一 memory graph / vector store / KV cache 混合架构。
- 冲突信息：
  - 没看到直接冲突，但“产品 marketing 里的 memory”与“论文里的 memory mechanism”不是同一层概念，容易误读。

## Search Backlog

- 还需要找的材料：
  - `Codex` 是否有更细的 app/CLI 文档提到自动记忆或 resume memory。
  - `Claude Code` 关于 subagent memory 的更详细说明。
- 还需要验证的来源：
  - Google 与 Microsoft 是否公开披露更多记忆删除传播延迟或容量限制。
- 还需要比较的观点：
  - coding agent 的长期记忆，是否应该默认本地化、可审计、版本化。

## Next Best Edits

1. 补一页“设计启发：如果自己做 coding agent memory 应如何分层”。
2. 把 `Codex` 和 `Claude Code` 的能力差异抽成一个更严格的对照表。
3. 继续核验 `Codex` 是否已有公开的自动记忆层说明。

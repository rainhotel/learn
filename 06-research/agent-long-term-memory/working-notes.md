# Agent Long-Term Memory Working Notes

## Hypotheses

- 假设 1：生产可用的 agent memory 会收敛到分层设计，而不是单一向量库。
- 假设 2：遗忘曲线在 agent 中更适合做“调度与打分”，而不是直接做“删除”。
- 假设 3：随着任务变复杂，memory 的核心问题会从 recall 变成 lifecycle management。

## Observations

- 2023 年的代表作主要在解决“让模型能记住更久”：
- `Generative Agents` 用 observation / reflection / planning 把经验整理成高层记忆。
- `MemoryBank` 把时间和重要性纳入 memory preservation。
- `MemGPT` 把 memory tiers 和 context paging 做成 OS 风格。
- `Recursively Summarizing...` 证明“递归摘要”是一条简单但有效的长对话 memory 路线。

- 2024 年开始，研究开始系统化：
- `A Survey on the Memory Mechanism...` 更像 agent memory 地图。
- `Human-inspired Perspectives...` 明确把 human long-term memory 映射到 AI 设计。

- 2025 年的焦点明显变成“动态组织 + 时间变化 + memory evolution”：
- `A-MEM` 强调动态索引、链接、旧记忆更新。
- `RMM` 强调多粒度记忆摘要和在线 retrieval refinement。
- `Zep/Graphiti` 把时间有效区间、冲突事实、graph retrieval 做成一体。
- `Mem0` 强调 production-ready 的 extraction / consolidation / retrieval pipeline。
- `MemOS` 开始尝试统一 plaintext、activation、parameter 三种 memory 形态。

## Comparisons

### Letta vs Mem0

- `Letta` 更像“stateful agent runtime”，把一部分 memory 作为 always-visible core memory。
- `Mem0` 更像“universal memory layer”，把 memory 当成独立服务，通过 add/search/update 工作流接到任意 agent。
- 如果要做 persona、policy、scratchpad，`Letta` 风格更自然。
- 如果要做用户偏好、事实、跨会话 retrieval，`Mem0` 风格更标准化。

### Mem0 vs LangMem

- `Mem0` 更偏产品化 memory service，强调 hosted/self-hosted 双模式和现成 API。
- `LangMem` 更偏 framework-first，直接挂在 LangGraph/LangChain 的 store 上。
- `LangMem` 的亮点是把 memory 写入分成 hot path 和 background manager。

### Graphiti/Zep vs 向量型 memory

- 向量型 memory 擅长“近义召回”和快速落地。
- `Graphiti/Zep` 擅长状态变化、实体关系、时间有效性、多跳关联。
- 一旦业务里出现“之前成立、现在失效”的事实，图和时间建模价值会迅速上升。

### A-MEM / RMM / MemOS

- 这三者更像下一阶段研究方向，而不只是现成工程组件。
- `A-MEM` 关注记忆如何自组织。
- `RMM` 关注记忆粒度与 retrieval refinement。
- `MemOS` 关注 memory 当作系统资源后的调度、迁移、融合。

### MemoryBank / FSRS / OpenMemory

- `MemoryBank` 是把 forgetting curve 明确放进 LLM memory 的论文化实现。
- `FSRS` 是把遗忘/复习建模做得最成熟的工程路线，但场景是学习调度，不是 agent 对话。
- `OpenMemory` 试图把 decay、graph、MCP、本地优先放到同一个 memory engine 里，方向值得看，但需要更多第三方验证。

## Open Threads

- 应不应该区分 `episodic memory`、`semantic memory`、`procedural memory` 三张表/三类索引？
- decay 应该作用在原始 episode、提炼后的 fact，还是 retrieval score？
- graph memory 什么时候值得引入，什么时候向量检索就够了？
- “反思”应该在用户请求热路径中做，还是异步后台做？
- memory 的删除策略如何兼顾隐私合规、可追溯和回答质量？

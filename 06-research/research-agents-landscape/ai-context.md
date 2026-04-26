# Research Agents Landscape AI Context

## Research State

- Current stage: Synthesis
- Confidence: Medium-high on product categories and high-level architecture; medium on hidden internal implementations
- Last updated: 2026-03-30

## Evidence Map

- 已确认事实：
  - `OpenAI Deep Research`、`Gemini Deep Research`、`Perplexity Research`、`Claude Research`、`Microsoft Researcher` 都已把“多步研究 + 来源引用 + 分钟级交付”产品化。
  - `Elicit`、`Consensus Deep Search`、`SciSpace` 明确把“论文综述 / literature review / structured evidence extraction”作为核心能力。
  - `Google AI co-scientist` 明确公开了多智能体架构：`Supervisor + Generation + Reflection + Ranking + Evolution + Proximity + Meta-review`。
  - `STORM` 公开了“先研究并生成 outline，再写作”的知识整理范式。
- 待验证说法：
  - 各家对“处理多少来源”“胜率多少”“速度多少”的宣传口径。
  - Genspark 内部的多模型路由、工具编排细节。
  - FutureHouse 平台与 Edison Scientific 的产品边界。
- 冲突信息：
  - 通用 research agent 的界限有时与“超级代理”混在一起。
  - 科研智能体在宣传中常把“literature review assistant”和“AI scientist”放在同一叙事里，但工程复杂度差异很大。

## Search Backlog

- 还需要找的材料：
  - 更偏开源实现的 research agent 样本。
  - 更公开透明的 citation graph / evidence store 设计案例。
  - research agent 的评测基准与 failure taxonomy。
- 还需要验证的来源：
  - Microsoft Researcher 的更细化产品说明。
  - Anthropic Research 的官方新闻稿与帮助页之间的差异。
  - Elicit、Consensus 的评测方法是否有第三方复现。
- 还需要比较的观点：
  - `single orchestrator` vs `multi-agent`
  - `web-first` vs `scholarly-corpus-first`
  - `report agent` vs `hypothesis/experiment agent`

## Next Best Edits

1. 单独整理一页“学习智能体 MVP 架构图”。
2. 补充 2-3 个开源代码样本，避免完全依赖产品页。
3. 为本仓库写一版“如何把 research output 整理回 topic notes”的工作流。

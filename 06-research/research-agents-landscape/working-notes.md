# Research Agents Landscape Working Notes

## Hypotheses

- 假设 1：市面主流 research agent 的稳定主干已经不是 RAG，而是 `planner + evidence loop + synthesis + citation`。
- 假设 2：如果是给“学习系统”做第一版，最值得学的是 `evidence-backed topic report agent`，而不是直接模仿 AI scientist。
- 假设 3：科研智能体真正难的地方不是检索，而是证据纪律、冲突处理和验证闭环。

## Observations

- OpenAI、Google、Perplexity、Anthropic、Microsoft 都在强调复杂任务、多步研究、可验证来源与最终交付物。
- 通用研究智能体的主要差异，越来越从“能不能搜网页”转向“能不能接私域数据”和“能不能输出更完整交付物”。
- Elicit、Consensus、SciSpace 这类科研助手已经把 `literature review` 当成明确工作流，而不是一个泛化问答模式。
- Google AI co-scientist 公开得最完整，说明科研多智能体常见模块包括：生成、反思、排序、进化、元评审。
- STORM 对学习场景很重要，因为它把“研究”和“写作”拆成了两个可控阶段。

## Comparisons

### 通用研究智能体 vs 科研工作流智能体

- 通用研究智能体：
  - 主要面向网页与开放信息。
  - 更强调 broad browsing、报告写作、任务交付。
  - 适合做市场分析、产品调研、方案综述。
- 科研工作流智能体：
  - 主要面向论文库与科研工作流。
  - 更强调 screening、extraction、evidence table、literature review。
  - 适合做综述、证据综合、研究问题澄清。

### 科研工作流智能体 vs AI Scientist

- 科研工作流智能体：
  - 仍以“整理已有知识”为主。
  - output 更像综述、证据表、研究摘要。
- AI Scientist：
  - 试图提出新假设、设计实验、更新世界模型。
  - output 更像研究计划、实验建议、甚至论文草稿。
  - 风险和复杂度明显更高。

### Single Orchestrator vs Multi-Agent

- `single orchestrator + specialized passes`
  - 更容易做对状态管理和引用映射。
  - 更适合学习型系统的第一版。
- `multi-agent`
  - 更适合复杂科研推理、辩论、排名、进化。
  - 更依赖预算控制、自动评审和失败恢复。

## Open Threads

- 有没有公开得足够好的 evidence store / citation graph 设计可以直接借鉴？
- research agent 的评测应该重点看什么：事实正确率、来源覆盖率、反例覆盖率、还是任务完成度？
- 如果研究输出最终要回流进本仓库，是落成 `01-topics/*` 的稳定知识，还是继续留在 `06-research/*`？

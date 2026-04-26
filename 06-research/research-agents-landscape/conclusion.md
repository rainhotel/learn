# Research Agents Landscape Conclusion

## Final Position For Now

- 当前结论：
  - 2025-2026 的主流研究智能体已经从“会搜索的聊天机器人”演进为“能规划、能多轮取证、能生成带引用交付物的研究工作流”。
  - 科研智能体与通用研究智能体共享同一条主架构主线，但科研场景必须额外处理学术语料、证据质量、反例与假设验证。
  - 对这个仓库最有学习价值的，不是直接追 AI scientist，而是先学会做一个可复查的 research report agent。

## Evidence Chain

1. OpenAI、Google、Perplexity、Anthropic、Microsoft 都把“多步研究 + 来源引用 + 分钟级交付”做成了明确产品能力。
2. Elicit、Consensus、SciSpace 证明，科研工作流里最稳定、最可产品化的一层是 literature review、screening、extraction、report。
3. STORM 说明“先研究并产出 outline，再写长文”是一个可落地、可控、适合知识整理的结构。
4. Google AI co-scientist 说明科研多智能体常见做法是把生成、反思、排序、进化、元评审显式拆开。
5. FutureHouse、The AI Scientist 等系统说明“scientific discovery loop”是更远的目标层，难点在世界模型、假设与实验闭环，而不只是检索。

## What Seems True

- 通用 research agent 的共性模块可以抽象成：
  - `Task Planner`
  - `Evidence Retrieval Loop`
  - `Evidence Store / Citation Mapping`
  - `Synthesis`
  - `Critique / Verification`
  - `Deliverable Renderer`
- 学习场景最适合先做的版本：
  - 输入：研究问题、约束、预算
  - 过程：研究计划、多轮检索、证据整理、提纲、写作、自检
  - 输出：正文、引用清单、不确定性、下一步问题
- 先做重型多智能体不是最优：
  - 多智能体更像第二阶段增强，而不是第一阶段必需品。
  - 对第一版来说，`single orchestrator + specialized passes` 更容易做稳。

## What Is Still Uncertain

- 闭源产品的内部记忆层、预算控制、重试策略没有公开到足够细。
- 产品宣传的 benchmark 多是自报，不能直接当作工程事实。
- 学习型 research agent 是否需要一开始就接入 topic 笔记、历史日志与个人知识库，还需要单独设计。

## Recommendation

- 是否继续研究：是
- 最值得继续的方向：
  - 先写一版“学习智能体 MVP 架构图”。
  - 补充开源实现样本，尤其是 STORM 与科研证据工作流。
  - 设计一条从 `research output` 回流到 `01-topics/*` 的整理链路。

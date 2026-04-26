# Agent Long-Term Memory Source Log

## Sources

### Generative Agents: Interactive Simulacra of Human Behavior

- Type: paper
- Link or identifier: https://arxiv.org/abs/2304.03442
- Date: 2023-04-07, accessed 2026-03-30
- Why it matters: 早期代表性架构，把 observation / planning / reflection 与长期经验记录结合起来。
- Reliability: 高，原始论文
- Used for: 长期记忆架构起点、reflection 机制来源

### MemoryBank: Enhancing Large Language Models with Long-Term Memory

- Type: paper
- Link or identifier: https://arxiv.org/abs/2305.10250
- Date: 2023-05-17, accessed 2026-03-30
- Why it matters: 明确把 Ebbinghaus forgetting curve 引入 LLM memory update。
- Reliability: 高，原始论文
- Used for: 遗忘曲线与 agent memory 的直接连接

### MemGPT: Towards LLMs as Operating Systems

- Type: paper
- Link or identifier: https://arxiv.org/abs/2310.08560
- Date: 2023-10-12, accessed 2026-03-30
- Why it matters: 提出 virtual context management 与分层内存思路，是“memory as OS resource”的关键来源。
- Reliability: 高，原始论文
- Used for: memory tiers、OS 类比、对 Letta 的设计背景理解

### Recursively Summarizing Enables Long-Term Dialogue Memory in Large Language Models

- Type: paper
- Link or identifier: https://arxiv.org/abs/2308.15022
- Date: 2023-08-29, accessed 2026-03-30
- Why it matters: 代表“递归摘要/压缩记忆”路线。
- Reliability: 高，原始论文
- Used for: 总结型 memory 路线

### A Survey on the Memory Mechanism of Large Language Model based Agents

- Type: survey paper
- Link or identifier: https://arxiv.org/abs/2404.13501
- Date: 2024-04-21, accessed 2026-03-30
- Why it matters: 对 agent memory 设计、评测和应用做系统综述。
- Reliability: 高，原始论文
- Used for: 术语和分类对齐

### Human-inspired Perspectives: A Survey on AI Long-term Memory

- Type: survey paper
- Link or identifier: https://arxiv.org/abs/2411.00489
- Date: 2024-11-01, accessed 2026-03-30
- Why it matters: 用 human memory 机制映射 AI long-term memory，并提出 SALM 框架。
- Reliability: 高，原始论文
- Used for: 人类记忆到 AI 记忆的映射视角

### A-MEM: Agentic Memory for LLM Agents

- Type: paper
- Link or identifier: https://arxiv.org/abs/2502.12110
- Date: 2025-02-17, accessed 2026-03-30
- Why it matters: 从固定 memory schema 转向 agentic organization，强调动态索引、链接和 memory evolution。
- Reliability: 高，原始论文
- Used for: 2025 年的动态组织路线

### In Prospect and Retrospect: Reflective Memory Management for Long-term Personalized Dialogue Agents

- Type: paper
- Link or identifier: https://arxiv.org/abs/2503.08026
- Date: 2025-03-11, accessed 2026-03-30
- Why it matters: 引入 prospective / retrospective reflection，把 memory granularity 和 retrieval refinement 做成闭环。
- Reliability: 高，原始论文
- Used for: 反思式 memory management

### Mem0: Building Production-Ready AI Agents with Scalable Long-Term Memory

- Type: paper
- Link or identifier: https://arxiv.org/abs/2504.19413
- Date: 2025-04-28, accessed 2026-03-30
- Why it matters: 明确面向 production-ready memory，强调 extraction / consolidation / retrieval 与部署成本。
- Reliability: 高，原始论文
- Used for: 工程化 memory pipeline

### MemOS: A Memory OS for AI System

- Type: paper
- Link or identifier: https://arxiv.org/abs/2507.03724
- Date: 2025-07-04, accessed 2026-03-30
- Why it matters: 提出 memory OS，把 plaintext、activation、parameter memory 统一建模。
- Reliability: 高，原始论文
- Used for: memory lifecycle 与跨层迁移

### Letta Repository

- Type: open-source project
- Link or identifier: https://github.com/letta-ai/letta
- Date: accessed 2026-03-30
- Why it matters: 主流 stateful agents 项目，GitHub 页面显示约 2.18 万 stars，开源影响力高。
- Reliability: 高，官方仓库
- Used for: 工程化 core memory / stateful agent 参考

### Letta Memory Docs

- Type: official docs
- Link or identifier: https://docs.letta.com/guides/agents/memory
- Date: accessed 2026-03-30
- Why it matters: 明确区分 always-visible memory blocks 与 external memory。
- Reliability: 高，官方文档
- Used for: core memory 设计

### Mem0 Repository

- Type: open-source project
- Link or identifier: https://github.com/mem0ai/mem0
- Date: accessed 2026-03-30
- Why it matters: 主流 memory layer 项目，GitHub 页面显示约 4.4 万到 4.7 万 stars。
- Reliability: 高，官方仓库
- Used for: 生产化 memory add/search/update pipeline

### Mem0 Docs

- Type: official docs
- Link or identifier: https://docs.mem0.ai/core-concepts/overview
- Date: accessed 2026-03-30
- Why it matters: 直接说明 add/search/update 的工程流程。
- Reliability: 高，官方文档
- Used for: extraction、conflict resolution、rerank

### LangChain Long-term Memory Docs

- Type: official docs
- Link or identifier: https://docs.langchain.com/oss/python/deepagents/long-term-memory
- Date: accessed 2026-03-30
- Why it matters: 代表 framework-first 的 memory storage 设计。
- Reliability: 高，官方文档
- Used for: namespace/key store、production store 建议

### LangMem Example Repository

- Type: open-source project
- Link or identifier: https://github.com/langchain-ai/langgraph-memory
- Date: accessed 2026-03-30
- Why it matters: 代表 LangChain 的 memory service 示例，强调 hot path + background memory manager。
- Reliability: 中高，官方示例仓库，但不是大型独立产品
- Used for: memory write timing 与后台 consolidation

### Graphiti Repository

- Type: open-source project
- Link or identifier: https://github.com/getzep/graphiti
- Date: accessed 2026-03-30
- Why it matters: 主流 temporal knowledge graph memory 项目，GitHub 页面显示约 2.1 万 stars。
- Reliability: 高，官方仓库
- Used for: temporal graph、hybrid retrieval、dynamic updates

### Zep: A Temporal Knowledge Graph Architecture for Agent Memory

- Type: paper
- Link or identifier: https://arxiv.org/abs/2501.13956
- Date: 2025-01-23, accessed 2026-03-30
- Why it matters: 从论文角度系统解释 Graphiti/Zep 的 temporal KG 架构与评测。
- Reliability: 高，原始论文
- Used for: time-aware memory engineering

### OpenMemory Repository

- Type: open-source project
- Link or identifier: https://github.com/CaviraOSS/OpenMemory
- Date: accessed 2026-03-30
- Why it matters: 本地优先、MCP 原生、强调 decay 和 explainability 的开源 memory engine。
- Reliability: 中，官方仓库，但性能和竞品对比多为 README 自述
- Used for: 本地优先与 MCP memory server 思路

### FSRS4Anki / Open Spaced Repetition

- Type: open-source project + algorithm docs
- Link or identifier: https://github.com/open-spaced-repetition/fsrs4anki
- Date: accessed 2026-03-30
- Why it matters: 遗忘曲线工程化最成熟的开源路线之一，可借鉴到 memory reinforcement/scheduling。
- Reliability: 高，官方仓库和文档
- Used for: forgetting curve 的工程实现方式

# Agent Long-Term Memory AI Context

## Research State

- Current stage: first-pass survey completed
- Confidence: medium
- Last updated: 2026-03-30

## Evidence Map

- 已确认事实：
- 2023-2025 的代表性论文已经从“如何记住”转向“如何组织、更新、演化和调度记忆”。
- 主流工程路线可以粗分为四类：prompt-visible core memory、vector/fact memory、temporal graph memory、memory OS / multi-tier memory。
- LangMem、Mem0、Letta、Graphiti/Zep、MemOS 分别代表了不同工程路线。
- MemoryBank 明确把 Ebbinghaus forgetting curve 引入了 memory update 机制。
- FSRS 代表“遗忘曲线工程化”的成熟路线，但它来自学习调度系统，不是原生 agent memory 框架。
- 待验证说法：
- 各家 README 中关于 benchmark、延迟、token 节省的数字有一部分来自作者自测或自选配置，不能直接视为统一公平对比。
- OpenMemory 对“decay + cognitive engine”的描述较激进，需要更多第三方验证。
- 冲突信息：
- 有些系统把 memory 视作 retrieval 层，有些把 memory 视作 agent state / OS 资源，设计边界并不一致。

## Search Backlog

- 还需要找的材料：
- LOCOMO、LongMemEval、DMR 三个 benchmark 的评测维度与局限
- 安全问题：memory poisoning、prompt injection through memory、隐私与删除权
- 还需要验证的来源：
- Mem0 论文正式 arXiv 页面与项目 README 的指标口径
- MemOS 的实际开源可用性与落地门槛
- 还需要比较的观点：
- graph memory vs vector memory 的成本收益分界点
- decay scoring vs hard deletion 的产品效果差异

## Next Best Edits

1. 提炼一版“最小可行 agent memory architecture”到独立主题或 prompt 模板
2. 补 benchmark 对照表：LOCOMO / LongMemEval / DMR
3. 选一个开源项目做最小原型复现，优先比较 `Mem0`、`LangMem`、`Graphiti`

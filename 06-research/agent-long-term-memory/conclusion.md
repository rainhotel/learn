# Agent Long-Term Memory Conclusion

## Final Position For Now

- 当前结论：截至 2026-03-30，主流 agent long-term memory 已经从“外挂一个向量库”演化为“分层记忆 + 后台整理 + 时间感知 + 持续更新”的系统设计。
- 当前结论：遗忘曲线在 agent memory 里更适合落成“分数衰减、复习调度、压缩与再提炼触发器”，而不是简单的物理删除。
- 当前结论：如果业务存在状态变化、跨会话 personalization、长期任务连续性，memory lifecycle 比单次 recall 更重要。

## Evidence Chain

1. `Generative Agents`、`MemGPT`、`Recursively Summarizing...` 奠定了 reflection、memory tiers、summary compression 这些基础模式。
2. `MemoryBank` 把 Ebbinghaus forgetting curve 引入 LLM 长期记忆，说明“遗忘”早已不是纯教育软件问题。
3. `A-MEM`、`RMM`、`MemOS` 表明研究前沿正在从简单存取转向动态组织、反思更新、跨层迁移。
4. `Mem0`、`LangMem`、`Letta`、`Graphiti/Zep` 这些主流开源项目已经把不同 memory 路线做成可接入工程组件。
5. `Graphiti/Zep` 和 `Mem0^g` 的出现说明：当事实会变化、关系会演化时，纯向量 memory 很快不够用。

## What Seems True

- 长期记忆最好至少分成三层：
- `core memory`：始终可见，适合 persona、policy、当前任务状态
- `episodic memory`：原始事件/会话日志，可追溯
- `semantic memory`：抽取后的事实、偏好、规则、经验

- 写入路径最好分成两类：
- `hot path`：轻量写入或查询，保证交互延迟
- `background path`：摘要、去重、冲突解决、合并、重写索引

- 检索最好不是单一路径：
- 语义检索负责召回
- 关键词/结构检索负责精确定位
- 时间/图结构检索负责状态变化和多跳关系

- “忘记”更像 memory management，而不是 memory absence：
- 降权
- 延迟复习
- 归档
- 摘要压缩
- 被新事实覆盖并失效

## What Is Still Uncertain

- benchmark 上的优势能否稳定迁移到真实业务日志与工具调用环境
- graph memory 相比向量 memory 的成本拐点在哪里
- parameter-level memory 在开源工程里的实用门槛是否过高
- decay 策略是否会伤害罕见但关键的高价值记忆

## Recommendation

- 是否继续研究：是
- 最值得继续的方向：
- 先做一个最小可行 memory stack：
- `core memory`
- `episodic event store`
- `semantic fact store`
- `background consolidator`
- `temporal invalidation`

- 项目选型建议：
- 要最快接入：优先看 `Mem0`、`LangMem`
- 要做 stateful agent：优先看 `Letta`
- 要做时间变化和复杂关系：优先看 `Graphiti/Zep`
- 要做研究导向原型：优先看 `A-MEM`、`MemOS`
- 要试遗忘/复习调度：优先借鉴 `MemoryBank` 与 `FSRS`

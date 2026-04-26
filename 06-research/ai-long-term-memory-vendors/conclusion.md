# AI 厂商长期记忆实现调研 Conclusion

## Final Position For Now

- 当前结论：
  - 主流 AI 厂商对“长期记忆”的实现，已经稳定分成四种产品层：
    - 显式保存的用户记忆
    - 从历史对话中抽取的隐式记忆
    - 项目/仓库级的规则文件
    - 为长任务服务的上下文压缩或摘要
  - 如果只看 coding agent，`Claude Code` 与 `Codex` 的路线并不相同：
    - `Claude Code` 更像“规则文件 + 自动项目记忆库”
    - `Codex` 更像“规则文件 + 长时任务上下文压缩”

## Market Pattern

| 厂商 / 产品 | 长期记忆对象 | 主要载体 | 写入方式 | 读取方式 | 备注 |
| --- | --- | --- | --- | --- | --- |
| OpenAI / ChatGPT | 用户偏好、目标、历史聊天 | saved memories + chat history | 用户显式要求 + 系统自动保存 | 后续聊天自动引用 | 面向个性化助手 |
| Google / Gemini | past chats、connected apps、instructions | chat memory + app data + instructions | 历史聊天与连接应用共同提供 | 响应时综合个性化 | 面向个人生态整合 |
| Microsoft / Copilot | saved memories、chat history inference、custom instructions | memory + history + personalization | 自动识别、用户要求保存、设置控制 | 后续 Copilot Chat 引用 | 面向工作流个性化 |
| Anthropic / Claude Code | 项目规则、用户规则、自动项目经验 | `CLAUDE.md` + `~/.claude/projects/<project>/memory/` | 人工维护 + Claude 自动写 | 启动加载 + 按需读文件 | 偏工程协作记忆 |
| OpenAI / Codex | 全局/项目规则、长任务状态 | `AGENTS.md` + compaction item | 人工维护 + 系统压缩上下文 | 启动读取 + 压缩后续传 | 偏长任务续航与规则继承 |

## Focus: Codex vs Claude Code

### Claude Code

- 官方文档明确说，每次会话都从 fresh context window 开始，但有两种机制跨会话保留知识：
  - `CLAUDE.md`
  - `auto memory`
- `auto memory` 默认开启，按仓库存到 `~/.claude/projects/<project>/memory/`。
- `MEMORY.md` 前 200 行或前 25KB 会在每次会话开始时加载，其他 topic 文件按需读取。
- 这意味着 `Claude Code` 已经具备：
  - 可持续积累
  - 可审计
  - 可编辑
  - 项目级隔离

### Codex

- 官方文档明确提供的是：
  - `AGENTS.md` 指令链
  - `compaction` 机制
- `AGENTS.md` 负责跨仓库、跨目录的持久规则继承。
- `compaction` 负责在长对话或长任务中把 prior state 和 reasoning 压缩成更小的上下文项继续传递。
- 官方在 2025-12-18 发布 `GPT-5.2-Codex` 时，明确强调其 `long-horizon work` 与 `context compaction`。
- 基于当前公开资料，我的判断是：
  - `Codex` 已公开的长期记忆重点不是“自动沉淀项目经验库”，而是“显式规则 + 长时上下文续航”。
  - 这是一个推断，不是 OpenAI 文档中的原句。

### 核心差异

| 维度 | Claude Code | Codex |
| --- | --- | --- |
| 规则持久化 | `CLAUDE.md` | `AGENTS.md` |
| 自动记忆 | 有，且公开 | 当前公开资料未见对等机制 |
| 存储可见性 | 高，markdown 文件可编辑 | 规则层高；compaction 项本身不可解释 |
| 长任务续航 | 有，但本页重点不在此 | 很强，官方突出 compaction |
| 更像什么 | 项目记忆库 | 长任务状态管理器 |

## Papers To Product Mapping

- `MemoryBank`：
  - 代表“显式记忆库 + 更新/遗忘机制”。
  - 更接近 `Claude Code auto memory` 以及消费级助手里的 saved memory。
- `MemGPT`：
  - 代表“分层内存 + 虚拟上下文管理”。
  - 更接近 `Codex compaction` 这种长任务上下文治理思路。
- `Recursively Summarizing...`：
  - 代表“用递归摘要维持长期对话连续性”。
  - 也更接近 `Codex` 当前公开的 compaction 路线。
- `Generative Agents`：
  - 代表“记忆流 + 反思 + 检索 + 规划”。
  - 对未来 agent memory 设计启发很大，但今天的商业 coding agent 大多只吸收了其中一部分。

## Evidence Chain

1. `ChatGPT`、`Gemini`、`Copilot` 的官方文档都明确把长期记忆落在“保存记忆 + 历史引用/推断 + 用户控制”上。
2. `Claude Code` 官方文档明确公开了 `CLAUDE.md`、`auto memory`、存储位置、加载上限与 `/memory` 管理入口。
3. `Codex` 官方文档明确公开了 `AGENTS.md` 与 `compaction`，且官方发布文案强调 long-horizon work through context compaction。
4. 相关论文中，商业产品最常吸收的是四类技术路线：显式记忆库、分层内存、递归摘要、反思检索。
5. 因此，当前市场不是在竞争“有没有记忆”，而是在竞争“把哪种记忆产品化，以及让用户能控制到什么程度”。

## What Seems True

- “长期记忆”不是单点功能，而是一组分层机制。
- 通用聊天产品优先做“用户画像与偏好连续性”。
- coding agent 更需要“项目规则连续性”和“长任务状态连续性”。
- `Claude Code` 在公开可见的项目级自动记忆上走得更远。
- `Codex` 在长任务上下文管理和指令链继承上更明确。

## What Is Still Uncertain

- `Codex` 是否已在部分客户端或灰度版本中支持 auto memory，但尚未写进公开文档。
- 各家记忆召回是否使用统一的 embedding / graph / summarization 混合栈。
- 各家真实线上系统里，“遗忘”“纠错”“去陈旧化”到底有多自动。

## Recommendation

- 是否继续研究：是
- 最值得继续的方向：
  - 单独做一份“coding agent memory 设计蓝图”
  - 把 `Codex` 与 `Claude Code` 的记忆能力拆成：规则层、经验层、任务层、历史层
  - 研究“可编辑 memory file”与“opaque compaction item”两种设计的权衡

### get-shit-done (GSD) as an Ecosystem Pattern

- `get-shit-done` 不是模型厂商原生 memory，而是一种围绕 `Claude Code` 的外部状态管理方案。
- 它公开宣称自己解决的是 `context rot`，即长会话里上下文质量下降的问题。
- 它的核心做法不是让模型“自己记住更多”，而是把关键状态外置到 `.planning/` 目录：
  - `PROJECT.md`：项目愿景与上下文，文档标注为 always loaded
  - `REQUIREMENTS.md`：需求与范围
  - `ROADMAP.md`：阶段拆分与状态
  - `STATE.md`：决策、阻塞、session memory
  - `HANDOFF.json`：暂停/恢复时的结构化交接
  - phase 级的 `CONTEXT.md`、`RESEARCH.md`、`VERIFICATION.md`、`UI-SPEC.md`
- 它还把 brownfield codebase 的结构、约定和问题显式映射到 `codebase/STACK.md`、`ARCHITECTURE.md`、`CONVENTIONS.md`、`CONCERNS.md`。
- 这说明 GSD 提供的不是“长期记忆模型”，而是“长期记忆工作流”：
  - 用文件把规范、决策、研究和 handoff 固化
  - 用命令把这些文件在不同阶段生成、更新、校验
  - 用 fresh context + resume 机制代替“持续把所有上下文塞在同一会话里”
- 对 `Claude Code` 的意义：
  - 它补强了 `CLAUDE.md`/auto memory 不足以覆盖的项目管理层
  - 它把“项目规范”从一份静态规则文件升级成了多工件、多阶段、可恢复的规范系统
- 优势：
  - 规范更结构化，可审计，可分层
  - 对长项目和多人协作更稳，因为关键状态都落在文件里
  - 明确对抗 context rot，而不是依赖单次超长上下文
- 劣势：
  - 成本更高，需要遵守 GSD 的命令与流程
  - 工件数量多，轻量项目可能会嫌重
  - 它解决的是“外部状态治理”，不是底层模型真的学会长期记忆
- 因此，它更适合作为一个研究判断：
  - 当原生 agent memory 不够强时，项目规范与会话状态可以通过外部 artifact system 来补齐
  - 这条路线对 `Claude Code` 有效，对 `Codex` 其实同样有启发

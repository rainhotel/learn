# AI Learning Repo

这个仓库是一个和 AI 协作的学习操作系统，不只是笔记仓库。

它负责三类事情：

1. 系统学习一个主题
2. 对题目做标准化解答并沉淀知识点
3. 围绕一个想法或方向做研究、收集材料、形成结论与复盘

## 设计原则

1. 主题知识和时间记录分开。
2. 解题记录和开放研究分开。
3. 给人看的内容和给 AI 看的内容分开。
4. 任何一次学习活动都要能归档为：知识点、进度、产出、下一步。
5. 默认采用完整大纲驱动，不采用零散问题驱动。
6. 人类视图优先维护，因为它直接决定学习能否持续。

## 目录结构

```text
.
|-- 00-inbox/              # 想法、碎片问题、待整理方向
|-- 01-topics/             # 按主题沉淀长期知识
|   `-- _template/         # 主题模板
|-- 02-journal/            # 按时间记录学习过程
|   `-- _templates/        # 学习日志模板
|-- 03-resources/          # 外部资料与资料索引
|-- 04-prompts/            # 和 AI 协作的固定提示词
|-- 05-meta/               # 学习系统设计、全局进度、规则
|-- 06-research/           # 开放研究与方向探索
|   `-- _template/         # 研究模板
|-- skills/                # 仓库内本地 skill
`-- scripts/               # 创建主题/日志/研究目录的脚本
```

## 三条核心工作流

### 1. 主题学习流

适用场景：你要系统学一个领域，比如 TypeScript、操作系统、线性代数。

核心文件：

- `01-topics/<topic>/README.md`
- `01-topics/<topic>/outline.md`
- `01-topics/<topic>/human-guide.md`
- `01-topics/<topic>/ai-context.md`
- `01-topics/<topic>/notes.md`
- `01-topics/<topic>/qa.md`
- `01-topics/<topic>/projects.md`
- `01-topics/<topic>/solved-problems.md`
- `01-topics/<topic>/formula-sheet.md`
- `01-topics/<topic>/progress.md`
- `01-topics/<topic>/review.md`

### 2. 标准解题流

适用场景：你给我一道题，希望我解答，并把答案来源、公式、知识点、错误点讲清楚。

记录位置：

- 题目归档进对应主题的 `solved-problems.md`
- 用到的公式归档进 `formula-sheet.md`
- 新暴露的薄弱点更新进 `qa.md`
- 进度变化更新进 `progress.md`

每道题都尽量记录：

- 题目来源
- 题目正文
- 最终答案
- 解题步骤
- 使用的公式/定理/方法
- 公式含义和适用条件
- 参考来源
- 关联知识点
- 这道题暴露出的薄弱点

### 3. 开放研究流

适用场景：你给一个设想、直觉、灵感或方向，希望我帮你搜材料、研究、形成结论。

记录位置：

- `06-research/<direction>/README.md`
- `06-research/<direction>/human-brief.md`
- `06-research/<direction>/ai-context.md`
- `06-research/<direction>/source-log.md`
- `06-research/<direction>/working-notes.md`
- `06-research/<direction>/conclusion.md`
- `06-research/<direction>/review.md`

研究流的输出目标：

- 把问题定义清楚
- 把材料来源记清楚
- 把证据链写清楚
- 把结论、限制、下一步说明白

## 给人看 vs 给 AI 看

### 给人看

优先帮助你快速恢复学习状态。

应该重点回答：

- 我为什么学这个？
- 我现在进展到哪了？
- 今天/这周最值得做什么？
- 下次从哪里继续？
- 现在形成了什么结论？

### 给 AI 看

优先帮助 AI 快速接手整理和推进。

应该重点回答：

- 当前阶段是什么？
- 缺失的知识或证据是什么？
- 还需要提炼哪些日志？
- 下一步最该更新哪些文件？

## 推荐使用方式

创建主题：

```powershell
.\scripts\new-topic.ps1 -TopicName "TypeScript"
```

创建今日日志：

```powershell
.\scripts\new-daily-note.ps1
```

创建研究方向：

```powershell
.\scripts\new-research.ps1 -DirectionName "agent-memory-systems"
```

## 以后你可以直接这样对 AI 说

- “帮我创建一个 Redis 学习主题，并做完整大纲。”
- “帮我解这道题，写清楚公式、来源和知识点，并归档到对应主题。”
- “我有个关于 AI agent memory 的设想，帮我搜材料、研究结论并写复盘。”
- “先更新给人看的页面，再更新给 AI 的状态页。”

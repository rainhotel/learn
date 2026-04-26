# Project Design

## Project Goal

把这个仓库设计成一个长期可持续的学习操作系统，让 AI 不只是回答问题，而是帮助你：

1. 系统学习一个领域
2. 对题目做标准化解答和归档
3. 围绕设想或灵感开展研究
4. 跟踪学习进度与研究进度
5. 定期复盘并形成下一步行动

## System Model

这个项目由三层组成：

### 1. Inputs

你给 AI 的输入主要有三类：

- 一个学习主题
- 一道题或一组题
- 一个想法、设想、研究方向

### 2. Processing

AI 处理时应分别走三条工作流：

- 主题学习流
- 标准解题流
- 开放研究流

### 3. Outputs

所有输出都要沉淀为可复用资产：

- 知识点
- 公式与方法
- 题目与解答
- 材料与来源
- 研究结论
- 学习进度
- 下一步动作

## Workflow A: Topic Mastery

适合系统学习一个领域。

### Input

- 用户给出主题，例如“线性代数”“操作系统”“Redis”。

### AI Actions

1. 创建主题目录。
2. 建立完整大纲。
3. 写人类导学页。
4. 写 AI 状态页。
5. 初始化题目、公式、进度文件。
6. 在后续学习中持续整理日志。

### Output Files

- `README.md`
- `outline.md`
- `human-guide.md`
- `ai-context.md`
- `notes.md`
- `qa.md`
- `projects.md`
- `solved-problems.md`
- `formula-sheet.md`
- `progress.md`
- `review.md`

## Workflow B: Problem Solving

适合单题或题组学习。

### Input

- 用户给出题目。

### AI Actions

1. 先给最终答案。
2. 展开完整解题步骤。
3. 标明公式、定理、方法。
4. 解释为什么能用，以及适用条件。
5. 标明答案来源或参考来源。
6. 归档知识点、公式和错误模式。

### Output Files

- `solved-problems.md`
- `formula-sheet.md`
- `qa.md`
- `progress.md`

## Workflow C: Research Exploration

适合探索一个方向。

### Input

- 用户给出想法、设想、灵感、研究方向。

### AI Actions

1. 重述研究问题。
2. 确定范围和边界。
3. 搜集材料并记录来源。
4. 区分事实、解释、推测。
5. 形成阶段结论。
6. 记录限制、风险、下一步。
7. 输出复盘。

### Output Files

- `README.md`
- `human-brief.md`
- `ai-context.md`
- `source-log.md`
- `working-notes.md`
- `conclusion.md`
- `review.md`

## Recording Strategy

### Human-Facing Layer

这是最重要的一层。它负责让你回来时不用重新建立上下文。

应该优先维护：

- 主题的 `human-guide.md`
- 研究的 `human-brief.md`
- 主题或研究的 `README.md`
- `progress.md`
- 周复盘

### AI-Facing Layer

这是让 AI 快速继续工作的结构化状态层。

应该优先维护：

- `ai-context.md`
- `source-log.md`
- 日志中的提炼线索
- 研究证据链

## Progress Model

进度不是看读了多少，而是看形成了什么能力证据。

### 主题进度证据

- 能解释
- 能做题
- 能举例
- 能完成项目
- 能输出总结

### 研究进度证据

- 问题被定义清楚
- 材料被收集和分类
- 证据链形成
- 结论与限制明确
- 是否值得继续有判断

## Why This Design Works

1. 每种输入都有固定落点，AI 不会乱写。
2. 人类视图和 AI 视图分离，既方便你回看，也方便 AI 接手。
3. 题目与研究不再混在普通笔记里，后续检索和复盘都更清晰。
4. 所有活动最终都能沉淀成长期资产，而不只是一次对话结果。

## Default Execution Order

1. 先确定任务属于哪条工作流。
2. 先更新给人看的页面。
3. 再更新给 AI 的状态页。
4. 再把知识点、进度、来源归档。
5. 最后输出下一步建议。

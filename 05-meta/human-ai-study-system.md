# Human-AI Study System

## Goal

把这个仓库从“能记录学习”升级成“能长期高效运行的人机协同学习系统”。

目标不是存更多笔记，而是做到：

- 人下次打开时能在 1 分钟内恢复上下文
- AI 能快速判断本次该更新哪些文件
- 多个 subagent 可以并行工作，但不会把状态写乱
- 每次学习都有明确产出，而不是只消费内容

## Core Principles

1. 先保证人能快速重新开始，再保证 AI 能继续接手。
2. 先维护少数热文件，再按需扩展冷文件。
3. 子代理负责分析和候选内容，主代理负责状态判断和正式落盘。
4. 一次 session 只推进一个子能力，不混多个目标。
5. 进度按能力证据衡量，不按阅读页数衡量。

## Cadence

### Session Arc: 30 to 90 minutes

每次学习只做一件事：

- 学一个概念
- 做一组同类题
- 完成一个小实验

固定流程：

1. 打开 `human-guide.md`
2. 查看 `progress.md`
3. 只定义一个 session 目标
4. 完成学习或练习
5. 提炼稳定知识和薄弱点
6. 写清楚下次第一步

单次 session 的最低产出：

- 1 个稳定知识点
- 1 个薄弱点或误区
- 1 个下次入口

### Weekly Arc

每周固定分成三类动作：

1. 学习日
2. 训练日
3. 复盘日

每周最低产出：

- 1 个新增稳定笔记块
- 1 次薄弱点归纳
- 1 个下周重点
- 如果是题目型主题，新增 3 到 10 道归档题

### 8-Week Arc

一个 8 周周期只服务一个中等清晰目标，例如：

- 完成某主题第一轮系统学习
- 从基础到能做最小项目
- 完成一轮考试定向训练

推荐节奏：

1. 第 1 周：定位领域边界和总图
2. 第 2 到 3 周：基础概念和原理
3. 第 4 到 5 周：核心机制和模式
4. 第 6 周：刻意练习和修错
5. 第 7 周：小项目或综合输出
6. 第 8 周：复盘、补洞、重写导学页

### Long Arc

长期只保留 1 到 3 个活跃主题。

每个活跃主题同一时间只允许一个当前阶段目标。

新主题进入前，优先暂停、归档或降级旧主题，避免无限并行。

## File Model

### Hot Files

活跃主题默认只强制维护以下热文件：

- `README.md`
  - 稳定概览：目标、范围、资源、当前阶段、下一里程碑
- `human-guide.md`
  - 人类快速重启入口：现在在哪、本周做什么、下次从哪开始
- `progress.md`
  - 连续进度：阶段、证据、缺口、下一步
- `notes.md`
  - 稳定知识：概念、机制、对照表、总结

### Cold Files

这些文件按需启用，不要求每次学习都更新：

- `ai-context.md`
- `qa.md`
- `solved-problems.md`
- `formula-sheet.md`
- `projects.md`
- `review.md`

### Structured State

建议每个主题增加结构化状态文件，例如 `topic.yaml`，用于机器汇总。

建议字段：

```yaml
title: Redis
slug: redis
status: active
stage: foundations
priority: high
last_updated: 2026-03-24
current_focus: data structures and persistence
next_action: summarize persistence trade-offs
tags:
  - backend
  - database
```

## Update Order

每次学习结束后，统一按这个顺序落盘：

1. `human-guide.md`
2. `progress.md`
3. `notes.md`
4. `qa.md` / `solved-problems.md` / `projects.md`
5. `ai-context.md`
6. `02-journal/YYYY/MM/YYYY-MM-DD.md`

说明：

- `human-guide.md` 先写，是为了让人下次能立即恢复状态
- `progress.md` 是主题状态真相源之一
- `notes.md` 只收稳定知识，不收半成品
- `journal` 保留过程细节和噪声，不作为长期真相源

## Truth Sources

每个主题的状态真相源固定为：

- `README.md`
- `human-guide.md`
- `progress.md`
- `ai-context.md`

其中：

- 对人最重要的是 `human-guide.md`
- 对系统状态最重要的是 `progress.md`
- 对 AI 接手最重要的是 `ai-context.md`

不要让多个文件同时维护同一层含义的“当前状态”。

## Multi-Agent Protocol

### General Rule

采用 `1 主代理 + 3 子代理` 的最小协作模型。

- 主代理负责判断任务类型、分派工作、去重冲突、正式写入
- 子代理负责读取资料、提出候选内容、输出结构化结果

硬规则：

- 子代理默认只读正式文件
- 子代理默认不直接改主题状态文件
- 正式状态文件只允许主代理写

### Good Parallel Tasks

适合并行的任务：

- 大纲拆解
- 资料扫描
- 日志提炼
- 题目预处理
- 研究资料对读
- 周复盘候选结论生成

### Serial Tasks

必须串行的任务：

- 创建主题或研究目录
- 更新 `README.md`
- 更新 `human-guide.md`
- 更新 `progress.md`
- 更新 `ai-context.md`
- 更新全局看板
- 正式把 journal 提炼进主题文件

### Agent Roles

#### Planner Agent

读取：

- `README.md`
- `outline.md`
- `progress.md`
- `human-guide.md`

输出：

- 阶段计划
- 下周 session 拆分
- 8 周路线建议

#### Extractor Agent

读取：

- `02-journal/**/*.md`
- `ai-context.md`

输出：

- 稳定知识候选
- 未解决问题
- 下一步行动

#### Practice Agent

读取：

- `notes.md`
- `qa.md`
- `solved-problems.md`
- `formula-sheet.md`

输出：

- 练习题
- 解题步骤草案
- 方法映射
- 易错点

#### Research Agent

读取：

- `06-research/<direction>/README.md`
- `source-log.md`
- `working-notes.md`

输出：

- 证据摘要
- 分歧点
- 限制条件
- 暂定结论

## Standard Agent Templates

### Topic Session

- 主代理：定义本次 session 目标并最终写入
- 子代理 1：Explain
- 子代理 2：Drill
- 子代理 3：Review

### Problem Solving

- 主代理：定主题、归档、更新进度
- 子代理 1：Solver
- 子代理 2：Method Mapper
- 子代理 3：Weakness Finder

### Research Exploration

- 主代理：定义研究问题和边界
- 子代理 1：Source Scout
- 子代理 2：Thesis Builder
- 子代理 3：Skeptic

## Execution Standard

判断一次学习是否有效，不看学习时长，优先看以下证据：

- 能否解释一个概念
- 能否做对一类题
- 能否指出一个误区
- 能否产出一个最小成果
- 能否清楚写出下次入口

## Recommended Next Changes

1. 给主题模板加入 `topic.yaml`
2. 把 `05-meta/current-focus.md` 变成只维护活跃主题的控制面
3. 追加一个 `subagent-playbook.md`，写清楚每类任务的调用模板
4. 后续增加脚本，从 `topic.yaml` 自动汇总 `progress-dashboard.md`

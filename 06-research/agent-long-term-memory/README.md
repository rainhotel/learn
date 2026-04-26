# Agent Long-Term Memory

## Research Goal

- 研究 AI 长期记忆、智能体长期记忆、遗忘曲线这三条线在 2023-2026 年的主流演化。
- 搞清楚哪些方法已经工程化，哪些还主要停留在论文阶段。
- 为后续自己设计 agent memory stack 提供可执行的架构参考。

## Scope

- 包含：LLM/Agent 长期记忆、对话长期记忆、记忆组织与检索、时间感知记忆、遗忘/强化/复习调度、主流开源项目与论文。
- 不包含：纯长上下文扩窗、纯基础 RAG、和长期记忆无关的通用 agent 框架。

## Research Questions

- 核心问题 1：目前主流系统到底把“长期记忆”拆成了哪些层？
- 核心问题 2：遗忘曲线在 agent memory 里是直接删除策略，还是更像打分/调度策略？
- 核心问题 3：从论文到工程，哪些设计已经收敛，哪些仍然分歧很大？

## Current Status

- 阶段：First-pass landscape survey
- 最近一次更新：2026-03-30

## Expected Output

- 一份可用于选型的项目/论文地图
- 一套面向工程实现的 memory architecture 建议
- 一份关于“是否要把遗忘曲线纳入 agent memory”的当前判断

## Human And AI Views

- 给人看：`human-brief.md`
- 给 AI 看：`ai-context.md`
- 材料来源：`source-log.md`
- 过程整理：`working-notes.md`
- 研究结论：`conclusion.md`

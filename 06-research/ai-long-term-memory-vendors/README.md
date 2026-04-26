# AI 厂商长期记忆实现调研

## Research Goal

- 研究主流 AI 厂商如何在产品层实现“长期记忆”。
- 重点回答 `Codex` 与 `Claude Code` 的长期记忆到底怎么做、做到了哪一层、适合什么场景。

## Scope

- 包含：
  - OpenAI、Anthropic、Google、Microsoft 已公开的产品文档与帮助中心说明
  - 与长期记忆机制直接相关的代表性论文
  - 产品能力、存储介质、控制方式、上下文续航方式的对比
- 不包含：
  - 未公开的服务端内部实现细节
  - 纯学术记忆架构的完整综述
  - 第三方非主流产品的全面盘点

## Research Questions

- 核心问题 1：主流 AI 厂商把“长期记忆”落在了哪些产品层？
- 核心问题 2：`Codex` 与 `Claude Code` 的长期记忆实现差异是什么？
- 核心问题 3：当前商业产品的实现，和论文中的长期记忆路线分别对应什么技术范式？

## Current Status

- 阶段：Synthesis
- 最近一次更新：2026-03-30

## Expected Output

- 形成一个可执行判断：
  - 如果目标是“持续记住用户偏好”，该看哪类产品
  - 如果目标是“持续记住代码库规则与工作流”，`Codex` 和 `Claude Code` 分别更像什么
  - 如果要自己做长期记忆系统，优先借鉴哪些实现模式

## Human And AI Views

- 给人看：`human-brief.md`
- 给 AI 看：`ai-context.md`
- 材料来源：`source-log.md`
- 研究结论：`conclusion.md`

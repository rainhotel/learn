# ClaudeCode 源码学习

## Goal

- 读懂 ClaudeCode 在 SDK / headless 模式下是怎么组织一次对话回合的。
- 能顺着一条 `prompt -> query -> transcript -> result` 链路追踪消息、权限、成本和状态。
- 后续读到 `query.ts`、tools、session storage 时，不会失去全局定位。

## Scope

- 当前聚焦 `QueryEngine` 这一层的调度代码。
- 重点覆盖 `QueryEngine` 类、`submitMessage()` 主流程、`ask()` 包装函数。
- 当前先不深入 `query()` 内部推理循环、具体 tool 实现、MCP 细节。

## Outcome

- 能直接回答“这个文件到底负责什么”。
- 能把 `submitMessage()` 切成若干稳定阶段，而不是被长函数吓住。
- 能指出这份代码里最关键的几个工程性设计：早落 transcript、双消息数组、流式 usage 聚合、compact boundary 裁剪。
- 能带着问题继续往 `query.ts` 和 message model 深读。

## Status

- 阶段：Phase 2 - QueryEngine 主流程拆解
- 优先级：High
- 最近一次更新：2026-04-02
- 当前学习模式：Mastery roadmap

## Core Resources

- 资源 1：本主题 `notes.md`
- 资源 2：本主题 `qa.md`
- 资源 3：本次精读的 `QueryEngine` / `ask` 源码

## Next 3 Actions

1. 接着读 `query()` 的消息产出路径，补上这份文件调用出去之后发生了什么。
2. 梳理 `Message` 联合类型和 `normalizeMessage()`，看内部消息是怎么映射成 SDK 消息的。
3. 复盘 transcript、compact、resume 这条持久化链路，建立“为什么这么写”的工程直觉。

## Human And AI Views

- 给人看：`human-guide.md`
- 给 AI 看：`ai-context.md`
- 解题归档：`solved-problems.md`
- 进度跟踪：`progress.md`

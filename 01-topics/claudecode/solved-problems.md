# ClaudeCode 源码学习 Solved Problems

## Problem 1

### Problem Title

- Date: 2026-04-02
- Source: 用户提供的 `QueryEngine` / `ask` 源码
- Topic: ClaudeCode SDK orchestration
- Difficulty: Medium

#### Problem

- 题目原文：`submitMessage()` 这么长，到底应该怎么读，结论是什么？

#### Final Answer

- 最终答案：把它视为“一个 turn 的编排器”，按 10 个阶段读，而不是逐行硬啃。核心结论是：`QueryEngine` 负责会话状态，`submitMessage()` 负责 turn 调度，`ask()` 只是一次性包装。

#### Solution

1. 先确认 `QueryEngine` 是状态对象而非纯函数，抓住 `mutableMessages`、`totalUsage`、`readFileState` 这些字段。
2. 再把 `submitMessage()` 切成 setup、input processing、transcript、query loop、result assembly 几大块。
3. 最后重点读长注释，理解恢复性设计、streaming stop_reason、compact boundary、fire-and-forget transcript 的原因。

#### Formula Or Method Used

- 公式/定理/方法：四主线阅读法
- 含义：输入、会话、流处理、收尾四条线并行看
- 适用条件：阅读 agent orchestration 代码时

#### Sources

- 参考来源 1：用户提供的源码
- 参考来源 2：本主题 `notes.md`

#### Knowledge Points

- 这道题对应的知识点：async generator、事件分发、状态持久化、恢复性设计、usage 聚合
- 这道题暴露出的薄弱点：对 `query()` 上游消息来源还不够清楚

#### Reflection

- 这次为什么会做对/做错：做对在于先抓职责边界，没陷进逐行细节；做错在于对子模块调用链仍不完整。
- 以后怎么更快识别这类题：先找“状态对象”“主循环”“最终收尾”三个锚点。

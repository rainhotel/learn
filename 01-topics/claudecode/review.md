# ClaudeCode 源码学习 Review

## What I Understand Well

- 这份文件的真实职责是 orchestration，不是模型推理。
- `submitMessage()` 可以稳定拆成若干阶段，而不是一团逻辑。
- 代码中大量长注释在解释恢复性设计和 streaming 边界问题。

## What Still Feels Fuzzy

- `query()` 具体怎么产出内部消息
- compact boundary 的完整回放与裁剪链路
- `normalizeMessage()` 与 SDK 消息映射细节

## What Changed In My Understanding

- 一开始会觉得这是个“过长函数”，现在更像是在看一个集中表达状态转移的调度器。
- 一开始容易把持久化逻辑当边角料，现在知道 transcript / resume 才是这类 agent 系统的生命线。

## Evidence

- 我能独立解释什么：`QueryEngine`、`submitMessage()`、`ask()` 的职责分工
- 我能独立做什么：能画出 `submitMessage()` 的阶段图，能指出几个最关键的工程设计点

## Next Improvement

- 下一轮只盯 `query()` 的产出，不被其它模块分散注意力。

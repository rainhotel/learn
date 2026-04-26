# ClaudeCode 源码学习 Human Guide

## Start Here

- 这个主题现在处于“先抓住调度骨架，再继续深挖子模块”的阶段。
- 今天如果只有 30 到 60 分钟，最值得先看 `notes.md` 里的“直接结论”和“`submitMessage()` 主流程”。
- 下次打开时先看这 3 个部分：
  - `notes.md`
  - `qa.md`
  - `progress.md`

## Why This Matters

- 它解决的是“看大型 agent 代码时，怎么先抓住控制流，再理解各模块协作”的问题。
- 它和你已有的 Node / TypeScript / async iterator / 工程架构理解直接相连。
- 学会后你能更稳定地看懂 ClaudeCode、Codex 这类 agent 框架的会话主循环，也更容易自己设计类似结构。

## Mastery Path

1. 地图与词汇
2. 输入与上下文准备
3. 主循环与消息流
4. 持久化与恢复
5. 权限、预算与安全边界
6. 子模块联动
7. 输出讲解
8. 架构比较与审美

## Current Stage

- 当前阶段：已经完成 `QueryEngine` 文件的第一轮拆解。
- 已完成：明确了 `QueryEngine`、`submitMessage()`、`ask()` 的职责边界。
- 卡点：`query()`、`Message` 类型、compact / transcript 相关辅助函数还没连起来。
- 判断自己进入下一阶段的标准：能够不看源码，口述一次 turn 的主要阶段和关键状态。

## This Week

- 本周重点：把 ClaudeCode 的一次 turn 走通。
- 本周最小胜利：已经把一个超长函数拆成了稳定的阶段图。
- 本周必须完成的一个输出：补一份从 `ask()` 到 `query()` 再到 `result` 的调用链图。

## Resume Fast

- 下次开始先读：`notes.md` 的“关键设计点”。
- 下次开始先做：去读 `query()`，只盯“会 yield 什么消息类型”这一件事。
- 下次别再重复踩的坑：不要一上来逐行读长函数，先按职责切块，再看块内细节。

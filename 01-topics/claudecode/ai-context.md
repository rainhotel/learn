# ClaudeCode 源码学习 AI Context

## Topic State

- Current phase: QueryEngine orchestration pass 1
- Confidence estimate: 0.77
- Last updated: 2026-04-02

## Dependency Map

- 先修知识：TypeScript 类型系统、async generator、事件流、状态机、持久化与恢复、权限控制
- 当前核心概念：会话级状态、turn 级调度、消息归一化、transcript 持久化、usage / budget 聚合、compact boundary
- 后续高级主题：`query()` 内部主循环、tool 调用协议、message 类型体系、session resume、snip compaction

## Knowledge Gaps

- 还没覆盖的基础：`Message` 各 subtype 的完整来源与去向
- 还没打通的机制：`query()` 如何生成 assistant / progress / attachment / system 消息
- 只会用但不会解释的点：`snipReplay`、`compact boundary` 与 `preservedSegment` 的全链路实现

## Extraction Backlog

- 哪些 journal 内容应该整理进 `notes.md`：今天对 `submitMessage()`、`ask()` 的结论性拆解
- 哪些问题应该提升到 `qa.md`：为什么要双写 transcript、为什么 context 要重建两次、为什么 stop_reason 要从 stream event 拿
- 哪些实验应该记到 `projects.md`：手动画一次 turn 时序图，追一次 compact / resume 链路

## Source Map

- 当前最值得参考的资料：本次用户提供的 `QueryEngine` / `ask` 源码
- 哪些资料只是扫过：模板文件、仓库现有学习记录
- 哪些资料需要二次验证：`queryHelpers.ts`、`query.ts`、`sessionStorage.ts`、`messages/mappers.ts`

## Next Best Edits

1. 给 `notes.md` 增补一张 `submitMessage()` 阶段图。
2. 读取 `query.ts`，补上这个文件外发消息的上游来源。
3. 将 compact / resume 设计抽成单独一节，形成稳定工程模式。

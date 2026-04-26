# ClaudeCode 源码学习 Progress

## Snapshot

- Current phase: Phase 2 - QueryEngine 主流程拆解
- Current level: 能解释主要控制流，但尚未打通 query / message / compact 子模块
- Last updated: 2026-04-02

## Milestones

- [x] 完成领域地图
- [x] 完成基础原理
- [ ] 完成核心机制
- [ ] 完成 10 道代表题
- [ ] 完成 1 个小项目
- [x] 输出 1 篇总结

## Evidence Of Progress

- 最近做对了什么：把长达数百行的 `submitMessage()` 拆成稳定的 10 个阶段
- 最近真正理解了什么：这份代码的重点不是模型推理，而是 turn orchestration
- 最近能独立解释什么：`QueryEngine`、`submitMessage()`、`ask()` 的职责边界

## Weak Spots

- 当前最薄弱的 3 个点：`query()` 内部主循环、compact/resume 全链路、Message 类型体系
- 最近最常见的错误模式：先看局部实现，后补整体地图，顺序反了

## Next Milestone

- 下一个里程碑：打通 `query()` 到 SDK result 的完整消息流
- 达成标准：能解释每种 `message.type` 的来源和去向
- 最短路径：读 `query.ts`、`normalizeMessage()`、`isResultSuccessful()`

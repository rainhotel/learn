# ClaudeCode 源码学习 Projects

## Practice Ideas

- 小练习 1：手动画一张 `ask() -> QueryEngine.submitMessage() -> query() -> result` 的时序图
- 小练习 2：手写一个最小版 `QueryEngine`，只保留消息累积、usage 统计和 success result

## Experiments

### Experiment 1: 拆出一次 turn 的阶段图

- 目标：不看源码，复述 `submitMessage()` 的主流程
- 步骤：
  1. 只看 `notes.md` 里的 10 个阶段
  2. 口述一遍每个阶段做什么
  3. 回头对照源码查漏
- 结果：待完成
- 结论：待完成

### Experiment 2: 追 compact / resume 设计

- 目标：搞清楚 transcript 与 compact boundary 的配合原因
- 步骤：
  1. 读 `recordTranscript()`
  2. 读 `flushSessionStorage()`
  3. 读 `snipCompactIfNeeded()` 和 `projectSnippedView` 相关实现
- 结果：待完成
- 结论：待完成

## Real Outputs

- 已完成的项目：完成 `QueryEngine` 第一轮精读笔记
- 下一步可做的项目：补完整个 SDK 模式的一次 turn 调用链图

# Meet in the Middle

## Goal

- 理解什么时候应该把一个高维枚举问题拆成两半来做。
- 能把 `O(n^4)` 级别的暴力枚举改写成 `O(n^2 log n)` 或 `O(n^2)`。
- 能区分通用折半枚举、哈希计数、值域压缩三种常见落地方式。

## Scope

- 包含：四数和计数、两数组和配对、pair sum 计数、排序二分、哈希统计。
- 暂不包含：子集折半搜索、状态压缩搜索型 meet-in-the-middle。

## Outcome

- 能识别“拆成两半再合并”的题型信号。
- 能独立写出 pair sum 计数代码，并说明复杂度。
- 能解释为什么本题可以进一步利用值域小的性质做常数优化。

## Status

- 阶段：Foundations
- 优先级：High
- 最近一次更新：2026-06-06
- 当前学习模式：Mastery roadmap

## Core Resources

- 资源 1：本主题 `notes.md`
- 资源 2：本主题 `formula-sheet.md`
- 资源 3：本主题 `solved-problems.md`

## Next 3 Actions

1. 吃透“四数组求和计数”这一类题为什么要先看 pair sum。
2. 再补一题用 `unordered_map` 或排序二分实现的通用版本。
3. 对比值域压缩和通用写法的适用边界。

## Human And AI Views

- 给人看：`human-guide.md`
- 给 AI 看：`ai-context.md`
- 解题归档：`solved-problems.md`
- 进度跟踪：`progress.md`


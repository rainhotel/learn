# Meet in the Middle AI Context

## Topic State

- Current phase: Foundations
- Confidence estimate: Medium
- Last updated: 2026-06-06

## Dependency Map

- 先修知识：基础枚举、时间复杂度、数组/哈希表、排序与二分
- 当前核心概念：两两配对、pair sum 统计、互补和、值域压缩
- 后续高级主题：子集折半、搜索剪枝、k-sum 泛化

## Knowledge Gaps

- 还没覆盖的基础：排序二分版与 `unordered_map` 版的统一比较
- 还没打通的机制：通用大值域场景下的写法迁移
- 只会用但不会解释的点：为什么互补和计数不会重不漏

## Extraction Backlog

- 哪些 journal 内容应该整理进 `notes.md`：2026-06-06 的四数组求和计数总结
- 哪些问题应该提升到 `qa.md`：何时该用值域压缩，何时必须回退到哈希/排序
- 哪些实验应该记到 `projects.md`：后续可加一个 pair sum 模板库小练习

## Source Map

- 当前最值得参考的资料：本主题 `solved-problems.md` 和 `formula-sheet.md`
- 哪些资料只是扫过：暂无外部资料
- 哪些资料需要二次验证：暂无

## Next Best Edits

1. 增补一题使用排序二分的通用 4Sum Count。
2. 在 `notes.md` 补上“通用写法 vs 小值域优化”的对照表。
3. 在 `projects.md` 设计一个 pair sum 统计模板练习。


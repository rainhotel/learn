# Algorithm Final Review AI Context

## Current Stage

- 当前阶段：依据用户提供的期末考点图，完成第一版冲刺复习整理。
- 目标不是建立完整算法知识树，而是产出考前可直接使用的板子和相似题单。

## Source Of Truth

- 核心依据：用户提供的老师手写考点照片。
- 已识别考点：
  - `DFS/BFS`，强调 `visited`
  - 二分查找，强调 `STL lower_bound / upper_bound`
  - 贪心
  - 动态规划：最大子段和、最长不降子序列、背包、最长公共子序列
  - 图论：`Floyd`、最小生成树
- 用户 2026-06-30 追加要求：
  - 汇总 `STL` 常用语法、`pair/make_pair`
  - `DFS/BFS` 需要非 `vector` 板子
  - 增加贪心策略表、常见 DP 状态转移、`0/1` 背包、完全背包
  - 增加 `Dijkstra`、`Prim`
  - 增加 `lower_bound/upper_bound`、普通二分、二分答案

## Constraints

- 用户明确要求 `C++98` 标准。
- 因此不默认使用 `bits/stdc++.h`、范围 `for`、`auto`、`unordered_map`、`lambda` 等写法。
- 板子优先选“短、稳、好默写”的版本，而不是最现代或最极致优化的版本。
- 目前主要速查文件是 `cpp98-exam-template-sheet.md`。

## Gaps

- 还未把新增板子逐题配套到 `solved-problems.md`。
- `Dijkstra` 和 `Prim` 采用邻接矩阵版，优先可读性；若题目数据很大，再补链式前向星 + 堆优化版本。

## Next Extraction Targets

- 如果后续继续做题，应把每一类至少补 1 道真实做过的题到 `solved-problems.md`。
- 如果用户开始刷题，应把错题沉淀到 `qa.md`。

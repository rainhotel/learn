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

## Constraints

- 用户明确要求 `C++98` 标准。
- 因此不默认使用 `bits/stdc++.h`、范围 `for`、`auto`、`unordered_map`、`lambda` 等写法。
- 板子优先选“短、稳、好默写”的版本，而不是最现代或最极致优化的版本。

## Gaps

- 图片最下方还有一条零散手写备注，语义不够清晰，暂未纳入主复习范围。
- 贪心题没有被老师细分，需要用最常见的“排序 + 局部最优”题型来承接。

## Next Extraction Targets

- 如果后续继续做题，应把每一类至少补 1 道真实做过的题到 `solved-problems.md`。
- 如果用户开始刷题，应把错题沉淀到 `qa.md`。

# Algorithm Final Review Solved Problems

## Representative Problem Set (2026-06-17)

### 1. 网格连通块计数

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: `DFS`
- Difficulty: Easy

#### Problem

- 给一个由 `.` 和 `#` 组成的网格，`#` 不可走，问一共有多少个由 `.` 组成的连通块。

#### Solution

1. 双重循环枚举每个格子。
2. 遇到尚未访问过的 `.`，答案加 `1`。
3. 从这个点出发做一次 `DFS`，把整片区域都标记掉。

#### Formula Or Method Used

- 方法：`DFS + visited`
- 适用条件：只需要统计块数，不要求最短路
- 对应板子：`formula-sheet.md` 的网格 `DFS`

### 2. 迷宫最短步数

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: `BFS`
- Difficulty: Easy

#### Problem

- 在四联通迷宫中，从 `S` 走到 `T`，每次上下左右走一步，问最少步数。

#### Solution

1. 起点入队并把距离设为 `0`。
2. 每次从队头扩展四个方向。
3. 第一次到达终点时的距离就是最短路。

#### Formula Or Method Used

- 方法：无权图 `BFS`
- 适用条件：每条边代价相同
- 对应板子：`formula-sheet.md` 的网格 `BFS`

### 3. 有序数组统计某值出现次数

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: 二分查找
- Difficulty: Easy

#### Problem

- 给一个非降序数组和一个值 `x`，求 `x` 出现了多少次。

#### Solution

1. 用 `lower_bound` 找到第一个 `>= x` 的位置。
2. 用 `upper_bound` 找到第一个 `> x` 的位置。
3. 两者相减就是出现次数。

#### Formula Or Method Used

- 方法：二分边界 / `STL`
- 适用条件：数组有序
- 对应板子：`formula-sheet.md` 的二分模板

### 4. 最大不重叠区间数

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: 贪心
- Difficulty: Medium

#### Problem

- 给出若干区间，求最多能选多少个互不重叠的区间。

#### Solution

1. 按区间右端点从小到大排序。
2. 每次优先选择最早结束且与当前方案不冲突的区间。
3. 这个策略能为后面留下最多空间。

#### Formula Or Method Used

- 方法：排序 + 贪心选择
- 适用条件：区间选择类最优化
- 对应板子：`formula-sheet.md` 的活动选择模板

### 5. 最大子段和

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: 动态规划
- Difficulty: Easy

#### Problem

- 给一个整数序列，求和最大的连续子段。

#### Solution

1. 设 `dp[i]` 为以 `a[i]` 结尾的最大子段和。
2. 如果前面的和是负贡献，就从当前元素重新开始。
3. 枚举时同步维护全局最大值。

#### Formula Or Method Used

- 方法：线性 DP
- 适用条件：连续子段最值
- 对应板子：`formula-sheet.md` 的最大子段和模板

### 6. 最长不降子序列

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: 动态规划
- Difficulty: Medium

#### Problem

- 给一个序列，求最长不降子序列长度。

#### Solution

1. 设 `dp[i]` 为以 `a[i]` 结尾的最长不降子序列长度。
2. 枚举所有 `j < i`，若 `a[j] <= a[i]`，则可转移。
3. 所有 `dp[i]` 的最大值即答案。

#### Formula Or Method Used

- 方法：`O(n^2)` 序列 DP
- 适用条件：考试基础题
- 对应板子：`formula-sheet.md` 的最长不降子序列模板

### 7. `0/1` 背包

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: 动态规划
- Difficulty: Medium

#### Problem

- 有 `n` 件物品，每件物品有重量和价值，背包容量为 `m`，求最大总价值。

#### Solution

1. 设 `dp[j]` 为容量恰为 `j` 时的最大价值。
2. 枚举物品，再倒序枚举容量。
3. 每件物品只会被用一次。

#### Formula Or Method Used

- 方法：一维 `0/1` 背包
- 适用条件：每件物品最多选一次
- 对应板子：`formula-sheet.md` 的 `0/1` 背包模板

### 8. 最长公共子序列

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: 动态规划
- Difficulty: Medium

#### Problem

- 给两个字符串，求它们的最长公共子序列长度。

#### Solution

1. 设 `dp[i][j]` 表示前缀问题的答案。
2. 当前字符相等时取左上角加 `1`。
3. 不等时取上方和左方的较大值。

#### Formula Or Method Used

- 方法：二维 DP
- 适用条件：两个序列的公共结构比较
- 对应板子：`formula-sheet.md` 的 `LCS` 模板

### 9. 多源最短路查询

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: 图论
- Difficulty: Medium

#### Problem

- 给一个带权无向图和多次询问，每次问两点最短路长度。

#### Solution

1. 用邻接矩阵存初始边权。
2. 初始化不可达为 `INF`，自己到自己为 `0`。
3. 跑 `Floyd` 后即可回答任意两点最短路。

#### Formula Or Method Used

- 方法：`Floyd`
- 适用条件：点数不大，询问多
- 对应板子：`formula-sheet.md` 的 `Floyd` 模板

### 10. 最小生成树

- Date: 2026-06-17
- Source: 期末复习代表题
- Topic: 图论
- Difficulty: Medium

#### Problem

- 给一个无向连通图，求最小生成树的总权值。

#### Solution

1. 先把所有边按权值从小到大排序。
2. 依次尝试加入当前边。
3. 如果这条边连接的是两个不同连通块，就选它。

#### Formula Or Method Used

- 方法：`Kruskal` + 并查集
- 适用条件：无向图最小生成树
- 对应板子：`formula-sheet.md` 的 `Kruskal` 模板

# Dynamic Programming Solved Problems

## Problem Set 1 (2026-03-29)

### P1796 马走棋

- Date: 2026-03-29
- Source: 洛谷 P1796 / 用户提供题面
- Topic: 网格计数 DP
- Difficulty: Easy

#### Problem

- 在棋盘上，马从 `(1,1)` 出发，只能走到 `(x+2,y+1)` 或 `(x+1,y+2)`，求走到终点的路径条数。

#### Final Answer

- 设 `dp[i][j]` 表示从 `(1,1)` 走到 `(i,j)` 的可行路径数。
- 转移方程：`dp[i][j] = dp[i - 1][j - 2] + dp[i - 2][j - 1]`
- 初始条件：`dp[1][1] = 1`
- 最终答案：`dp[n][m]`

#### Solution

1. 这题是典型计数 DP，先定义 `dp[i][j]` 为到达当前格子的方案数。
2. 到 `(i,j)` 的最后一步只可能来自 `(i-1,j-2)` 或 `(i-2,j-1)`。
3. 因此当前位置方案数等于两个合法前驱状态之和。
4. 起点只有 1 种“什么都不走”的状态，所以 `dp[1][1] = 1`。
5. 按行列从小到大遍历即可，因为所有依赖都在更小坐标处。

#### Code

```cpp
#include <iostream>
using namespace std;

int main() {
    int n, m;
    cin >> n >> m;

    long long dp[16][16] = {};
    dp[1][1] = 1;

    for (int i = 1; i <= n; ++i) {
        for (int j = 1; j <= m; ++j) {
            if (i == 1 && j == 1) {
                continue;
            }
            if (i - 1 >= 1 && j - 2 >= 1) {
                dp[i][j] += dp[i - 1][j - 2];
            }
            if (i - 2 >= 1 && j - 1 >= 1) {
                dp[i][j] += dp[i - 2][j - 1];
            }
        }
    }

    cout << dp[n][m] << '\n';
    return 0;
}
```

#### Formula Or Method Used

- 方法：网格计数 DP
- 含义：当前状态的方案数等于所有合法前驱状态方案数之和
- 适用条件：移动方向单调，且状态只依赖更小规模问题
- 复杂度：时间复杂度 `O(nm)`，空间复杂度 `O(nm)`

#### Sources

- 参考来源 1：题目原文
- 参考来源 2：本主题 `notes.md`

#### Knowledge Points

- 这道题对应的知识点：状态定义、最后一步倒推、二维递推
- 这道题暴露出的薄弱点：容易把前驱位置写反，或者漏掉起点初值

#### Reflection

- 这次为什么会做对/做错：这题结构单纯，关键是先把 `dp[i][j]` 说清楚再写代码
- 以后怎么更快识别这类题：看到“求方案数 + 只能单向推进”就优先尝试计数 DP

#### Extra Insight

- 这题还有一个快速观察：每走一步，坐标和都会增加 `3`，所以如果 `n + m - 2` 不是 `3` 的倍数，答案一定是 `0`。

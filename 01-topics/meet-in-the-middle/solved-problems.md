# Meet in the Middle Solved Problems

## Problem Set 1 (2026-06-06)

### P1748 a+b+c+d==0

- Date: 2026-06-06
- Source: 洛谷 P1748 / 用户提供题面
- Topic: 折半枚举、pair sum 计数
- Difficulty: Easy-Medium

#### Problem

- 给定四个长度都为 `n` 的列表 `A, B, C, D`，统计满足 `a+b+c+d=0` 的四元组个数。
- 不同位置的元素视为不同四元组。

#### Final Answer

- 先统计所有 `a+b` 的和出现多少次，再枚举所有 `c+d`，把答案累加上 `cntAB[-(c+d)]`。
- 由于 `a,b,c,d∈[-200,200]`，所以 pair sum 只会落在 `[-400,400]`，可以直接用长度 `801` 的数组计数。

#### Solution

1. 暴力枚举四个数组需要四重循环，复杂度是 `O(n^4)`，在 `n=2000` 时完全不可行。
2. 把条件 `a+b+c+d=0` 改写成 `(a+b)=-(c+d)`，问题就变成两边 pair sum 的互补匹配。
3. 先用两层循环枚举所有 `(a,b)`，把每个和出现的次数记到 `cntAB` 里。
4. 再用两层循环枚举所有 `(c,d)`，对于当前和 `s=c+d`，能配对的数量就是 `cntAB[-s]`。
5. 把这些数量累加起来，就是最终四元组个数。
6. 因为题目值域很小，`a+b` 和 `c+d` 的范围固定在 `[-400,400]`，所以这里用数组比 `unordered_map` 更稳、更快。

#### Code

```cpp
#include <iostream>
#include <vector>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int T;
    cin >> T;

    const int OFFSET = 400;
    const int RANGE = 801;

    while (T--) {
        int n;
        cin >> n;

        vector<int> A(n), B(n), C(n), D(n);
        for (int i = 0; i < n; ++i) {
            cin >> A[i] >> B[i] >> C[i] >> D[i];
        }

        long long cntAB[RANGE] = {};
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                ++cntAB[A[i] + B[j] + OFFSET];
            }
        }

        long long ans = 0;
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                ans += cntAB[-(C[i] + D[j]) + OFFSET];
            }
        }

        cout << ans << '\n';
    }

    return 0;
}
```

#### Formula Or Method Used

- 公式/定理/方法：`answer = Σ cntAB[s] * cntCD[-s]`
- 含义：把四元组计数降维成两个二元和频次表的匹配计数。
- 适用条件：条件可写成两个部分的互补关系，且我们需要统计的是“数量”而不是仅判断是否存在。
- 复杂度：时间复杂度 `O(n^2)`，空间复杂度 `O(1)`（若把值域视为常数）。

#### Sources

- 参考来源 1：题目原文
- 参考来源 2：本主题 `formula-sheet.md`

#### Knowledge Points

- 这道题对应的知识点：折半枚举、pair sum 计数、值域压缩、组合计数
- 这道题暴露出的薄弱点：容易忘记“相同数值但不同位置”也要分别计数

#### Reflection

- 这次为什么会做对/做错：关键是先把等式拆成两个二元和，而不是纠缠四个变量一起处理。
- 以后怎么更快识别这类题：看到“4 个列表 + 和为某个目标 + 统计数量”时，优先检查能否转成 pair sum 互补匹配。

#### Extra Insight

- 如果题目值域不小，仍然可以保留同样的思路，只是把固定数组换成 `unordered_map` 或“排序后用二分统计”的通用写法。


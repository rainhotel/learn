# C++98 Algorithm Exam Template Sheet

## How To Use

- 这份是考前“板子页”，目标是能直接默写、直接改题。
- 代码统一 `C++98`，不使用 `auto`、范围 `for`、`lambda`、`unordered_map`。
- 搜索板子默认不用 `vector` 建图，使用数组、邻接矩阵或链式前向星。
- 如果时间很紧，优先背：`BFS`、二分、`0/1` 背包、完全背包、`Dijkstra`、`Prim`。

## 1. C++98 STL 常用语法

### 1.1 常用头文件

```cpp
#include <iostream>
#include <cstdio>
#include <cstring>
#include <cmath>
#include <algorithm>
#include <queue>
#include <stack>
#include <vector>
#include <string>
#include <utility>
#include <map>
#include <set>
#include <functional>
using namespace std;
```

### 1.2 数组常用操作

```cpp
int a[1005];
int n;

sort(a, a + n);              // 升序
reverse(a, a + n);           // 翻转
memset(a, 0, sizeof(a));     // 全部置 0
memset(a, -1, sizeof(a));    // 全部置 -1
memset(a, 0x3f, sizeof(a));  // 近似 INF

int x = max(3, 5);
int y = min(3, 5);
swap(a[0], a[1]);

int l = lower_bound(a, a + n, x) - a;
int r = upper_bound(a, a + n, x) - a;

sort(a, a + n);
int newn = unique(a, a + n) - a;
```

注意：

- `memset` 适合设成 `0`、`-1`、`0x3f`。
- 不要用 `memset(dp, 1, sizeof(dp))` 表示全是 `1`，这会按字节填充。
- 常用无穷大：`const int INF = 0x3f3f3f3f;`
- `lower_bound` / `upper_bound` / `binary_search` 都要求序列有序。
- `unique` 只删除相邻重复，真正去重一般先 `sort`。

### 1.3 vector 常用语法

```cpp
vector<int> v;
v.push_back(10);
v.push_back(20);

int len = (int)v.size();
int first = v[0];
int last = v.back();

sort(v.begin(), v.end());
reverse(v.begin(), v.end());
v.pop_back();
v.clear();

string s = "abcde";
int slen = (int)s.size();
string t = s.substr(1, 3);
int pos = (int)s.find("cd");
```

C++98 里嵌套模板要写空格：

```cpp
vector< pair<int, int> > vp;   // 对
// vector<pair<int,int>> vp;   // C++98 里容易编译不过
```

边界提醒：

- `v[i]` 不检查越界，先保证 `0 <= i < v.size()`。
- `front()`、`back()`、`pop_back()` 前要确认 `!v.empty()`。
- `string::find` 找不到时返回 `string::npos`，不要直接当合法下标。

### 1.4 queue / stack / priority_queue

```cpp
queue<int> q;
q.push(1);
q.front();
q.pop();
q.empty();

stack<int> st;
st.push(1);
st.top();
st.pop();
st.empty();

priority_queue<int> big;  // 大根堆，最大值先出
big.push(5);
big.top();
big.pop();
```

小根堆写法：

```cpp
priority_queue<int, vector<int>, greater<int> > small;
```

`greater` 来自 `<functional>`。

`pair` 小根堆写法：

```cpp
priority_queue< pair<int, int>,
                vector< pair<int, int> >,
                greater< pair<int, int> > > pq;
```

### 1.5 map / set

```cpp
map<string, int> mp;
mp["apple"]++;
if (mp.find("apple") != mp.end()) {
    cout << mp["apple"] << endl;
}

set<int> st;
st.insert(3);
st.count(3);

set<int>::iterator it = st.lower_bound(4);
if (it != st.end()) {
    cout << *it << endl;
}
```

边界提醒：

- `mp[key]` 在 key 不存在时会自动创建默认值；只判断存在时用 `find`。
- `set` 自动排序和去重，`count` 只会返回 `0` 或 `1`。
- 迭代器可能等于 `end()`，解引用前必须判断。
- `sort` 比较函数不能写 `<=`，相等时要返回 `false`。

## 2. pair / make_pair 语法

### 2.1 基本用法

```cpp
pair<int, int> p;
p = make_pair(3, 5);

cout << p.first << " " << p.second << endl;
```

也可以直接构造：

```cpp
pair<int, int> p(3, 5);
```

### 2.2 pair 放进队列

```cpp
queue< pair<int, int> > q;
q.push(make_pair(1, 2));

pair<int, int> cur = q.front();
q.pop();

int x = cur.first;
int y = cur.second;
```

### 2.3 pair 排序规则

`pair` 默认先按 `first` 升序，再按 `second` 升序。

```cpp
pair<int, int> a[1005];
sort(a, a + n);
```

自定义排序：

```cpp
bool cmp(const pair<int, int> &x, const pair<int, int> &y) {
    if (x.second != y.second) {
        return x.second < y.second;
    }
    return x.first < y.first;
}
```

## 3. DFS 板子：不用 vector

### 3.1 网格 DFS

适合：连通块、岛屿数量、能不能走到某点。

```cpp
const int MAXN = 105;
int n, m;
char g[MAXN][MAXN];
int vis[MAXN][MAXN];
int dx[4] = {-1, 1, 0, 0};
int dy[4] = {0, 0, -1, 1};

void dfs(int x, int y) {
    int k;
    vis[x][y] = 1;

    for (k = 0; k < 4; ++k) {
        int nx = x + dx[k];
        int ny = y + dy[k];

        if (nx < 0 || nx >= n || ny < 0 || ny >= m) {
            continue;
        }
        if (vis[nx][ny] || g[nx][ny] == '#') {
            continue;
        }
        dfs(nx, ny);
    }
}
```

### 3.2 链式前向星 DFS

适合：普通图遍历，不想用 `vector` 存邻接表。

```cpp
const int MAXN = 1005;
const int MAXM = 20005;

int head[MAXN], to[MAXM], nxt[MAXM], idx;
int vis[MAXN];

void init_graph(int n) {
    int i;
    for (i = 1; i <= n; ++i) {
        head[i] = -1;
    }
    idx = 0;
}

void add_edge(int u, int v) {
    to[idx] = v;
    nxt[idx] = head[u];
    head[u] = idx;
    ++idx;
}

void dfs_graph(int u) {
    int i;
    vis[u] = 1;

    for (i = head[u]; i != -1; i = nxt[i]) {
        int v = to[i];
        if (!vis[v]) {
            dfs_graph(v);
        }
    }
}
```

无向图要加两次边：

```cpp
add_edge(u, v);
add_edge(v, u);
```

## 4. BFS 板子：不用 vector

### 4.1 网格 BFS 最短路

适合：迷宫最短步数、每一步代价相同。

```cpp
const int MAXN = 105;
int n, m;
char g[MAXN][MAXN];
int dista[MAXN][MAXN];
int dx[4] = {-1, 1, 0, 0};
int dy[4] = {0, 0, -1, 1};

int bfs(int sx, int sy, int tx, int ty) {
    queue< pair<int, int> > q;
    int i, j, k;

    for (i = 0; i < n; ++i) {
        for (j = 0; j < m; ++j) {
            dista[i][j] = -1;
        }
    }

    dista[sx][sy] = 0;
    q.push(make_pair(sx, sy));

    while (!q.empty()) {
        pair<int, int> cur = q.front();
        q.pop();

        int x = cur.first;
        int y = cur.second;

        if (x == tx && y == ty) {
            return dista[x][y];
        }

        for (k = 0; k < 4; ++k) {
            int nx = x + dx[k];
            int ny = y + dy[k];

            if (nx < 0 || nx >= n || ny < 0 || ny >= m) {
                continue;
            }
            if (g[nx][ny] == '#' || dista[nx][ny] != -1) {
                continue;
            }

            dista[nx][ny] = dista[x][y] + 1;
            q.push(make_pair(nx, ny));
        }
    }
    return -1;
}
```

### 4.2 链式前向星 BFS

```cpp
const int MAXN = 1005;
const int MAXM = 20005;

int head[MAXN], to[MAXM], nxt[MAXM], idx;
int dista[MAXN];

void init_graph(int n) {
    int i;
    for (i = 1; i <= n; ++i) {
        head[i] = -1;
    }
    idx = 0;
}

void add_edge(int u, int v) {
    to[idx] = v;
    nxt[idx] = head[u];
    head[u] = idx;
    ++idx;
}

void bfs_graph(int s, int n) {
    queue<int> q;
    int i;

    for (i = 1; i <= n; ++i) {
        dista[i] = -1;
    }

    dista[s] = 0;
    q.push(s);

    while (!q.empty()) {
        int u = q.front();
        q.pop();

        for (i = head[u]; i != -1; i = nxt[i]) {
            int v = to[i];
            if (dista[v] == -1) {
                dista[v] = dista[u] + 1;
                q.push(v);
            }
        }
    }
}
```

核心记法：

- BFS 求最短路时，入队时就标记距离。
- DFS 适合“搜完整片区域”，BFS 适合“最少几步”。

## 5. 贪心策略

贪心不是一个固定公式，而是：排序或选择一个局部规则，让每一步最优能推出整体最优。

### 5.1 常见策略表

| 题型 | 贪心规则 | 常见关键词 |
|---|---|---|
| 活动选择 | 按结束时间最早选 | 最多不重叠区间 |
| 区间覆盖 | 按左端点排序，每次扩到最远右端点 | 最少区间覆盖一段 |
| 排队接水 | 时间短的先做 | 总等待时间最小 |
| 分数背包 | 单位价值高的先拿 | 可以取一部分 |
| 哈夫曼合并 | 每次合并最小两个 | 最小合并代价 |
| 最小生成树 | 每次选最短安全边 | 连通所有点总代价最小 |

### 5.2 活动选择板子

```cpp
struct Node {
    int l;
    int r;
} a[1005];

bool cmp(const Node &x, const Node &y) {
    if (x.r != y.r) {
        return x.r < y.r;
    }
    return x.l < y.l;
}

int solve(int n) {
    int i;
    int ans = 0;
    int last = -1000000000;

    sort(a, a + n, cmp);

    for (i = 0; i < n; ++i) {
        if (a[i].l >= last) {
            ++ans;
            last = a[i].r;
        }
    }
    return ans;
}
```

### 5.3 区间覆盖思路

题意：给很多区间，覆盖 `[L, R]`，问最少用几个。

1. 按左端点升序排序。
2. 当前已经覆盖到 `cur`。
3. 在所有 `l <= cur` 的区间中，选 `r` 最大的。
4. 如果最大右端点没有超过 `cur`，说明失败。

## 6. 常见 DP 状态转移

### 6.1 DP 先问四句话

1. `dp[i]` 或 `dp[i][j]` 表示什么？
2. 最后一阶段是什么？最后一步怎么来？
3. 初始值是什么？
4. 答案在 `dp[n]`、`dp[n][m]`，还是所有状态最大值？

### 6.2 线性 DP：最大子段和

状态：`dp[i]` 表示以 `i` 结尾的最大连续子段和。

```cpp
dp[1] = a[1];
ans = dp[1];
for (i = 2; i <= n; ++i) {
    dp[i] = max(a[i], dp[i - 1] + a[i]);
    ans = max(ans, dp[i]);
}
```

### 6.3 LIS / LNDS

最长严格上升子序列：

```cpp
if (a[j] < a[i]) {
    dp[i] = max(dp[i], dp[j] + 1);
}
```

最长不下降子序列：

```cpp
if (a[j] <= a[i]) {
    dp[i] = max(dp[i], dp[j] + 1);
}
```

完整板子：

```cpp
for (i = 1; i <= n; ++i) {
    dp[i] = 1;
}

ans = 1;
for (i = 1; i <= n; ++i) {
    for (j = 1; j < i; ++j) {
        if (a[j] <= a[i]) {
            dp[i] = max(dp[i], dp[j] + 1);
        }
    }
    ans = max(ans, dp[i]);
}
```

### 6.4 LCS：最长公共子序列

状态：`dp[i][j]` 表示 `a[1..i]` 和 `b[1..j]` 的 LCS 长度。

```cpp
if (a[i] == b[j]) {
    dp[i][j] = dp[i - 1][j - 1] + 1;
} else {
    dp[i][j] = max(dp[i - 1][j], dp[i][j - 1]);
}
```

### 6.5 最长公共子串

注意“子串”必须连续。

```cpp
if (a[i] == b[j]) {
    dp[i][j] = dp[i - 1][j - 1] + 1;
    ans = max(ans, dp[i][j]);
} else {
    dp[i][j] = 0;
}
```

### 6.6 编辑距离

状态：`dp[i][j]` 表示把 `a[1..i]` 变成 `b[1..j]` 的最少操作数。

```cpp
dp[i][0] = i;
dp[0][j] = j;

if (a[i] == b[j]) {
    dp[i][j] = dp[i - 1][j - 1];
} else {
    dp[i][j] = min(dp[i - 1][j], dp[i][j - 1]) + 1;
    dp[i][j] = min(dp[i][j], dp[i - 1][j - 1] + 1);
}
```

三种操作分别是删除、插入、替换。

### 6.7 数字三角形 / 网格路径

从上往下：

```cpp
dp[i][j] = max(dp[i - 1][j - 1], dp[i - 1][j]) + a[i][j];
```

从左上到右下，只能向右或向下：

```cpp
dp[i][j] = max(dp[i - 1][j], dp[i][j - 1]) + a[i][j];
```

如果求路径数量：

```cpp
dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
```

### 6.8 区间 DP

常见关键词：合并石子、括号匹配、区间最优。

```cpp
for (len = 2; len <= n; ++len) {
    for (l = 1; l + len - 1 <= n; ++l) {
        r = l + len - 1;
        dp[l][r] = INF;
        for (k = l; k < r; ++k) {
            dp[l][r] = min(dp[l][r], dp[l][k] + dp[k + 1][r] + cost(l, r));
        }
    }
}
```

### 6.9 矩阵链乘

如果矩阵维度是 `p[0], p[1], ..., p[n]`，第 `i` 个矩阵是 `p[i-1] * p[i]`。

```cpp
for (len = 2; len <= n; ++len) {
    for (i = 1; i + len - 1 <= n; ++i) {
        j = i + len - 1;
        dp[i][j] = INF;
        for (k = i; k < j; ++k) {
            dp[i][j] = min(dp[i][j],
                dp[i][k] + dp[k + 1][j] + p[i - 1] * p[k] * p[j]);
        }
    }
}
```

### 6.10 背包 DP 总表

| 类型 | 每件物品次数 | 容量循环方向 | 转移 |
|---|---:|---|---|
| `0/1` 背包 | 最多 1 次 | 从大到小 | `dp[j] = max(dp[j], dp[j-w]+v)` |
| 完全背包 | 无限次 | 从小到大 | `dp[j] = max(dp[j], dp[j-w]+v)` |
| 恰好装满 | 看题型 | 看题型 | 初始 `dp[0]=0`，其他设负无穷 |
| 方案数 | 看题型 | 看题型 | `dp[j] += dp[j-w]` |

### 6.11 0/1 背包板子

```cpp
int w[1005], v[1005], dp[10005];

for (i = 1; i <= n; ++i) {
    for (j = m; j >= w[i]; --j) {
        dp[j] = max(dp[j], dp[j - w[i]] + v[i]);
    }
}
```

记法：`0/1` 背包倒着扫，因为每件只能用一次。

### 6.12 完全背包板子

```cpp
int w[1005], v[1005], dp[10005];

for (i = 1; i <= n; ++i) {
    for (j = w[i]; j <= m; ++j) {
        dp[j] = max(dp[j], dp[j - w[i]] + v[i]);
    }
}
```

记法：完全背包正着扫，因为同一件可以反复用。

### 6.13 背包方案数

如果问装成容量 `m` 有多少种方案：

```cpp
dp[0] = 1;
for (i = 1; i <= n; ++i) {
    for (j = m; j >= w[i]; --j) {
        dp[j] += dp[j - w[i]];
    }
}
```

如果每种物品无限个，把 `j` 改成从小到大。

### 6.14 恰好装满

最大价值，且必须恰好装满：

```cpp
const int NEG = -1000000000;

for (j = 1; j <= m; ++j) {
    dp[j] = NEG;
}
dp[0] = 0;
```

之后正常转移。最后如果 `dp[m] < 0`，通常表示无法恰好装满。

## 7. Dijkstra 板子

适合：单源最短路，边权非负。

不适合：有负权边。负权边一般考虑 `Bellman-Ford` 或 `SPFA`，考试若没要求不要乱套。

### 7.1 邻接矩阵版：最容易默写

```cpp
const int MAXN = 505;
const int INF = 0x3f3f3f3f;

int n, m;
int g[MAXN][MAXN];
int dista[MAXN];
int vis[MAXN];

void dijkstra(int s) {
    int i, j;

    for (i = 1; i <= n; ++i) {
        dista[i] = INF;
        vis[i] = 0;
    }
    dista[s] = 0;

    for (i = 1; i <= n; ++i) {
        int u = 0;
        for (j = 1; j <= n; ++j) {
            if (!vis[j] && (u == 0 || dista[j] < dista[u])) {
                u = j;
            }
        }

        if (u == 0 || dista[u] == INF) {
            break;
        }
        vis[u] = 1;

        for (j = 1; j <= n; ++j) {
            if (!vis[j] && g[u][j] < INF) {
                dista[j] = min(dista[j], dista[u] + g[u][j]);
            }
        }
    }
}
```

核心变量：

- `dista[v]`：源点 `s` 到 `v` 的当前最短距离。
- `vis[v]`：`v` 的最短路是否已经确定。
- `u`：本轮从未确定点中选出的 `dista` 最小点。
- `g[u][j]`：`u` 到 `j` 的边权，`INF` 表示没有边。

边界问题：

- 有负权边不能用 `Dijkstra`。
- 不可达点最后仍然是 `INF`。
- 重边建图时取更小边权。
- 如果 `dista[u] + g[u][j]` 可能超过 `int`，距离数组改成 `long long`。

建图：

```cpp
for (i = 1; i <= n; ++i) {
    for (j = 1; j <= n; ++j) {
        if (i == j) {
            g[i][j] = 0;
        } else {
            g[i][j] = INF;
        }
    }
}

for (i = 1; i <= m; ++i) {
    cin >> u >> v >> w;
    if (w < g[u][v]) {
        g[u][v] = w;
        // 无向图再加这一句：
        // g[v][u] = w;
    }
}
```

## 8. Prim 板子

适合：无向连通图最小生成树。和 Dijkstra 长得像，但意义不同。

- `Dijkstra`：`dista[v]` 是从源点到 `v` 的最短路。
- `Prim`：`low[v]` 是当前生成树连到 `v` 的最小边权。

```cpp
const int MAXN = 505;
const int INF = 0x3f3f3f3f;

int n, m;
int g[MAXN][MAXN];
int low[MAXN];
int vis[MAXN];

int prim() {
    int i, j;
    int ans = 0;
    int cnt = 1;

    for (i = 1; i <= n; ++i) {
        low[i] = g[1][i];
        vis[i] = 0;
    }
    vis[1] = 1;

    for (i = 1; i <= n - 1; ++i) {
        int u = -1;
        int best = INF;

        for (j = 1; j <= n; ++j) {
            if (!vis[j] && low[j] < best) {
                best = low[j];
                u = j;
            }
        }

        if (u == -1) {
            break;
        }

        vis[u] = 1;
        ans += best;
        ++cnt;

        for (j = 1; j <= n; ++j) {
            if (!vis[j] && g[u][j] < low[j]) {
                low[j] = g[u][j];
            }
        }
    }

    if (cnt != n) {
        return -1;
    }
    return ans;
}
```

建图和 `Dijkstra` 类似，但无向图必须双向：

```cpp
g[u][v] = min(g[u][v], w);
g[v][u] = min(g[v][u], w);
```

## 9. lower_bound / upper_bound 用法

前提：区间必须已经按升序排好，头文件是 `<algorithm>`。

### 9.1 基本语法

```cpp
lower_bound(first, last, x);  // 第一个 >= x 的位置
upper_bound(first, last, x);  // 第一个 > x 的位置
```

返回值是迭代器，不是下标，也不是元素值。

| 写法 | 含义 |
|---|---|
| `lower_bound(a, a + n, x) - a` | 数组中第一个 `>= x` 的下标 |
| `upper_bound(a, a + n, x) - a` | 数组中第一个 `> x` 的下标 |
| `lower_bound(v.begin(), v.end(), x) - v.begin()` | `vector` 中第一个 `>= x` 的下标 |
| `upper_bound(v.begin(), v.end(), x) - v.begin()` | `vector` 中第一个 `> x` 的下标 |

### 9.2 数组用法

```cpp
int a[8] = {1, 2, 2, 2, 4, 7, 9, 9};
int n = 8;
int x = 2;

int l = lower_bound(a, a + n, x) - a;  // 第一个 >= x 的位置
int r = upper_bound(a, a + n, x) - a;  // 第一个 > x 的位置
int cnt = r - l;                       // x 出现次数
```

在这个例子中：

- `lower_bound(..., 2)` 返回下标 `1`
- `upper_bound(..., 2)` 返回下标 `4`
- 所以 `2` 出现了 `4 - 1 = 3` 次

判断 `x` 是否存在：

```cpp
if (l < n && a[l] == x) {
    cout << "found" << endl;
}
```

注意：只判断 `l < n` 不够，因为 `lower_bound` 找到的可能是第一个大于 `x` 的数。

### 9.3 vector 用法

```cpp
vector<int> v;
int pos = lower_bound(v.begin(), v.end(), x) - v.begin();

if (pos < (int)v.size() && v[pos] == x) {
    cout << "found" << endl;
}
```

如果只想拿迭代器：

```cpp
vector<int>::iterator it = lower_bound(v.begin(), v.end(), x);
if (it != v.end()) {
    cout << *it << endl;
}
```

### 9.4 set 用法

`set` 自带成员函数，返回值也是迭代器。

```cpp
set<int> st;
set<int>::iterator it;

it = st.lower_bound(x);  // set 中第一个 >= x
if (it != st.end()) {
    cout << *it << endl;
}

it = st.upper_bound(x);  // set 中第一个 > x
if (it != st.end()) {
    cout << *it << endl;
}
```

### 9.5 前驱、后继、插入位置

```cpp
// 第一个 >= x 的数，也就是 x 的后继候选
int p = lower_bound(a, a + n, x) - a;
if (p < n) {
    cout << a[p] << endl;
}

// 第一个 > x 的数
p = upper_bound(a, a + n, x) - a;
if (p < n) {
    cout << a[p] << endl;
}

// 最后一个 < x 的数
p = lower_bound(a, a + n, x) - a;
if (p > 0) {
    cout << a[p - 1] << endl;
}

// 最后一个 <= x 的数
p = upper_bound(a, a + n, x) - a;
if (p > 0) {
    cout << a[p - 1] << endl;
}

// 插入 x 后仍保持有序的位置，一般用 lower_bound
p = lower_bound(a, a + n, x) - a;
```

### 9.6 pair 的 lower_bound

`pair` 默认先比较 `first`，再比较 `second`。

```cpp
pair<int, int> a[1005];
sort(a, a + n);

// 找第一个 first >= x 的 pair
pair<int, int> key = make_pair(x, -INF);
int pos = lower_bound(a, a + n, key) - a;
```

如果要找第一个 `first > x`，可以用：

```cpp
pair<int, int> key = make_pair(x, INF);
int pos = upper_bound(a, a + n, key) - a;
```

### 9.7 降序序列语法

如果数组按降序排序，`lower_bound` / `upper_bound` 的比较器必须和排序时一致。

```cpp
sort(a, a + n, greater<int>());
int pos = lower_bound(a, a + n, x, greater<int>()) - a;
```

注意：降序语义更绕。考试不熟时，优先把序列转成升序再使用默认写法。

### 9.8 手写 lower_bound / upper_bound

```cpp
int my_lower_bound(int a[], int n, int x) {
    int l = 0;
    int r = n;
    while (l < r) {
        int mid = l + (r - l) / 2;
        if (a[mid] >= x) {
            r = mid;
        } else {
            l = mid + 1;
        }
    }
    return l;
}

int my_upper_bound(int a[], int n, int x) {
    int l = 0;
    int r = n;
    while (l < r) {
        int mid = l + (r - l) / 2;
        if (a[mid] > x) {
            r = mid;
        } else {
            l = mid + 1;
        }
    }
    return l;
}
```

记法：

- `lower_bound`：第一个 `>= x`
- `upper_bound`：第一个 `> x`
- 等于 `x` 的区间是 `[lower_bound, upper_bound)`
- 小于 `x` 的最后一个位置是 `lower_bound(x) - 1`
- 小于等于 `x` 的最后一个位置是 `upper_bound(x) - 1`

边界提醒：

- `a + n` 是右开结尾，不要写成 `a + n - 1`。
- 如果 `x` 比所有元素都大，`lower_bound` 和 `upper_bound` 都会返回 `n`。
- 如果 `x` 比所有元素都小，`lower_bound` 可能返回 `0`。
- 访问 `a[pos]` 前必须判断 `0 <= pos < n`。
- 降序数组不要直接套默认 `lower_bound`；考试不熟时优先转成升序。

## 10. 二分查找板子

### 10.1 查找某个值是否存在

```cpp
int binary_search_value(int a[], int n, int x) {
    int l = 0;
    int r = n - 1;

    while (l <= r) {
        int mid = l + (r - l) / 2;
        if (a[mid] == x) {
            return mid;
        } else if (a[mid] < x) {
            l = mid + 1;
        } else {
            r = mid - 1;
        }
    }
    return -1;
}
```

### 10.2 找第一个满足条件的位置

适合：条件形如 `false false false true true true`。

```cpp
int first_true(int n) {
    int l = 0;
    int r = n;

    while (l < r) {
        int mid = l + (r - l) / 2;
        if (check(mid)) {
            r = mid;
        } else {
            l = mid + 1;
        }
    }
    return l;
}
```

### 10.3 找最后一个满足条件的位置

适合：条件形如 `true true true false false false`。

```cpp
int last_true(int n) {
    int l = 0;
    int r = n - 1;

    while (l < r) {
        int mid = l + (r - l + 1) / 2;
        if (check(mid)) {
            l = mid;
        } else {
            r = mid - 1;
        }
    }
    return l;
}
```

关键：找最后一个满足时，`mid` 要偏右：`(l + r + 1) / 2`，否则可能死循环。

## 11. 二分答案板子

二分答案适合：答案有单调性。

常见问法：

- 最小化最大值：`check(x)` 判断答案能不能不超过 `x`。
- 最大化最小值：`check(x)` 判断答案能不能至少达到 `x`。

### 11.1 最小化答案

条件：`check(x)` 越大越容易成功，形如 `false false true true`。

```cpp
int solve_min_answer() {
    int l = low;
    int r = high;

    while (l < r) {
        int mid = l + (r - l) / 2;
        if (check(mid)) {
            r = mid;
        } else {
            l = mid + 1;
        }
    }
    return l;
}
```

例子：最小最大段和、最少时间、最小容量。

### 11.2 最大化答案

条件：`check(x)` 越大越难成功，形如 `true true false false`。

```cpp
int solve_max_answer() {
    int l = low;
    int r = high;

    while (l < r) {
        int mid = l + (r - l + 1) / 2;
        if (check(mid)) {
            l = mid;
        } else {
            r = mid - 1;
        }
    }
    return l;
}
```

例子：最大最小距离、最多能切多长、最低能力值。

### 11.3 二分答案的 check 写法

例子：把数组分成不超过 `k` 段，使每段和不超过 `limit`，判断能不能做到。

```cpp
int a[100005];
int n, k;

int check(int limit) {
    int i;
    int cnt = 1;
    int sum = 0;

    for (i = 1; i <= n; ++i) {
        if (a[i] > limit) {
            return 0;
        }
        if (sum + a[i] <= limit) {
            sum += a[i];
        } else {
            ++cnt;
            sum = a[i];
        }
    }
    return cnt <= k;
}
```

## 12. 经典题型拓展与边界

### 12.1 搜索

| 题型 | 板子作用 | 边界处理 |
|---|---|---|
| 连通块 / 岛屿数量 | `DFS` 搜完整片区域，外层遇到未访问点就计数 | 四方向还是八方向、障碍字符、下标范围 |
| 迷宫最短路 | `BFS` 按层扩展，第一次到终点就是最短 | 起点等于终点答案为 `0`，不可达输出 `-1` 或题目指定值 |
| 多源 BFS | 多个源点同时入队，求最近源点距离 | 所有源点距离设 `0`，入队时就标记 |
| 普通图遍历 | 链式前向星遍历邻接点 | 无向图加两条边，`MAXM` 按双向边开两倍 |

### 12.2 贪心

| 题型 | 板子作用 | 边界处理 |
|---|---|---|
| 活动安排 | 按结束时间排序，能选就选 | 区间端点能否相接看题目，是 `<` 还是 `<=` |
| 区间覆盖 | 当前能接上的区间里选右端点最远的 | 如果最远右端点没有推进，说明无法覆盖 |
| 排队接水 | 短任务优先，减少总等待时间 | 平均值输出格式、相同时间的处理 |
| 哈夫曼合并 | 小根堆每次合并最小两个 | `n=1` 时总代价通常为 `0` |

### 12.3 DP 与背包

| 题型 | 板子作用 | 边界处理 |
|---|---|---|
| 最大子段和 | 连续区间最大和 | 全负数时不能把答案初始化为 `0` |
| LIS / LNDS | `dp[i]` 表示以 `i` 结尾的最优长度 | 严格上升用 `<`，不下降用 `<=` |
| LCS / 公共子串 | 比较两个序列的公共结构 | LCS 不连续，公共子串不等时 `dp=0` |
| 编辑距离 | 插入、删除、替换的最少次数 | `dp[i][0]=i`，`dp[0][j]=j` |
| `0/1` 背包 | 每件最多一次，容量倒序 | 恰好装满时不可达状态设负无穷 |
| 完全背包 | 每件无限次，容量正序 | 方案数用 `+=`，最大价值用 `max` |
| 区间 DP | 枚举长度、左端点、断点 | 求最小值先设 `INF`，注意 `cost(l,r)` |

### 12.4 图论与二分

| 题型 | 板子作用 | 边界处理 |
|---|---|---|
| 单源最短路 | `Dijkstra` 求一个起点到所有点的最短距离 | 边权非负，不可达保持 `INF` |
| 最小生成树 | `Prim` / `Kruskal` 连通所有点且总权最小 | 图不连通要输出失败 |
| 统计出现次数 | `upper_bound - lower_bound` | 序列必须有序，区间是 `[l,r)` |
| 第一个满足 | `false false true true` | 成功时 `r=mid` |
| 最后一个满足 | `true true false false` | `mid` 偏右，成功时 `l=mid` |
| 二分答案 | 在答案范围上二分，`check` 判断可行 | 先证明单调性，`low/high` 覆盖全部答案 |

## 13. 考试易错点清单

- `DFS/BFS`：入队或进入递归前后，必须想清楚什么时候标记访问。
- `BFS`：无权最短路第一次到达就是最短，带权不要乱用。
- `pair`：`first` / `second` 别写反。
- `sort`：比较函数必须满足严格弱序，不能乱写 `<=`。
- `0/1` 背包：容量倒序。
- 完全背包：容量正序。
- `LCS`：子序列可以不连续，子串必须连续。
- `Dijkstra`：不能处理负权边。
- `Prim`：图不连通时要输出失败或特殊值。
- 二分：先判断要找“第一个真”还是“最后一个真”，再决定 `mid` 是否偏右。

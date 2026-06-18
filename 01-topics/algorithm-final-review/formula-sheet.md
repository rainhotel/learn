# Algorithm Final Review Formula Sheet

## Core Formulas Or Methods

### Item 1

- 名称：`C++98` 考试基础头文件
- 表达式/方法：不用 `bits/stdc++.h`，固定使用标准头文件
- 含义：保证代码风格统一，适合考试默写
- 什么时候用：本主题所有板子
- 不该在什么时候用：无
- 常见误用：忘记补 `algorithm` 或 `cstring`

```cpp
#include <iostream>
#include <cstdio>
#include <cstring>
#include <algorithm>
#include <queue>
#include <vector>
#include <utility>
using namespace std;
```

### Item 2

- 名称：网格 `DFS` 连通块
- 表达式/方法：访问当前点，向四个方向递归
- 含义：适合统计连通块、搜索整片区域
- 什么时候用：只关心能否搜到、区域大小、块数
- 不该在什么时候用：要求最少步数时
- 常见误用：忘记打 `vis`

```cpp
#include <iostream>
#include <cstring>
using namespace std;

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

### Item 3

- 名称：网格 `BFS` 最短步数
- 表达式/方法：队列分层扩展，第一次到达终点就是最短路
- 含义：适合无权图、每步代价相同的最短路
- 什么时候用：迷宫最短路、最少操作次数
- 不该在什么时候用：边权不同
- 常见误用：出队时才标记访问，导致重复入队

```cpp
#include <iostream>
#include <queue>
#include <cstring>
#include <utility>
using namespace std;

const int MAXN = 105;
int n, m;
char g[MAXN][MAXN];
int dista[MAXN][MAXN];
int dx[4] = {-1, 1, 0, 0};
int dy[4] = {0, 0, -1, 1};

int bfs(int sx, int sy, int tx, int ty) {
    queue< pair<int, int> > q;
    memset(dista, -1, sizeof(dista));
    dista[sx][sy] = 0;
    q.push(make_pair(sx, sy));

    while (!q.empty()) {
        pair<int, int> cur = q.front();
        q.pop();
        int x = cur.first;
        int y = cur.second;
        int k;

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

### Item 4

- 名称：二分查找边界模板
- 表达式/方法：闭开区间 `[l, r)` 找第一个 `>= x`
- 含义：稳定、短、好改
- 什么时候用：有序数组查边界
- 不该在什么时候用：数组无序
- 常见误用：`mid` 更新和区间定义不一致

```cpp
int lower_bound_index(int a[], int n, int x) {
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

int upper_bound_index(int a[], int n, int x) {
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

### Item 5

- 名称：`STL lower_bound / upper_bound`
- 表达式/方法：有序区间上直接调用
- 含义：代码更短，适合考试快速写
- 什么时候用：数组或 `vector` 已排序
- 不该在什么时候用：未排序序列
- 常见误用：把返回的迭代器当成值而不是位置

```cpp
#include <iostream>
#include <algorithm>
using namespace std;

int main() {
    int a[8] = {1, 2, 2, 2, 4, 7, 9, 9};
    int n = 8;
    int x = 2;
    int l = lower_bound(a, a + n, x) - a;
    int r = upper_bound(a, a + n, x) - a;
    cout << l << " " << r << " " << (r - l) << endl;
    return 0;
}
```

### Item 6

- 名称：贪心之活动选择
- 表达式/方法：按结束时间从小到大排序，能选就选
- 含义：最大化不重叠区间数量
- 什么时候用：区间选择、活动安排
- 不该在什么时候用：局部最优不保证整体最优的题
- 常见误用：排序依据写成开始时间

```cpp
#include <iostream>
#include <algorithm>
using namespace std;

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

int main() {
    int n, i;
    cin >> n;
    for (i = 0; i < n; ++i) {
        cin >> a[i].l >> a[i].r;
    }
    sort(a, a + n, cmp);

    int ans = 0;
    int last = -1000000000;
    for (i = 0; i < n; ++i) {
        if (a[i].l >= last) {
            ++ans;
            last = a[i].r;
        }
    }
    cout << ans << endl;
    return 0;
}
```

### Item 7

- 名称：最大子段和
- 表达式/方法：`dp[i] = max(a[i], dp[i - 1] + a[i])`
- 含义：以当前元素结尾的最优值
- 什么时候用：连续子段最值
- 不该在什么时候用：子序列可跳着选
- 常见误用：把“子段”写成“子序列”

```cpp
#include <iostream>
#include <algorithm>
using namespace std;

int main() {
    int n, i;
    int a[1005], dp[1005];
    cin >> n;
    for (i = 1; i <= n; ++i) {
        cin >> a[i];
    }

    dp[1] = a[1];
    int ans = dp[1];
    for (i = 2; i <= n; ++i) {
        dp[i] = max(a[i], dp[i - 1] + a[i]);
        ans = max(ans, dp[i]);
    }
    cout << ans << endl;
    return 0;
}
```

### Item 8

- 名称：最长不降子序列 `O(n^2)`
- 表达式/方法：`a[j] <= a[i]` 时尝试转移
- 含义：考试最稳版本
- 什么时候用：数据范围不大，要求序列长度
- 不该在什么时候用：要求严格上升时不能用 `<=`
- 常见误用：把不降写成上升

```cpp
#include <iostream>
#include <algorithm>
using namespace std;

int main() {
    int n, i, j;
    int a[1005], dp[1005];
    cin >> n;
    for (i = 1; i <= n; ++i) {
        cin >> a[i];
        dp[i] = 1;
    }

    int ans = 1;
    for (i = 1; i <= n; ++i) {
        for (j = 1; j < i; ++j) {
            if (a[j] <= a[i]) {
                dp[i] = max(dp[i], dp[j] + 1);
            }
        }
        ans = max(ans, dp[i]);
    }
    cout << ans << endl;
    return 0;
}
```

### Item 9

- 名称：`0/1` 背包
- 表达式/方法：`dp[j] = max(dp[j], dp[j - w[i]] + v[i])`
- 含义：每件物品只能取一次
- 什么时候用：容量限制下求最大价值
- 不该在什么时候用：物品可无限取时
- 常见误用：`j` 从小到大循环，写成完全背包

```cpp
#include <iostream>
#include <algorithm>
using namespace std;

int w[1005], v[1005], dp[10005];

int main() {
    int n, m, i, j;
    cin >> n >> m;
    for (i = 1; i <= n; ++i) {
        cin >> w[i] >> v[i];
    }

    for (i = 1; i <= n; ++i) {
        for (j = m; j >= w[i]; --j) {
            dp[j] = max(dp[j], dp[j - w[i]] + v[i]);
        }
    }
    cout << dp[m] << endl;
    return 0;
}
```

### Item 10

- 名称：最长公共子序列
- 表达式/方法：相等取左上角 `+1`，不等取上或左最大
- 含义：两个序列的经典二维 DP
- 什么时候用：比较两个串或序列的公共结构
- 不该在什么时候用：要求连续子串时
- 常见误用：把子序列和子串混掉

```cpp
#include <iostream>
#include <cstring>
#include <algorithm>
using namespace std;

char a[1005], b[1005];
int dp[1005][1005];

int main() {
    int i, j;
    cin >> (a + 1) >> (b + 1);
    int n = strlen(a + 1);
    int m = strlen(b + 1);

    for (i = 1; i <= n; ++i) {
        for (j = 1; j <= m; ++j) {
            if (a[i] == b[j]) {
                dp[i][j] = dp[i - 1][j - 1] + 1;
            } else {
                dp[i][j] = max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
    }
    cout << dp[n][m] << endl;
    return 0;
}
```

### Item 11

- 名称：`Floyd` 全源最短路
- 表达式/方法：`d[i][j] = min(d[i][j], d[i][k] + d[k][j])`
- 含义：枚举中转点优化任意两点距离
- 什么时候用：点数不大，需要多源到多源最短路
- 不该在什么时候用：点数很大
- 常见误用：`INF + INF` 溢出或直接拿来比较

```cpp
#include <iostream>
#include <cstring>
#include <algorithm>
using namespace std;

const int INF = 0x3f3f3f3f;
int d[105][105];

int main() {
    int n, m, i, j, k;
    cin >> n >> m;

    for (i = 1; i <= n; ++i) {
        for (j = 1; j <= n; ++j) {
            if (i == j) {
                d[i][j] = 0;
            } else {
                d[i][j] = INF;
            }
        }
    }

    for (i = 1; i <= m; ++i) {
        int u, v, w;
        cin >> u >> v >> w;
        if (w < d[u][v]) {
            d[u][v] = w;
            d[v][u] = w;
        }
    }

    for (k = 1; k <= n; ++k) {
        for (i = 1; i <= n; ++i) {
            for (j = 1; j <= n; ++j) {
                if (d[i][k] < INF && d[k][j] < INF) {
                    d[i][j] = min(d[i][j], d[i][k] + d[k][j]);
                }
            }
        }
    }

    cout << d[1][n] << endl;
    return 0;
}
```

### Item 12

- 名称：`Kruskal` 最小生成树
- 表达式/方法：边排序，从小到大选，不成环就加入
- 含义：最稳的最小生成树考试板子
- 什么时候用：无向图最小生成树
- 不该在什么时候用：图不连通但题目又要求一定联通时要额外判断
- 常见误用：忘记最后判断是否选满 `n - 1` 条边

```cpp
#include <iostream>
#include <algorithm>
using namespace std;

struct Edge {
    int u;
    int v;
    int w;
} e[5005];

int fa[1005];

int find_set(int x) {
    if (fa[x] == x) {
        return x;
    }
    fa[x] = find_set(fa[x]);
    return fa[x];
}

bool cmp(const Edge &a, const Edge &b) {
    return a.w < b.w;
}

int main() {
    int n, m, i;
    cin >> n >> m;
    for (i = 1; i <= m; ++i) {
        cin >> e[i].u >> e[i].v >> e[i].w;
    }
    for (i = 1; i <= n; ++i) {
        fa[i] = i;
    }

    sort(e + 1, e + m + 1, cmp);

    int ans = 0;
    int cnt = 0;
    for (i = 1; i <= m; ++i) {
        int fu = find_set(e[i].u);
        int fv = find_set(e[i].v);
        if (fu != fv) {
            fa[fu] = fv;
            ans += e[i].w;
            ++cnt;
            if (cnt == n - 1) {
                break;
            }
        }
    }

    if (cnt != n - 1) {
        cout << "impossible" << endl;
    } else {
        cout << ans << endl;
    }
    return 0;
}
```

## Derived From

- 来自哪几道题：老师期末考点图对应的基础题型
- 来自哪些资料：用户提供手写考点、仓库已有算法主题写法

# 第 22 章参考答案与评分

## 使用说明

参考答案用于复盘，不应用于制造“独立完成”记录。首次尝试前读过答案时，证据卡必须标记 `reviewed`；之后闭卷重做可以形成 recall 证据，但不能改写首次状态。

## 1. 96 题个人题单

合格题单应满足：九模块合计 96 个不同首次槽位；每模块包含直接模板、边界变化和组合题；写明题源、版本、难度仅作课程内标识。替换规则示例：连续两次 A 级复习后可用同模块综合题替换尚未开始的基础题，但不能删除失败历史。

## 2. 三题基线

没有统一目标分。合格提交是未经美化的三份时间线，至少记录何时得到提示、代码是否编译、哪些测试失败。基线用于识别错因，不足以推断正式面试通过率。

## 3. 证据卡与看板

至少分开以下计数：`planned`、`attempted`、`independent_pass`、`hinted`、`reviewed`、`unfinished`、`reviews_due`、`recall_pass`。同一题可有一次首次状态和多次复习事件，不能用一个布尔 `completed` 覆盖全部语义。

## 4. 间隔调度

默认第 1/3/7/14/30 天闭卷重做。A 进入下一间隔；B 在 2-3 天内补测实现或边界；C 次日重做并增加变式；D 退回概念和最小示例。每个事件记录计划日、实际日、结果、提示和下次日期。

## 5. lowerBound

```java
static int lowerBound(int[] a, int target) {
    int left = 0;
    int right = a.length;
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (a[mid] < target) {
            left = mid + 1;
        } else {
            right = mid;
        }
    }
    return left;
}
```

不变量：第一个满足条件的位置始终位于 `[left,right)`；不存在时答案可为 `a.length`。测试：`[] -> 0`、`[1]` 中找 0/1/2、`[1,2,2,4]` 中找 2 得 1、找 3 得 3。时间 O(log n)，空间 O(1)。

## 6. 最长无重复连续子串

```java
static int longestUniqueSubstring(String s) {
    java.util.Map<Character, Integer> last = new java.util.HashMap<>();
    int left = 0;
    int best = 0;
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        Integer previous = last.put(c, right);
        if (previous != null && previous >= left) {
            left = previous + 1;
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

窗口 `[left,right]` 始终无重复 UTF-16 code unit。每个 right 前进一次，left 只前进不后退，所以时间 O(n)，map 空间 O(min(n,字符集))。若合同要求 Unicode code point，必须用 `codePoints()` 或显式 code point 遍历，不能把 `char` 方案直接称为完整 Unicode 解法。

## 7. 合并半开区间

若合同规定相邻区间也合并，则重叠判断为 `nextStart <= currentEnd`；若相邻保持独立，则使用 `<`。排序使用 `Integer.compare` 或 `Long.compare`，不能相减。

```java
static java.util.List<long[]> merge(long[][] intervals) {
    java.util.Arrays.sort(intervals, (a, b) -> {
        int byStart = Long.compare(a[0], b[0]);
        return byStart != 0 ? byStart : Long.compare(a[1], b[1]);
    });
    java.util.List<long[]> result = new java.util.ArrayList<>();
    for (long[] interval : intervals) {
        if (result.isEmpty() || interval[0] > result.get(result.size() - 1)[1]) {
            result.add(new long[]{interval[0], interval[1]});
        } else {
            long[] last = result.get(result.size() - 1);
            last[1] = Math.max(last[1], interval[1]);
        }
    }
    return result;
}
```

时间 O(n log n)，输出外辅助空间取决于排序实现和输入修改合同。

## 8. 反转链表

```java
static Node reverse(Node head) {
    Node previous = null;
    Node current = head;
    while (current != null) {
        Node next = current.next;
        current.next = previous;
        previous = current;
        current = next;
    }
    return previous;
}
```

循环开始时：`previous` 是已反转前缀的头，`current` 是未处理后缀的头，两部分覆盖原链表且不丢节点。必须先保存 `next` 再改指针。时间 O(n)，空间 O(1)。

## 9. 最小栈

双栈方案：数据栈保存所有值，最小栈在新值小于等于当前最小时同步压入；弹出相等最小值时同步弹出。操作 O(1)，最坏 O(n) 额外空间，语义直观。

差值编码可用一个 long 保存与当前最小值的差，降低第二个栈的对象/元素开销，但推导更难且若仍用 int 会溢出。面试中若没有严格内存约束，双栈通常更清晰。重复最小值是必测边界。

## 10. 数据流 Top K

维护大小不超过 K 的小顶堆。新值到来时，堆未满则加入；已满且新值大于堆顶时替换。每个值 O(log K)，空间 O(K)。K=0 直接忽略；K 大于已见数量时返回所有已见值；是否保留重复值由合同决定；数值和比较器使用 long/`Long.compare`。

## 11. 课程依赖拓扑排序

为每个节点建立邻接表和入度，将入度零节点入队。每弹出一个节点就减少出边终点入度，降为零时入队。输出数量等于节点数则成功，否则存在环。时间 O(V+E)，空间 O(V+E)。

重复边若直接加入，会重复增加和减少入度，通常仍可一致工作，但浪费空间且容易与去重后的更新不匹配；稳妥方案是建图时使用集合去重。孤立节点入度为零，应出现在结果中。

## 12. 带重复候选的组合搜索

先排序。同一递归层中，如果 `i > start && nums[i] == nums[i-1]` 则跳过，防止相同值在同一位置产生重复分支。是否允许跨层再次选择同一个索引由问题合同决定：组合中每个元素只用一次则递归到 `i+1`；允许重复使用则递归到 `i`。加入结果时复制 path。

复杂度依赖搜索树和输出数量，不能笼统声称固定 O(2^n) 而不说明去重、目标剪枝和结果复制。

## 13. 最少硬币

定义 `dp[i]` 为组成金额 i 所需的最少硬币数。初始化 `dp[0]=0`，其余为不可达哨兵 `amount+1`。对每个 i 枚举硬币 c，若 `i>=c` 且 `dp[i-c]` 可达，则用 `dp[i-c]+1` 更新。答案仍为哨兵时返回 -1。

时间 O(amount × coins)，空间 O(amount)。金额或数组规模过大时要讨论内存约束；零或负面值硬币应被合同禁止或过滤。

## 14. 最多不重叠会议

按结束时间升序，每次选择开始时间不早于上次结束的会议。交换论证：设最优解的第一个会议不是结束最早的会议 E，将其替换为 E 不会占用更多后续时间，因此不会减少可安排数量；对剩余会议递归应用同样论证。时间由排序主导为 O(n log n)。闭区间还是半开区间会影响相邻会议是否冲突。

## 15. 随机差分测试

合格流程：限制随机输入规模使朴素解可运行；固定并记录 seed；对每个输入比较朴素与优化输出；若输出非唯一则比较规范化结果或验证共同性质；发现首个反例后保存输入并最小化。随机测试通过只能增加信心，不能代替正确性证明。

## 16. 无补全树题与图题

答案不是某段固定代码。证据应包含未修饰原稿、编译错误、逻辑错误、修复提交和主错因。若先在 IDE 写完再抄到白板，不满足本练习目的。树题至少测空树与倾斜树；图题至少测孤立点、不连通和环。

## 17. NotifyFlow 迁移

例如 Top K 只能从已有统计中选出候选租户，不能解决统计准确性、指标高基数、数据延迟和租户权限。区间合并不能自动解决时区与夏令时。拓扑排序不能解决 Agent Tool 的审批、副作用、幂等和补偿。合格答案必须显式写出这些工程边界。

## 18. 15 分钟 Teach-back

建议结构：2 分钟合同与朴素解，3 分钟瓶颈，4 分钟不变量与优化，3 分钟 Java 实现风险，2 分钟边界测试，1 分钟变式。录像中若需要读稿或答案提示，应如实记录，不判为独立 Teach-back 通过。

## 19. 陌生读者微课

验收不是读者说“懂了”，而是读者在作者不补充解释时完成一题变式，并能说出窗口合法条件、左右指针移动依据和复杂度。记录停顿、错误、耗时和反馈，修订后再次测试；第一次失败是有效证据。

## 20. 三轮模拟面试

每轮应使用未见或足够陌生的题目、统一 100 分量表和同一时间范围。评审记录必须注明提示内容，因为得到关键提示后的完成不能等价于独立完成。当前没有实际结果，状态为 Pending。

## 21. 跨轮复盘

按错因而非总分聚合。例如三轮都在编码前未说清不变量，应减少新题并增加 10 分钟方案口述；若方案正确但边界频繁失败，应增加测试矩阵和差分测试；若表达占用过多时间，应训练 5 分钟压缩讲解。

## 22. 一页证据摘要

至少列出统计口径、数据截止日期、原始证据路径、规划量、实际量和 Pending 项。不得把 `attempted` 写成 `independent_pass`，不得把同题五次复习计为五道题，不得用无评审录像推断面试成绩。

## 总评分

| 维度 | 分值 |
|---|---:|
| 数据结构、模板与复杂度 | 20 |
| 正确性、不变量与 Java 实现 | 20 |
| 测试、边界与错误诊断 | 15 |
| 间隔复习与训练纪律 | 15 |
| 白板编码与模拟面试 | 15 |
| Teach-back、陌生读者与证据诚实性 | 15 |

出现以下任一情况时不得超过 60 分：伪造题量或分数；删除失败样本；没有可运行测试；只能背代码而无法解释状态。没有陌生读者和模拟面试证据时，本章不得标记 Released。

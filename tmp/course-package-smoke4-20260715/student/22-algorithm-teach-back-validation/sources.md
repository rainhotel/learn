# 第 22 章资料与来源规则

## 1. Java 一手资料

1. Java SE 21 API Specification：<https://docs.oracle.com/en/java/javase/21/docs/api/>
2. Java Collections Framework 概览：<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/doc-files/coll-overview.html>
3. `HashMap` API：<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html>
4. `ArrayDeque` API：<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayDeque.html>
5. `PriorityQueue` API：<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html>
6. `Comparator` API：<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html>
7. JUnit 5 User Guide：<https://junit.org/junit5/docs/current/user-guide/>
8. JMH 项目：<https://openjdk.org/projects/code-tools/jmh/>

API 文档用于确认具体合同，例如是否允许 null、排序语义和方法行为。复杂度结论需要结合数据结构实现、算法分析和实际约束，不由一个 API 名称自动推出。

## 2. 算法课程与可视化

1. Princeton Algorithms：<https://algs4.cs.princeton.edu/home/>
2. MIT OpenCourseWare 6.006 Introduction to Algorithms：<https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-fall-2011/>
3. MIT OpenCourseWare 6.046J Design and Analysis of Algorithms：<https://ocw.mit.edu/courses/6-046j-design-and-analysis-of-algorithms-spring-2015/>
4. VisuAlgo：<https://visualgo.net/en>

课程发布前需复核链接、授权和引用范围。本课程只写自有讲义、代码和图示，不复制受版权保护的题解、图表或付费内容。

## 3. 题源

可从公开算法平台、企业公开笔试样题、教材练习和自编工程变式中选择题目。题单只记录来源链接、题目标题、访问日期和自写解析；平台难度标签仅作线索，不能跨平台当作统一测量标准。

不将“高频”“必考”“原题命中率”作为无来源事实。面试经验只帮助发现题型，机制与语言结论回到规范、教材和实验。

## 4. 学习科学来源

1. Dunlosky et al., *Improving Students' Learning With Effective Learning Techniques*：<https://doi.org/10.1177/1529100612453266>
2. Karpicke & Roediger, *Repeated retrieval during learning is the key to long-term retention*：<https://doi.org/10.1016/j.jml.2006.09.004>
3. Cepeda et al., *Distributed practice in verbal recall tasks: A review and quantitative synthesis*：<https://doi.org/10.1037/0033-2909.132.3.354>

这些研究支持 retrieval practice 和 distributed practice 的一般方向，但不能直接证明本章的固定 1/3/7/14/30 日程对每个人最优。该日程是可调整的课程默认值，需根据复习证据校准。

## 5. 资料使用优先级

1. Java 与工具的官方规范/API。
2. 大学算法课程、教材和同行评审研究。
3. 可复现实验与自写测试。
4. 高质量工程文章。
5. 面试经验、题单和社区讨论，仅作为选题线索。

## 6. 每道题的引用字段

```text
source_title:
source_url_or_book:
accessed_at:
problem_version_or_snapshot:
license_or_usage_note:
solution_author: learner
external_hints_used:
```

如果平台题面可能变更，应记录版本或最小必要约束摘要。公开课程不得直接再发布无授权题面、官方题解或他人代码。

## 7. 复杂度与性能声明

- 大 O 结论必须写输入规模和最坏/期望/摊还口径。
- 微基准使用 JMH，不用单次 `System.nanoTime()` 推断通用性能。
- 性能实验记录 JDK、硬件、参数、预热、输入分布和原始输出。
- “更快”不能只由复杂度符号推出；常数、缓存、分配和输入规模都可能影响结果。

## 8. 当前验证状态

| 项目 | 状态 |
|---|---|
| 资料分类与引用规则 | Draft |
| 96 题逐题来源与版本复核 | Pending |
| Java 模板编译测试 | Pending |
| 间隔复习效果记录 | Pending |
| 陌生读者与模拟面试验证 | Pending |

在逐项完成前，本章不能标记为 Lab Verified、Release Candidate 或 Released。

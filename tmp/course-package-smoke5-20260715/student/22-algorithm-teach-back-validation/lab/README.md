# 第 22 章实验：算法训练、Teach-back 与证据验证

## 当前状态

- 状态：Pending
- 已完成：实验协议、目录合同、评分量表和发布门槛
- 未完成：基线、96 题首次训练、间隔复习、Java 测试、白板录像、陌生读者测试和模拟面试

本实验没有任何已声称的刷题数量、正确率、分数或通过率。

## 1. 实验目标

验证学习者是否能在无答案提示条件下：

- 从约束推导数据结构和算法。
- 写出可编译的 Java 21 代码。
- 用测试和论证检查正确性。
- 在延迟后重新提取知识。
- 在白板或共享编辑器中持续沟通。
- 教会陌生读者并处理迁移任务。

## 2. 证据目录合同

```text
lab/evidence/
  environment.md
  baseline/
    session-01/
      prompt-source.md
      timeline.md
      original-code.java
      test-output.txt
      review.md
  problems/
    <problem-id>/
      card.md
      first-attempt.java
      solution.java
      tests.java
      test-output.txt
      reviews.md
  whiteboard/
    <session-id>/
      prompt-source.md
      recording-link.md
      original-code.txt
      compiled-code.java
      test-output.txt
      retrospective.md
  teach-back/
    <session-id>/
      lesson-version.md
      recording-link.md
      observer-notes.md
      learner-artifact.md
      revision.diff
  mocks/
    <session-id>/
      prompt-source.md
      recording-link.md
      code.java
      test-output.txt
      rubric.md
      interviewer-feedback.md
```

录像链接可以指向私有存储，但发布审计者必须能访问。不要把个人信息、平台受版权保护的完整题面或面试保密内容放入公开仓库。

## 3. Phase A：环境与基线

记录：

- JDK 版本、操作系统、编辑器和是否开启补全。
- 编译与运行命令。
- 三道未见题的来源、约束和固定时间。
- 每次提示的时间与具体内容。

基线输出只用于决定训练重点，不给求职能力下结论。

## 4. Phase B：96 题首次训练

九模块配额为 `12/16/10/8/14/10/8/12/6`。每题必须：

1. 闭卷开始并记录开始时间。
2. 写合同、朴素解和优化方向。
3. 保留 first attempt，不覆盖失败代码。
4. 编译并运行边界测试。
5. 写复杂度和正确性依据。
6. 标记 independent/hinted/reviewed/unfinished。
7. 安排下一次复习。

只有目录中存在对应证据卡和原始产物，题目才计入 attempted。只有未获得关键提示且测试通过，才可计入 independent pass。

## 5. Phase C：间隔复习

默认在 1/3/7/14/30 天进行闭卷复现。允许因日程延迟，但必须同时保留计划日和实际日。看旧代码后才重写记作 review，不记 recall pass。

每周生成：到期数、按时完成数、A/B/C/D 分布、逾期债务和主错因。没有真实数据时字段保持空或 Pending，禁止生成示例数字冒充结果。

## 6. Phase D：Java 测试

每个模块至少选择两题做自动化测试：

- 固定边界用例。
- 小规模朴素 oracle。
- 固定 seed 的随机差分测试。
- 首个反例保存与最小化。

推荐直接使用 JDK 21 编译运行；若引入 JUnit/JMH，必须锁定版本并记录依赖来源。复杂度趋势实验不能用单次计时支持生产性能结论。

## 7. Phase E：白板编码

至少三次 35-45 分钟会话，覆盖数组/窗口、树/图和 DP/综合。录制或逐分钟记录：澄清、方案、编码、测试、提示和修复。先保留白板原稿，再编译修复；不得用最终代码替换原稿。

## 8. Phase F：Teach-back 与陌生读者

完成六个必讲主题，其中至少三节由陌生读者静默测试。每个读者完成一个非原题变式。记录读者背景、任务耗时、停顿、错误、结果与原话，再保存讲义修订 diff。

至少一节必须形成：v1 测试未通过或暴露明显缺口 -> 修订 -> v2 复测。失败不是需要删除的记录。

## 9. Phase G：结构化模拟面试

完成三轮陌生题模拟，每轮 35-45 分钟，统一评分：

| 维度 | 分值 |
|---|---:|
| 澄清与样例 | 10 |
| 朴素解与优化推导 | 15 |
| 数据结构与不变量 | 20 |
| Java 实现 | 20 |
| 测试与修错 | 15 |
| 复杂度与正确性 | 10 |
| 沟通与协作 | 10 |

评分表必须记录关键提示。熟题、自选题或事先排练题可以作为训练，但不能算陌生模拟。

## 10. 自动检查建议

对证据目录运行检查，验证：

- 96 个槽位没有重复 problem id。
- 每个 attempted 项都有 first attempt、测试输出和状态。
- 复习事件日期单调且保留计划/实际日期。
- 看答案状态没有被统计为首次独立通过。
- 模拟分数能由各维度相加得到。
- 对外摘要中的数字与证据目录重新计算一致。

检查器尚未实现，状态为 Pending；在实现和运行前不能宣称自动审计通过。

## 11. 里程碑

| 里程碑 | 通过证据 | 当前状态 |
|---|---|---|
| M0 基线 | 环境 + 三题原始记录 | Pending |
| M1 基础结构 | 前 32 个首次槽位及到期复习 | Pending |
| M2 树图搜索 | 累计 64 个首次槽位及测试 | Pending |
| M3 完整覆盖 | 96 个首次槽位，无重复计数 | Pending |
| M4 延迟提取 | 到期复习记录和债务解释 | Pending |
| M5 教学验证 | 六次讲解、三次陌生读者测试 | Pending |
| M6 面试验证 | 三轮模拟及跨轮复盘 | Pending |

## 12. 发布门槛

- M0-M6 均有可访问的原始证据。
- Java 模板和选定题目在记录的 JDK 21 环境中编译、测试通过。
- 题量统计可从目录重新计算，复习不重复计入首次题量。
- 失败、提示和逾期没有从公开统计中删除。
- 陌生读者与模拟评审不是作者本人。
- 发布文案区分课程规划、实际完成、复习通过和模拟表现。

在满足全部条件前，本实验保持 Pending。

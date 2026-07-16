# 第 23 章实验：岗位、作品与发布迭代

## 当前状态

- 状态：Pending
- 已完成：实验矩阵、数据字段、评分和发布门禁设计
- 未完成：实时 JD 样本、真实投递批次、陌生读者完整试用和发布审计

## 实验矩阵

1. JD 采样、去重和关键词编码一致性。
2. 三类岗位匹配评分与人工复核。
3. 事实主简历到岗位版本的一致性 diff。
4. 简历 bullet 到 evidence index 的可追溯检查。
5. 两批投递的单变量实验与漏斗记录。
6. 面试反馈到课程章节的回写时间。
7. 作品 quickstart 的干净环境复现。
8. 链接、版本、版权、隐私和 Secret 扫描。
9. 陌生读者样章任务与可用性观察。
10. 全课程 Release Candidate 审计。

## 证据目录

```text
evidence/<experiment>/
  protocol.md
  sample-definition.md
  input.csv
  raw-output/
  decisions.md
  privacy-review.md
  conclusion.md
```

## 发布门槛

- 岗位结论有带日期的真实样本，且说明样本限制。
- 三类简历没有事实冲突或无证据数字。
- 作品可在干净环境按说明复现，或明确标注阻塞。
- 至少一名陌生学习者完成端到端试用并关闭阻断问题。
- 版权、隐私、Secret、版本、勘误和销售范围全部通过。
- 未满足以上条件时维持 Draft。

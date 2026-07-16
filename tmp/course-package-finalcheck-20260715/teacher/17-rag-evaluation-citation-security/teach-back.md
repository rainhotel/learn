# 第 17 章 Teach-back

## 5 分钟：为什么 RAG 不能只看答案

### 目标

让听众理解一个答案可能“说对了但没有证据”，也可能“忠实引用了错误版本”。

### 脚本

1. 30 秒：展示 query -> retrieval -> context -> generation -> citation 链路。
2. 60 秒：解释 Recall@k 只测证据是否被找回，不测回答。
3. 60 秒：对比 faithfulness 与 answer correctness。
4. 60 秒：说明引用需要 document/version/span/hash/ACL，不是一个 URL。
5. 60 秒：举无答案和权限不足必须拒答的例子。
6. 30 秒：总结平均分不能抵消跨租户泄露。

### 验收追问

- 一个正确但没有 context 支持的答案算不算通过？
- 一个带链接但引用错误版本的答案算不算通过？
- 为什么权限不足的拒答不能说出文档标题？

## 15 分钟：从评测集到发布门禁

### 结构

1. 评测对象：retrieval、generation、citation、refusal、security、cost。
2. 数据集：普通、hard negative、无答案、版本、ACL、注入和敏感样本。
3. 指标：Recall/MRR/nDCG、faithfulness/correctness、citation、拒答矩阵。
4. 方法：确定性检查、人工标注、校准 judge 的责任边界。
5. 门禁：硬安全失败、相对质量 diff、延迟/成本预算。
6. 证据：ranking、context、answer、引用 span、配置 hash 和人工仲裁。

### 演示

画两个 case：

- Case A：检索命中旧 Runbook，回答忠实但时态错误。
- Case B：tenant-a 的 query 语义接近 tenant-b 文档，但 ACL pre-filter 使其从未进入 context。

### 验收

- 能解释为什么 frozen test 不能用于日常调参。
- 能说明 judge 升级为什么必须重校准。
- 能给出一个不能被平均分抵消的硬门禁。

## 45 分钟：NotifyFlow RAG 评测与红队评审

### 0-5 分钟：业务风险

事故助手可能产生错误根因、错误引用、跨租户泄露和高风险建议。明确它只读，不执行 replay 或扩容。

### 5-12 分钟：数据合同

逐字段解释 query、identity、asOf、gold/forbidden evidence、expected behavior、tags、risk、annotation。展示如何按 incident/document family 防 split 泄漏。

### 12-20 分钟：指标手算

现场手算一个 Recall@k、MRR 和 nDCG；把回答拆 claim，计算 citation completeness；画拒答混淆矩阵。

### 20-28 分钟：Java 流水线

画 DatasetValidator、Snapshot、Retrieval/Generation Runner、Deterministic Checks、Judge、Human Review、Gate 和 Report。解释 configHash、幂等续跑和失败 case 不静默丢弃。

### 28-36 分钟：安全红队

演示间接注入、跨租户 canary、伪造 citation、旧版本冲突和成本耗尽五类样本。逐层检查 ranking、context、模型请求、输出和审计。

### 36-42 分钟：回归与 Pareto

比较 baseline/candidate 的 tag diff、硬失败、P95、token 和成本。解释为什么平均质量上升仍可能拒绝发布。

### 42-45 分钟：边界

明确本章当前所有实验 Pending；没有数据集、Runner、原始结果和人工复核前，不展示虚构指标。

## 60 分钟实验课建议

```text
10 分钟：标注 5 条 case 并讨论分歧
10 分钟：手算检索和拒答指标
10 分钟：拆 claim 与核验引用 span
15 分钟：红队桌面推演
10 分钟：设计回归 gate
5 分钟：记录失败 taxonomy 和下一步
```

答案在学习者先提交标注和计算后再开放。

## 常见讲解失败

- 把 RAGAS 或某个 judge 框架当成完整评测答案。
- 只讲公式，不解释 gold 不完整、权限和版本会怎样破坏指标。
- 说“引用准确”但无法复取具体 span。
- 用 prompt 防注入，忽略 ACL、输出验证和审计。
- 把未运行的门禁写成安全已通过。

## 试讲评分

| 维度 | 分值 |
|---|---:|
| 层次拆分与指标边界 | 20 |
| 数据集和人工标注 | 15 |
| 引用与拒答 | 20 |
| 安全红队 | 20 |
| Java/回归工程 | 15 |
| 事实与实验边界 | 10 |

听众能独立判断“错在检索、生成、引用还是权限”，并能写出一个安全硬门禁，才算 Teach-back 达标。

# 第 20 章实验：事实台账、三版简历与逐句压力测试

## 当前状态

- 状态：Pending
- 类型：文档实验 + 人工核验 + 可解析性检查 + 陌生评审
- 已完成：实验步骤、表结构、断言、评分和证据目录设计
- 未完成：导入学习者真实材料、JD 采样、三版成稿、PDF 解析、同伴压力测试和真实投递反馈

## 实验目标

将学习者所有求职 claim 建成一个可审计数据集，并证明：

1. 每条 claim 有唯一来源和证据。
2. 大烨实习没有混入独立项目技术。
3. 三版简历事实一致，只改变选择、排序和表达重点。
4. 所有数字都有定义、环境和原始材料。
5. 最终 PDF 可阅读、可提取、可搜索。
6. 每条投递 claim 可以通过逐句技术追问。

## 实验输入

- 原始简历和历史版本。
- 合法可用的实习证明、任务记录、周报和提交信息。
- 独立项目仓库、实验报告、运行输出、架构文档和演示。
- 当期目标 JD 原文、URL、采样日期和岗位类型。
- 教育、奖项、开源贡献等可核验材料。

敏感材料不直接复制到公开仓库；证据索引可记录脱敏摘要、保管位置和核验人。

## 实验阶段

### Phase A：事实原子化

1. 从所有旧简历提取 claim。
2. 将复合句拆成原子事实。
3. 填写 `fact-ledger.csv`。
4. 标记 source_type、confidence、confidentiality 和 forbidden_wording。
5. 单独生成大烨实习技术使用清单。

断言：任何 D 级事实和无法确认的技术都不能进入投递版。

### Phase B：证据索引

为每项证据记录：

```text
evidence_id:
fact_ids:
type:
created_at:
environment:
location:
hash_or_version:
what_it_proves:
what_it_does_not_prove:
confidentiality:
reviewer:
```

断言：证据必须说明“不证明什么”，避免由代码提交推断生产影响。

### Phase C：JD 关键词矩阵

1. 至少采样 15 条目标 JD。
2. 每条保存公司、岗位、链接、日期、城市和原文摘要。
3. 统计 Java 后端、Agent 后端、制造业数字化三类关键词。
4. 关键词关联 fact_id；无证据词进入 gap_action。

断言：没有真实样本时不得声称市场频次或 ATS 结论。

### Phase D：同源生成三版简历

从 master resume 生成：

- Java 后端版。
- Agent 后端版。
- 先进制造数字化版。

运行一致性审计：公司、时间、角色、项目归属、技术事实、结果和数字不得冲突。

### Phase E：可读性与解析

1. 导出 PDF。
2. 使用 PDF 文本提取工具读取全文。
3. 检查标题、时间、bullet 和链接顺序。
4. 搜索目标关键词并核对上下文。
5. 在不同阅读器中检查字体、换行、链接和页数。

断言：视觉正常但提取错序视为失败。

### Phase F：逐句压力测试

由至少一名未参与写作的人随机抽取十条 claim，按六层追问。记录 Green、Yellow、Red 和修订。

必须抽到：

- 大烨实习。
- 独立项目。
- 一个量化数字。
- 一项 Agent/RAG 能力。
- 一项尚处于设计或 Pending 的内容。

### Phase G：版本与投递反馈

每次投递记录：

```text
resume_version, target_job, jd_capture_date,
selected_claim_ids, sent_at, outcome,
interview_questions, weak_claims, revision_reason
```

投递结果不能简单归因于某个关键词或版式；只能记录相关性和后续实验。

## 自动与人工检查

### 可自动检查

- 每个投递 claim 是否有 `claim_id`。
- `claim_id` 是否关联 fact 和 evidence。
- 三版中的公司、时间、数字是否冲突。
- 禁用词和未经批准的技术是否出现于大烨实习段落。
- PDF 提取文本是否为空、错序或缺段。
- URL 是否为预期公开入口。

### 必须人工检查

- 本人贡献与团队贡献是否准确。
- STAR/CAR 因果是否成立。
- 数字是否能证明 claim。
- 是否泄露保密或个人信息。
- 是否能回答技术取舍、失败和反事实。

自动扫描通过不能替代人工事实核验。

## 证据目录

```text
evidence/resume-audit/<version>/
  input-manifest.md
  fact-ledger.csv
  claim-evidence-map.md
  jd-keyword-matrix.csv
  consistency-report.md
  pdf-extraction.txt
  redaction-review.md
  pressure-test.md
  revision-diff.md
  conclusion.md
```

涉及隐私或公司保密的原始材料只记录受控位置和脱敏摘要，不提交公开仓库。

## 发布门槛

- 三版简历全部来自同一事实台账。
- 大烨实习技术迁移审计为零违规。
- 所有量化 claim 至少达到课程第 5 级证据并注明环境。
- 所有投递 claim 为 Green；没有 Red，Yellow 不进入投递版。
- PDF 文本提取和人工阅读均通过。
- 至少两名陌生读者审阅，至少三次模拟面试并完成修订。
- 有至少一轮真实投递反馈记录。
- 完成敏感信息、公司保密和版权检查。

在上述条件真实满足前，不得标记 Lab Verified、Release Candidate 或 Released。


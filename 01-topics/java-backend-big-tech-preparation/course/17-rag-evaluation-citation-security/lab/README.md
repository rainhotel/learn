# 第 17 章实验：RAG 评测、引用、安全与回归

## 当前状态

- 状态：Pending
- 已完成：实验合同、矩阵、断言、证据结构、停止条件和发布门禁设计
- 未完成：评测语料、Java Runner、检索/模型服务、人工标注、红队和线上 shadow 真实运行

本目录没有可声称通过的指标。下面的命令接口和文件结构是实现目标，不代表当前已有可执行程序。

## 一、实验目标

1. 建立版本化、可授权、可仲裁的离线评测集。
2. 分别测量 retrieval、context、generation、citation 和 refusal。
3. 证明 ACL 在候选、context、模型请求和输出全链路生效。
4. 用红队样本验证 prompt injection、跨租户和敏感数据控制。
5. 校准自动 judge，并把高风险/分歧样本送人工复核。
6. 比较 baseline/candidate 的质量、延迟、token 和成本。
7. 用硬门禁阻断安全失败，用版本化策略控制发布。

## 二、前置条件

- Java 21 和仓库约定的 Maven Runner。
- 可固定版本的检索服务、reranker、生成模型和 judge。
- 只含合成或已授权脱敏数据的独立测试语料。
- 至少两个测试租户、独立 ACL 和合成敏感 canary。
- 模型端点已获准处理该测试数据，Secret 通过运行时环境注入。
- 评测总请求数、并发、token、成本和持续时间预算已审批。

任何前置条件不满足时，只能做静态评审，不能填写运行结果。

## 三、目标目录

```text
lab/
  datasets/
    dataset-card.md
    annotation-guide.md
    development.jsonl
    validation.jsonl
    frozen-test.jsonl
    red-team.jsonl
  configs/
    baseline.yaml
    candidate.yaml
    gate-policy.yaml
  evidence/<run-id>/
    environment.md
    manifest.json
    dataset-validation.json
    corpus-snapshot.json
    retrieval.jsonl
    context.jsonl
    answers.jsonl
    deterministic-results.jsonl
    judge-results.jsonl
    human-review.csv
    metrics.json
    latency-cost.csv
    failure-cases.md
    regression-diff.md
    gate-decision.md
    conclusion.md
```

这些目录和 artifact 需由后续实现生成，当前不可视为已存在证据。

## 四、Runner 命令合同

未来 Java Runner 应提供等价接口：

```powershell
mvn.cmd -q -pl rag-eval test
mvn.cmd -q -pl rag-eval exec:java -Dexec.args="validate --dataset datasets/frozen-test.jsonl"
mvn.cmd -q -pl rag-eval exec:java -Dexec.args="run --config configs/candidate.yaml --dataset datasets/frozen-test.jsonl"
mvn.cmd -q -pl rag-eval exec:java -Dexec.args="compare --baseline evidence/baseline --candidate evidence/candidate --policy configs/gate-policy.yaml"
```

当前仓库尚无 `rag-eval` 模块，上述命令不得执行后标成通过。实现时可以调整 CLI，但必须保留 validate、run、compare 三类能力和等价证据。

## 五、实验矩阵

| 编号 | 实验 | 主要变量 | 核心断言 | 状态 |
|---|---|---|---|---|
| E01 | 数据合同与 split 泄漏 | schema、document family、版本 | ID 唯一、gold 可复取、集合无家族泄漏 | Pending |
| E02 | 人工标注一致性 | 标签指南、标注者 | 保存独立标签、分歧和仲裁 | Pending |
| E03 | 检索基线 | BM25/vector/hybrid、top-k | Recall/MRR/nDCG 可复算，无越权候选 | Pending |
| E04 | Rerank/context 消融 | reranker、budget、去重 | 多跳覆盖提高时不引入 stale/forbidden evidence | Pending |
| E05 | 生成与 faithfulness | prompt、模型、temperature | claim 可拆、unsupported claim 可定位 | Pending |
| E06 | 引用验证 | span/hash/version/ACL | 每条引用可复取且属于本次 context | Pending |
| E07 | 拒答 | evidence 阈值、行为策略 | 分开 no-evidence/access/conflict，报告混淆矩阵 | Pending |
| E08 | ACL/跨租户 canary | identity、租户、精确/语义查询 | ranking/context/request/output 均无 forbidden evidence | Pending |
| E09 | Prompt injection 红队 | 文档、metadata、日志、隐藏文本 | 恶意内容不改变权限、schema 和动作边界 | Pending |
| E10 | 敏感数据泄露 | canary、PII/Secret pattern | 模型请求、回答、引用、日志和报告无未授权明文 | Pending |
| E11 | Judge 校准 | judge 模型/prompt、标签 | 与人工 gold 比较，分层报告偏差 | Pending |
| E12 | Java Runner 恢复 | 并发、中断、429、timeout | case 不丢失，按 configHash 幂等续跑 | Pending |
| E13 | 质量/延迟/成本 Pareto | top-k、rerank、model、context | 同一快照对照，报告 P95/token/query cost | Pending |
| E14 | 回归门禁 | baseline、candidate、policy | 硬失败不可被平均分抵消 | Pending |
| E15 | Shadow/canary | 流量比例、自动停止 | 不展示 shadow 输出，canary 可回滚且隐私合规 | Pending |

## 六、E01：数据合同与泄漏检查

### 步骤

1. 固定 dataset version 和 corpus/ACL snapshot。
2. 校验 caseId 唯一、必填字段、expected behavior 和风险标签。
3. 复取所有 gold/forbidden document/version/span 并验证 hash。
4. 按 incident/document family 检查跨 split 重复和近重复。
5. 扫描真实 Secret、手机号、身份证、token 和未授权生产标识。
6. 输出每类样本数、来源、授权和已知偏差。

### 停止条件

- 任一 gold 无法复取或 ACL 不明确。
- frozen test 与 development 存在同 family 泄漏。
- 发现真实 Secret、未授权生产数据或不可再分发正文。

### 证据

`dataset-validation.json`、数据卡、重复 case 列表、敏感扫描摘要和人工处置记录。

## 七、E02：人工标注与 Judge 校准

### 步骤

1. 两名标注者独立标注相同分层样本，不共享答案。
2. 计算标签级一致性并保留原始分歧。
3. 由领域专家仲裁，更新 annotation status，不覆盖原始标签。
4. 在仲裁集上运行固定 judge，计算每类混淆矩阵。
5. 抽查高风险、门槛附近和 judge/人工分歧。

### 断言

- 每个 gold 标签可追溯到标注者、时间、指南版本和仲裁理由。
- Judge 只评它获得的 claim/context，不判断 ACL 或真实业务状态。
- Judge 模型/prompt 变化会创建新校准 run。

## 八、E03-E04：检索、Rerank 与 Context

### 基线配置

至少比较 BM25、vector、hybrid 和 hybrid + rerank。每次保持 corpus snapshot、query set、ACL、硬件/region 和并发一致。

### 保存内容

每个 case 保存过滤前后合法范围内的 ranking、score、rank、document/version/chunk、retrieval path、rerank score、context 选择/丢弃原因和耗时。禁止保存越权正文以便“调试”。

### 断言

- 指标可以从 `retrieval.jsonl` 重新计算。
- exact token、multi-hop、hard negative、stale 和 access-denied 分层报告。
- ACL violation/forbidden hit 为硬失败，不参与平均质量抵消。
- Context 截断、重复 chunk 和版本冲突均有明确分类。

## 九、E05-E07：生成、引用与拒答

### 生成实验

固定 ranking/context 后比较 prompt 或模型，避免检索变化混入生成结论。保存 prompt hash、context manifest、原始响应、解析结果、token 和分段延迟。

### 引用实验

对每个 citation：

1. 以 case identity 重新鉴权。
2. 复取 document/version/span，校验 hash。
3. 检查 chunk 是否在本次 context manifest。
4. 将 answer 拆 claim，评估 entailment 和 completeness。
5. 对不存在、越权、错版本或错 span 的引用硬失败。

### 拒答实验

分别运行 ANSWER、NO_EVIDENCE、ACCESS_DENIED、CONFLICT 样本。检查拒答不会透露受限文档存在性；计算混淆矩阵而非只报总拒答率。

## 十、E08-E10：安全红队

### 隔离要求

- 使用独立测试租户和合成 canary，不连接生产索引。
- 攻击文本不得包含真实恶意 URL、Secret 或个人信息。
- 模型/检索端点有总请求、token、成本和时间上限。
- 任何跨租户或敏感输出立即停止整次 run，保留最小必要证据并启动处置。

### 跨租户断言

```text
retriever candidate: no forbidden document ID
context manifest: no tenant-b chunk/hash
model request: no tenant-b content/canary
answer/citation: no tenant-b fact/canary
cache/audit/report: no unauthorized plaintext
```

### Prompt injection 断言

恶意文档不得改变 identity、ACL、回答 schema、引用要求和禁止高风险动作；不得访问未提供的数据源或输出外部发送指令。

### 泄露断言

扫描模型请求、响应、结构化日志、Trace attributes、评测 artifact 和截图。检测器命中后由人工确认，避免仅靠正则判断安全结论。

## 十一、E12：Java Runner 中断恢复

### 故障点

- 数据集校验后退出。
- 检索完成一半时终止进程。
- 模型端点返回 429/timeout。
- Judge 完成但报告尚未聚合时退出。
- Candidate 配置变化后尝试复用旧 run。

### 断言

- 预期 case 数与最终状态数一致，失败不会从分母消失。
- 已完成阶段按 `(runId, caseId, stage, configHash)` 不重复计费执行。
- 配置 hash 改变时拒绝污染旧 run，创建新 run。
- 重试次数、退避、错误和费用均记录。
- 最终报告按 caseId 稳定排序，不受并发完成顺序影响。

## 十二、E13-E14：Pareto 与回归门禁

### 配置矩阵

先固定 baseline，再一次只改一个变量：top-k、rerank、context budget、生成模型或输出长度。每个配置运行同一 frozen snapshot；若模型有波动，按预先定义次数重复，而不是只挑最好一次。

### 报告

- 总体和 tag-level Recall/MRR/nDCG。
- faithfulness/correctness/citation/refusal。
- 所有安全硬失败。
- retrieval/rerank/model/verify P50/P95/P99。
- token、query cost、run cost 和人工复核量。
- case-level improvement/regression 与失败 taxonomy。

### 门禁顺序

1. 检查 artifact 和 case 完整性。
2. 检查安全硬门禁。
3. 检查高风险 tag 下界。
4. 检查总体相对质量。
5. 检查延迟、错误、token 和成本预算。
6. 记录人工风险接受或拒绝，禁止静默降低策略。

## 十三、E15：Shadow 与 Canary

### Shadow

- 候选输出不返回用户，不触发 Tool。
- 输入发送仍需满足租户、隐私和模型 Provider 政策。
- 对照 baseline/candidate 的质量代理、延迟、token 和错误。
- 不保存不必要的原始 payload。

### Canary

- 只进入已授权的小比例用户/租户。
- 预设跨租户/敏感输出、错误、P95、成本和拒答异常的停止条件。
- 一键回到 baseline，并保存版本/时间/流量和回滚证据。
- 严重失败人工复核后，脱敏并审批进入 red-team/development。

## 十四、统一实验时间线

```text
T-30m  固定版本、预算、语料/ACL snapshot 和基线健康
T-10m  校验 case 数、授权、Secret/PII 扫描和停止开关
T0     启动 run，记录 runId/configHash
T+...  采集 ranking/context/answer/metrics，不临时改配置
Tend   校验 artifact 完整性、硬安全门禁和成本上限
T+1h   人工复核高风险与分歧样本
T+1d   输出 diff、gate decision、限制和后续修复
```

## 十五、证据审核清单

- 日期、操作者、代码 commit、JDK/依赖和模型服务版本。
- dataset/corpus/ACL/ingestion snapshot 与授权证明。
- 运行命令、配置、环境、并发、timeout、重试和预算。
- 每 case ranking、context、answer、citation 和错误状态。
- 人工原始标签、仲裁、judge 校准和复核队列。
- 质量、安全、延迟、token、成本和失败分布。
- baseline/candidate diff、门禁版本、审批和结论边界。

## 十六、Lab Verified 门槛

必须同时满足：

- Java Runner 在固定环境真实运行，命令和原始输出可复核。
- 冻结集有数据卡、双标/仲裁和 split 泄漏检查。
- 指标可由保存的 ranking/context/answer 独立复算。
- 引用可按 identity/version/span/hash 复取。
- ACL、跨租户、注入和敏感 canary 完成真实攻击运行。
- Judge 已与人工 gold 校准，高风险分歧已复核。
- 回归门禁、延迟/成本和失败 case 完整报告。
- 没有把静态设计、框架自带 demo 或模型自评当成运行验证。

当前以上条件均未完成，本章实验状态保持 Pending。

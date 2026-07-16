# 第 17 章讲义：把 RAG 从演示变成可发布系统

## 学习目标

RAG Demo 常用几个顺利问题展示“能回答”。生产评测必须回答更难的问题：相关证据是否被召回、无权限内容是否从未进入上下文、回答的每个事实是否得到正确版本支持、没有答案时是否拒答、升级模型后安全和成本是否回归。

本章建立一条独立于在线服务的评测控制面。被测系统可以变化，但数据集、版本、指标、原始结果、人工仲裁和发布决策必须可追溯。

## 一、先定义评测对象

RAG 不是一个单分数模型，而是多个可失败阶段：

```text
query + identity + time
-> query transform
-> ACL/filter
-> keyword/vector retrieval
-> fusion/rerank
-> context selection
-> generation
-> citation mapping
-> policy/output validation
```

评测至少拆成：

- retrieval：正确证据是否进入候选，排序是否合理。
- context：最终送入模型的证据是否相关、完整、授权且未过期。
- generation：答案是否正确、相关、完整并忠于上下文。
- citation：claim 能否定位到真正支持它的证据。
- refusal：无答案、权限不足或证据冲突时是否正确拒答。
- security：是否泄露、服从恶意文档或绕过 ACL。
- operations：延迟、token、成本、错误率和可重复性。

只报“整体准确率”会隐藏故障发生在哪一层。

## 二、评测集是产品合同

### 2.1 EvaluationCase 数据合同

建议每条样本使用 JSONL，并包含：

```json
{
  "caseId": "nf-incident-0001",
  "datasetVersion": "2026-07-15.1",
  "query": "邮件渠道从 10:05 开始积压的首个证据是什么？",
  "identity": {"tenantId": "tenant-a", "roles": ["oncall-read"]},
  "asOf": "2026-07-15T10:20:00Z",
  "expectedBehavior": "ANSWER",
  "goldEvidence": [
    {"documentId": "inc-42", "version": "7", "spanId": "timeline-10:05", "grade": 3}
  ],
  "referenceClaims": ["10:05 时 Provider timeout 率先上升"],
  "forbiddenEvidence": ["tenant-b-private-runbook"],
  "tags": ["timeline", "hard-negative", "versioned"],
  "riskLevel": "HIGH",
  "annotationStatus": "ADJUDICATED"
}
```

示例只定义合同，不代表该样本已存在或已标注。

### 2.2 样本类型

冻结集不能只收录“一个问题对应一个段落”：

- direct：单段直接命中。
- paraphrase：同义和口语表达。
- multi-hop：需要组合两个以上授权证据。
- exact token：错误码、类名、订单号、配置键，考验关键词召回。
- hard negative：字面相似但租户、版本、渠道或时间错误。
- no-answer：语料中不存在答案。
- access-denied：答案存在但当前身份无权读取。
- stale/conflict：旧版本或多个来源冲突。
- long-context：相关证据被大量相似内容包围。
- injection：文档或日志含试图改变系统规则的指令。
- sensitive：证据含手机号、token、Secret 形态或受限 payload。

### 2.3 数据划分和污染

- development：允许调 chunk、检索、prompt 和阈值。
- validation：用于选择方案，减少对测试集反复拟合。
- frozen test：发布门禁使用，严格限制查看和修改。
- challenge/red-team：安全和极端失败，允许持续追加但要版本化。
- online replay：来自脱敏真实失败，经审批后进入候选集。

同一文档的相邻 chunk、同一事故的改写问题不能跨集合泄漏。时间切分可验证新版本和新事故的泛化。模型训练语料不可完全知晓时，要把“可能污染”写入数据卡，优先评测引用和权限，而非只看世界知识答案。

## 三、检索指标

### 3.1 Recall@k 与 Hit@k

```text
Recall@k = top-k 中相关证据数 / 该问题全部相关证据数
Hit@k = top-k 是否至少有一个相关证据
```

Hit@k 适合“任一证据足够”的任务；多证据问题要看 Recall@k 或按 claim 计算覆盖。gold evidence 本身不完整时，Recall 也会误导。

### 3.2 MRR

```text
ReciprocalRank = 1 / 第一个相关结果的名次
MRR = 所有问题 ReciprocalRank 的平均
```

MRR 强调首个相关结果，不奖励后续多个相关证据。若生成需要多段材料，MRR 不能单独代表 context 质量。

### 3.3 nDCG

当证据有“关键、辅助、弱相关”等等级时：

```text
DCG@k = Σ (2^rel_i - 1) / log2(i + 1)
nDCG@k = DCG@k / IdealDCG@k
```

nDCG 同时考虑相关等级和位置，但标注成本更高，等级定义必须统一。

### 3.4 安全和版本指标

检索评测必须额外报告：

- ACL violation rate：任何越权候选进入 retriever/context 的比例。
- stale evidence rate：错误版本或超出 `asOf` 的证据比例。
- tenant isolation failure：跨租户证据出现次数。
- context precision：最终 context 中真正有用的 chunk 比例。
- claim evidence coverage：每个参考 claim 是否至少有一条授权证据。

ACL 泄露不能被高 Recall 抵消。对于安全门禁，它通常是零容忍类别，而不是参与加权平均。

## 四、生成指标：正确不等于忠实

### 4.1 Faithfulness / Groundedness

将回答拆成可验证 claim，判断每条 claim 是否由实际提供给模型的 context 支持：

```text
faithfulness = supported_answer_claims / verifiable_answer_claims
```

建议把纯语气、建议和无法外部验证的句子单独分类，避免分母随 judge 任意变化。

### 4.2 Answer correctness

比较回答与参考事实、结构化真值或人工判定是否一致。它可拆成：

- factuality：事实值是否正确。
- completeness：必要要点是否覆盖。
- contradiction：是否与 gold 或权威状态冲突。
- temporal correctness：是否使用正确时间和版本。

一个答案可能忠实于错误的过期文档，因此 faithfulness 高但 correctness 低。答案也可能依赖模型记忆说对了，但 context 没有证据，因此 correctness 高而 faithfulness 低。生产 RAG 通常两者都要满足。

### 4.3 Answer relevance

判断是否直接回应用户问题，而不是复述全部 context。相关性高不代表正确；一个简洁但错误的答案仍然失败。

### 4.4 结构与业务规则

NotifyFlow 事故答案还需要确定性检查：

- 时间窗、渠道、租户和任务范围是否保留。
- 数字是否带单位和聚合口径。
- 结论、证据、不确定性和下一步只读查询是否分开。
- 是否错误建议 replay、清队列、改 RBAC 或输出 Secret。

这些规则优先用代码验证，不必交给 LLM judge。

## 五、引用是 claim-evidence 图

### 5.1 引用合同

每条引用至少保存：

```text
citationId
answerClaimId
documentId + documentVersion
chunkId + spanStart/spanEnd
sourceUri/page/section
contentHash
tenantId/ACL policy version
ingestionRunId
retrievalRank/rerankScore
```

展示层的 `[1]` 只是引用编号；后台必须能复取同一版本、定位同一 span、验证 hash 和访问权限。

### 5.2 引用指标

- citation entailment/precision：被引用证据是否真的支持关联 claim。
- citation completeness/recall：需要证据的 claim 是否都有引用。
- citation validity：文档、版本、span、hash 是否可解析且未被删除/替换。
- citation placement：引用是否附着在正确 claim，而不是统一堆在段末。
- source quality：是否引用当前任务认可的权威来源。

一条证据可以支持多个 claim，一个 claim 也可能需要多条证据。不要简单按“回答里有几个链接”计分。

### 5.3 确定性验证顺序

1. 校验 citation schema 和引用 ID 唯一。
2. 以 identity + ACL 重新授权，而不是沿用模型输出。
3. 按 document/version/span 复取原文并验证 content hash。
4. 确认该证据确实进入本次 context。
5. 再由人工或校准后的 judge 判断 claim 是否被证据蕴含。

模型不能引用没有进入上下文的文档来补充可信感。

## 六、拒答评测

### 6.1 四种典型行为

- ANSWER：授权证据充分且一致。
- ABSTAIN_NO_EVIDENCE：语料中无足够证据。
- ABSTAIN_ACCESS_DENIED：可能存在证据，但当前身份无权访问；不能暗示敏感内容。
- ABSTAIN_CONFLICT：权威来源冲突或版本不明，需要人工确认。

### 6.2 混淆矩阵

将“应回答/应拒答”与“实际回答/实际拒答”形成混淆矩阵：

- refusal precision：系统拒答时，有多少确实应该拒答。
- refusal recall：所有应拒答样本中，有多少被正确拒答。
- answer coverage：可回答样本中有多少被回答。
- unsafe answer rate：应拒答却给出实质答案的比例。

只提高拒答率会让系统安全但无用；只提高覆盖率会增加幻觉和泄露。阈值应按风险等级分层。

### 6.3 拒答文案也要安全

权限不足时不要说“tenant-b 的 Runbook 明确写了……但你无权查看”，这本身泄露存在性与内容。安全拒答只说明当前授权上下文不足，并给出合法的申请或只读查询路径。

## 七、人工标注与 judge 校准

### 7.1 标注指南

每个标签都要有定义、正例、反例和边界：

- relevant 的粒度是 chunk、span 还是 document。
- claim 如何拆分，数字和因果是否分开。
- “部分支持”“冲突支持”“仅背景相关”如何分级。
- 过期或越权证据即使内容正确是否计为相关。
- 无答案、权限不足和冲突如何区分。

### 7.2 双标与仲裁

高风险冻结集建议至少两名标注者独立标注。记录原始分歧，计算 Cohen's kappa 或适合标签类型的一致性指标，再由第三人/领域专家仲裁。低一致性通常意味着任务或指南不清晰，不能只责怪标注者。

### 7.3 LLM-as-a-judge

Judge 可降低规模化成本，但存在位置偏差、长度偏差、自偏好、提示敏感和版本漂移。使用规则：

- 先与人工 gold 对照，报告各类别 precision/recall，而非只报相关系数。
- 固定 judge 模型、版本、prompt、temperature 和解析器。
- 优先让 judge 输出结构化标签和证据，而不是模糊总分。
- 对安全、引用和接近门槛的样本进行人工复核。
- judge 升级必须重跑校准集，旧分数不能直接横向比较。

Judge 不能验证它未获得的真实业务状态，也不能替代 ACL 和 Secret 检测器。

## 八、失败 taxonomy 与误差分析

每次评测失败应进入可操作分类：

```text
DATASET_GOLD_INCOMPLETE
QUERY_REWRITE_LOST_CONSTRAINT
ACL_FILTER_MISSING
KEYWORD_MISS
SEMANTIC_MISS
RERANK_WRONG_ORDER
CONTEXT_TRUNCATED
STALE_VERSION
GENERATION_UNSUPPORTED_CLAIM
GENERATION_MISREAD_EVIDENCE
CITATION_WRONG_SPAN
SHOULD_ABSTAIN
PROMPT_INJECTION_FOLLOWED
SENSITIVE_DATA_LEAKED
JUDGE_DISAGREEMENT
SYSTEM_TIMEOUT_OR_ERROR
```

先看失败分布和高风险 case，再优化平均分。若 gold 不完整，盲目调 retriever 会把评测噪声学进去。

## 九、离线评测流水线

```text
dataset contract validation
-> materialize authorized corpus snapshot
-> run retrieval and save full ranking
-> build exact context snapshot
-> run generation
-> deterministic schema/ACL/citation/PII checks
-> retrieval metrics
-> calibrated judge + human review queue
-> latency/token/cost aggregation
-> baseline diff and regression gates
-> signed report + failure cases
```

每次 run 固定：代码 commit、数据集、语料快照、ingestion、chunk、embedding、索引参数、reranker、prompt、生成模型、judge、硬件/服务区域和并发。

### 9.1 可重复性边界

外部模型服务可能更新且非完全确定。保存请求参数、响应、provider request id 和时间；对确定性规则复算，对模型 judge 接受有限波动并使用置信区间/重复样本。不能把单次偶然分数作为门禁基线。

## 十、Java 21 评测流水线设计

### 10.1 核心类型

```java
public record EvaluationCase(
        String caseId,
        String datasetVersion,
        String query,
        Identity identity,
        Instant asOf,
        ExpectedBehavior expectedBehavior,
        List<GoldEvidence> goldEvidence,
        Set<String> forbiddenEvidence,
        Set<String> tags) {}

public record RetrievedChunk(
        String documentId,
        String version,
        String chunkId,
        double score,
        int rank,
        AccessDecision accessDecision,
        String contentHash) {}

public record AnswerResult(
        String answer,
        List<Claim> claims,
        List<Citation> citations,
        Usage usage,
        Duration latency) {}
```

这些是接口草案，尚未编译或运行。

### 10.2 组件责任

- `DatasetValidator`：schema、ID 唯一、split 泄漏、gold 引用和敏感字段检查。
- `CorpusSnapshotResolver`：按 run 固定文档版本与 ACL policy。
- `RetrievalRunner`：保存未截断 ranking、过滤决策和耗时。
- `GenerationRunner`：保存 context、prompt hash、模型响应和 usage。
- `DeterministicEvaluator`：Recall/MRR/nDCG、schema、ACL、hash、PII 和预算。
- `JudgeEvaluator`：批量调用固定 judge，重试只处理安全的传输错误。
- `HumanReviewQueue`：抽样、门槛附近、高风险和 judge 分歧进入人工复核。
- `RegressionGate`：比较 baseline，输出 pass/fail 和具体 case diff。
- `ReportWriter`：生成 JSON、CSV、Markdown 和证据清单。

### 10.3 并发和失败

Java Runner 可用有界线程池或结构化并发组织 case，但必须受模型/检索服务 rate limit、token 和总成本预算约束。单 case 失败写入明确错误，不应让整批静默缺样；超时和 429 使用有限退避，生成请求不能无限重试并重复计费。

结果以 append-only run event 或每 case 独立文件保存，进程恢复时按 `(runId, caseId, stage, configHash)` 幂等续跑。并发完成顺序不影响最终按 caseId 排序的报告。

## 十一、回归门禁

门禁分为三类：

### 11.1 硬门禁

- 出现跨租户证据、ACL 绕过、Secret/PII 泄露。
- 引用指向不存在/未授权版本。
- 高风险攻击样本服从恶意指令并输出受限信息。
- 评测样本缺失、配置未记录或 judge 未校准。

硬门禁不允许由其他平均分抵消。

### 11.2 相对门禁

与冻结 baseline 比较 Recall、nDCG、faithfulness、answer correctness、citation completeness 和 refusal。阈值应基于重复运行波动和风险确定，可表达为“不得低于 baseline 的容忍下界”，而不是复制其他项目的百分比。

### 11.3 预算门禁

限制 P95/P99 端到端延迟、检索/rerank/生成分段延迟、输入/输出 token、每 query 成本、错误率和评测总预算。质量提升若成本或延迟不可接受，应进入 Pareto 评审，而不是自动发布。

门禁配置本身需要版本、所有者、变更理由和审批，禁止为了让某次发布通过而临时降低阈值不留记录。

## 十二、线上评测

离线集覆盖已知问题，线上观测发现真实分布变化：

- 系统：请求成功、TTFT、总延迟、timeout、429、token 和成本。
- 用户：追问/改写、拒答后离开、反馈、引用展开、人工升级。
- 质量代理：有引用回答比例、引用解析失败、无证据回答、检索零命中。
- 安全：policy deny、跨租户 canary、敏感输出拦截、攻击样本命中。
- 数据：新文档 ingestion 延迟、旧版本命中、ACL 变更传播和索引新鲜度。

用户点赞不是 ground truth，引用点击也不证明引用支持 claim。高风险失败应进入人工复核；脱敏后再进入在线 replay 候选集。

### 12.1 Shadow、Canary 与 A/B

- shadow：同一授权输入运行候选系统但不展示，比较质量、延迟和成本；仍要遵守数据和模型发送权限。
- canary：小比例真实用户，设置自动停止和回滚条件。
- A/B：比较用户结果，但必须控制流量、时间、用户群和学习效应。

安全修复不应只依赖普通 A/B 显著性；已知泄露风险优先阻断。

## 十三、Prompt injection 与数据泄露

### 13.1 攻击面

- 直接注入：用户要求忽略规则、泄露其他租户或伪造引用。
- 间接注入：Runbook、工单、日志、网页或 PDF 内嵌恶意指令。
- metadata 注入：标题、作者、URL 或 ACL 字段被恶意控制。
- Tool/result 注入：外部结果诱导模型执行额外动作；本章虽不执行 Tool，仍需防止生成越权建议。
- 跨会话/缓存：context、response cache 或日志错误复用到另一租户。

### 13.2 防御分层

1. ingestion：来源信任级、恶意内容标记、敏感字段处理，不把清洗当完美防线。
2. retrieval：identity-bound pre-filter，必要时物理/逻辑索引隔离。
3. context：最小化字段，明确数据与指令边界，保留来源和信任级。
4. generation：系统策略固定，禁止文档改变权限；输出结构化 claim/citation。
5. validation：重新授权引用、Secret/PII 扫描、动作/链接 policy。
6. operations：审计、速率/成本限制、canary 文档、告警和 kill switch。

仅靠 prompt 写“不要泄露”不构成安全控制。

### 13.3 ACL 时机

权限过滤应尽可能发生在候选召回前，防止越权 chunk 进入 rerank、context、模型服务或日志。若底层 ANN 过滤能力有限，应使用租户分区、允许集合或安全索引策略，而不是先全局 top-k 后静默删除。context 构建和引用返回时再次授权，形成纵深防御。

## 十四、成本与延迟联合评测

端到端延迟拆分：

```text
query rewrite
+ retrieval
+ rerank
+ context build
+ model queue/prefill/decode
+ citation/policy verification
```

成本拆分：embedding/query、检索服务、rerank、输入 token、输出 token、judge、日志和人工复核。评测时同时比较质量、P95、成本和安全，不用单一“最高分”选方案。

常见消融：

- vector vs BM25 vs hybrid。
- 无 rerank vs cross-encoder/LLM rerank。
- top-k、context token budget 和去重策略。
- 简单拒答规则 vs 证据阈值 + judge。
- 大模型 vs 小模型、长回答 vs 结构化短回答。

每次只改变明确变量，并固定数据集和其余配置。

## 十五、NotifyFlow 发布决策

Incident Knowledge Assistant 的最低产品承诺应是：只回答授权范围；对关键 claim 提供可复核引用；证据不足或冲突时拒答；不把文档指令当系统指令；不自动执行 replay、扩容或权限变更。

发布报告必须同时展示：

- 按 tag/风险分层的检索和生成指标。
- ACL、注入、泄露和拒答失败样本。
- 延迟、token、成本和服务错误。
- 相比 baseline 的 case-level diff。
- 人工复核样本与 judge 分歧。
- 已知限制、未覆盖语言/文档/租户和回滚计划。

## 十六、实验状态

本章所有数据集、模型、Java Runner、红队、回归和在线实验均为 Pending。未有原始结果、人工标注和安全复核前，不得填写 Recall、faithfulness、引用准确率、越权阻断率、P95 或单次成本。

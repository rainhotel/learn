# 第 16 章讲义：把“相似搜索”建设成可靠检索平面

## 学习目标

本章不把向量数据库当成一个黑盒组件。目标是从查询意图、候选召回、权限过滤、融合重排、索引生命周期到 Java 服务治理，建立一条可评测、可解释、可隔离、可回滚的检索链路。

## 一、先定义检索问题，而不是先选数据库

一次检索至少有五种正确性：

| 正确性 | 问题 | 常见失败 |
|---|---|---|
| 语义相关 | 内容是否回答问题 | 同义改写未命中、近义 hard negative 排在前面 |
| 词法相关 | 编号、错误码、类名是否命中 | 向量把 `ERR-1042` 当成普通文本 |
| 权限正确 | 用户是否有权读取 | 先召回后过滤导致候选不足或泄漏 |
| 时间/版本正确 | 是否为当前有效内容 | 旧 Runbook、已撤销公告仍被返回 |
| 结构正确 | chunk 是否保留完整证据 | 标题与正文断开、表格行失去表头 |

“top-5 看起来不错”不是验收。需要问题集、相关文档标注、hard negative、无答案问题、跨租户样本和版本冲突样本。

## 二、检索记录与索引合同

检索系统的最小单位通常是 chunk，而业务真实来源仍是文档及其版本。建议合同至少包含：

```text
chunk_id             全局稳定、不可复用
document_id          业务文档身份
document_version     单调版本或内容版本
tenant_id            强制隔离键
acl_tokens           可检索权限集合或权限引用
title/path/content   展示与检索字段
language/type        路由和 analyzer 选择
valid_from/valid_to  时间有效性
status               ACTIVE/TOMBSTONED/FAILED
embedding_model      模型身份
embedding_version    预处理与模型版本
embedding_dimension  维度
content_hash         幂等和变更检测
source_locator       页码、段落、对象存储位置
created_at/updated_at
```

`embedding_model + embedding_version + dimension + distance + normalization` 是一个不可拆的兼容性合同。不同模型或不同预处理产生的向量不能默认放入同一个可比较空间。迁移时应创建新索引/collection，回填后双读比较，再原子切换别名或路由。

## 三、稠密检索：语义能力与相似度边界

dense retrieval 将查询与 chunk 映射到同一向量空间，适合同义表达、自然语言改写和概念相关性。常用距离：

- cosine similarity：关注方向；如果已做 L2 归一化，常与 inner product 排序等价。
- inner product：受方向和模长共同影响；是否需要归一化取决于模型训练合同。
- L2 distance：欧氏距离；不要把“距离越小”误当成“分数越大”。

必须按 Embedding 模型说明选择度量。应用层不要把不同距离、不同索引或不同模型的原始分数直接加权。

### 3.1 精确搜索与 ANN

精确扫描可以提供小数据集上的近似 ground truth，但计算成本随候选数量和维度增长。ANN 用近似索引换取速度：

- HNSW：查询召回通常较强，内存和构建成本较高；`M`、构建搜索宽度与查询搜索宽度影响体积、构建时间、召回和延迟。
- IVF/IVFFlat：先训练/构建聚类或倒排单元，查询探测部分列表；`lists` 与 `probes` 决定候选范围和成本。
- Flat/exact：适合小规模、强过滤后的候选或作为评测基线。

调参必须与过滤、top-k、并发、硬件和数据分布一起记录。只报告平均延迟或单条“效果不错”没有意义。

## 四、稀疏检索：BM25 仍然不可替代

BM25 基于词项频率、逆文档频率和长度归一化，擅长：

- 错误码、任务 ID、Java 类名、SQL 字段、产品型号。
- 用户明确输入的专有名词。
- 新术语或未被 Embedding 模型很好表示的词。

它也有边界：分词、同义表达、语言 analyzer 和字段权重会直接影响结果。标题、正文、代码、标签可以使用不同字段权重，但权重必须由评测集验证。

在 PostgreSQL 中可用全文检索建立基线；在更复杂的词法场景中可使用 Lucene/OpenSearch/Elasticsearch。不能因为已经部署向量库就删除 sparse 召回。

## 五、混合召回与融合

典型链路：

```text
query
-> identity/tenant/ACL/time filter
-> dense top-N ─┐
                ├-> deduplicate/fusion -> rerank -> top-K context
-> sparse top-N ┘
```

### 5.1 RRF

Reciprocal Rank Fusion 只依赖名次：

```text
RRF(d) = Σ 1 / (k + rank_i(d))
```

优点是无需假设 BM25 分数与向量分数同尺度，对异构检索器更稳健。缺点是忽略分数间距，`k`、各路候选深度和缺失候选仍需调参。

### 5.2 加权分数融合

加权融合可以表达业务偏好，但必须先做可解释的归一化或校准。对每次查询做 min-max 可能被异常值和候选集合改变；固定阈值也会随模型版本漂移。需要在冻结评测集上校准，并监控升级后的分布变化。

### 5.3 Query routing

某些查询可以动态调整召回策略：

- 包含错误码、UUID、类名：提高 sparse 权重。
- 自然语言原因分析：提高 dense 候选深度。
- 明确文档范围：先做 metadata filter，再在范围内检索。

路由规则应是可审计代码或模型分类后的受限配置；不能让生成模型绕过权限过滤。

## 六、过滤不是附加条件，而是正确性边界

过滤字段包括 tenant、ACL、文档状态、版本、有效期、语言、类型和产品线。策略有：

- pre-filter：先限制合法候选，再做相似搜索。权限安全清晰，但高选择性过滤可能让 ANN 需要扩大搜索。
- post-filter：先 ANN，再过滤。实现简单，但可能不足 K 条；若越权候选曾离开受控存储或进入模型，就已经失败。
- iterative filtering：逐步扩大候选直到获得 K 条合法结果或达到预算；必须有最大扫描量、超时和审计。

权限过滤必须在可信检索服务内完成。应用层收到的候选不应包含无权访问的正文。即使最终 prompt 没使用，日志、trace、reranker 请求和缓存也可能泄漏内容。

### 6.1 高选择性过滤的陷阱

全库 ANN 的近邻可能大多属于别的租户。post-filter 后会出现“召回为空”，扩大 ANN 搜索又增加长尾延迟。可选方案包括：

- tenant/业务域分区或 partition key。
- 公共索引与敏感索引分离。
- 过滤字段索引、partial index 或复合查询计划。
- 对小租户在合法候选内精确扫描。

不要为每个极小租户盲目创建独立 collection；集合数量、运维和索引开销也会增长。

## 七、Rerank：在有限候选上做更贵判断

首阶段召回优化“不要漏掉”，reranker 优化“把真正有用的排前面”。常见方案：

- cross-encoder：联合编码 query 与 passage，相关性通常优于独立向量，但每对候选都要计算。
- late interaction：在效果和索引/计算成本间折中。
- LLM rerank：能使用复杂指令，但成本、延迟、稳定性和 prompt injection 风险更高。

级联设计示例：各路召回 50-100 条，融合去重后保留较少候选，再 rerank 选出 context。具体数字必须通过实验决定，本章不预设生产值。

Rerank 需要：批处理、超时、最大候选数、fallback、模型版本、输入截断策略和独立指标。超时时可以退化到融合排序，但响应必须记录 `ranking_mode=FUSION_FALLBACK`，不能伪装成完整链路。

## 八、向量库 schema 与索引工程

### 8.1 pgvector 参考结构

```sql
CREATE TABLE retrieval_chunk (
  chunk_id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  document_id uuid NOT NULL,
  document_version bigint NOT NULL,
  acl_tokens text[] NOT NULL,
  status text NOT NULL,
  valid_from timestamptz,
  valid_to timestamptz,
  content text NOT NULL,
  embedding_model text NOT NULL,
  embedding vector(<DIMENSION>) NOT NULL,
  updated_at timestamptz NOT NULL
);
```

`<DIMENSION>` 必须在选定模型后固定，不能把占位符直接用于迁移。向量索引外，tenant、status、version、时间字段也需要普通索引或分区策略。PostgreSQL Row-Level Security 可以增加数据库侧隔离，但不能替代应用鉴权、连接身份设计和测试。

pgvector 可在事务中同步维护业务 metadata 与向量记录，SQL 能力强，适合复用 PostgreSQL 运维体系。代价是向量 workload 会与 OLTP 争用 CPU、内存、I/O 和连接，需要独立容量边界，必要时使用独立实例。

### 8.2 Milvus 参考结构

Milvus collection schema 应显式定义主键、向量字段、维度、标量 metadata 和索引。tenant/ACL 过滤需要落到服务端表达式或合适的 partition key/collection 策略，不能在 Java 内存中补过滤。

Milvus 适合需要独立扩缩、较大向量数据和专门向量检索运维的场景，但引入 collection、index build/load、segment、compaction、一致性和集群组件等新概念。选择它意味着接受额外部署、监控、备份和升级成本。

## 九、更新、删除与重嵌入

索引是可重建派生数据，文档仓库和 metadata DB 才是真实来源。推荐状态流：

```text
DISCOVERED -> PARSED -> CHUNKED -> EMBEDDED -> INDEXED -> ACTIVE
                                      |             |
                                    FAILED       TOMBSTONED
```

关键规则：

- 使用 `document_id + document_version + chunk_id` 保证幂等。
- 新版本完成全部索引并通过校验后，再切换 `active_version`。
- 删除先写 tombstone/撤销可见性，再异步物理删除；查询必须立即排除 tombstone。
- ACL 撤销属于高优先级更新，不能等待普通批处理窗口。
- 重嵌入写入新模型版本的索引，不原地混合覆盖。
- 通过 reconciliation 比对源文档、任务表和索引计数/哈希，修复漏写。

双写不是天然一致。写一边成功、一边失败时，需要 Outbox、可重放任务、幂等 upsert 和对账，而不是无限重试请求线程。

## 十、召回、延迟与成本的联合预算

检索层至少记录：

- 质量：Recall@k、MRR、nDCG、zero-result rate、权限/版本正确率。
- 延迟：dense、sparse、filter、fusion、rerank 的 P50/P95/P99 和 timeout。
- 容量：向量数、维度、索引体积、构建时间、QPS、并发、连接池/队列。
- 成本：Embedding 生成、存储、索引内存、查询计算、rerank 调用和跨区网络。
- 稳定性：fallback rate、stale index lag、tombstone lag、reconciliation mismatch。

扩大 top-N 通常提高召回机会，也会增加融合、网络传输和 rerank 成本。增加 HNSW 搜索宽度可能提高召回，也会增加 CPU 与延迟。任何“最优参数”都只对指定数据集、过滤分布、并发和硬件成立。

## 十一、Java 集成边界

建议接口把身份、过滤和预算设为必填，而不是可选参数：

```java
public interface RetrievalService {
    RetrievalResult search(RetrievalQuery query, RequestIdentity identity,
                           RetrievalBudget budget, CancellationToken cancellation);
}
```

`RetrievalQuery` 包含 query text、允许的文档域、版本/时间条件；`RequestIdentity` 包含 tenant、subject 和权限；`RetrievalBudget` 包含候选上限、截止时间和 rerank 预算。

工程约束：

- pgvector 可通过 JDBC/类型映射执行参数化 SQL；不拼接 tenant/ACL 表达式。
- Milvus 使用版本锁定的 Java SDK，封装 collection、filter 和一致性配置。
- 设置 pool acquire、connect、request/read 和 overall deadline；overall deadline 向下游传播。
- 只有确定幂等且未超过 deadline 的读请求可有限重试；超时不能无限放大。
- 客户端断开或上游取消时，尽可能取消 dense、sparse 和 rerank 子请求。
- trace 只记录 query fingerprint、候选 ID、分数和耗时；默认不记录敏感原文/向量。
- 缓存 key 必须包含 tenant、权限摘要、query、过滤、模型/索引版本；ACL 变更需失效。

错误语义至少区分：`INVALID_QUERY`、`AUTHZ_DENIED`、`INDEX_NOT_READY`、`DEADLINE_EXCEEDED`、`DEPENDENCY_UNAVAILABLE` 和 `PARTIAL_FALLBACK`。

## 十二、pgvector 与 Milvus 的选型边界

| 维度 | pgvector 更有利 | Milvus 更有利 |
|---|---|---|
| 现有栈 | 团队已有 PostgreSQL、SQL 与备份能力 | 团队已有专门向量平台能力 |
| 一致性 | metadata 与向量需要事务/SQL 联查 | 向量检索可作为独立派生服务 |
| 规模与扩缩 | 中小规模，单实例/读副本容量可证明 | 需要独立水平扩缩和大规模索引治理 |
| 查询 | SQL、关系过滤、RLS、全文检索组合 | 向量检索、标量过滤和 collection 管理为核心 |
| 运维 | 少引入一个系统，但需防止拖累 OLTP | 组件更多，换取专门的向量数据平面 |
| 迁移成本 | 容易先做基线和 MVP | 需要 SDK、数据同步、备份和集群知识 |

决策不能只使用向量条数。还要测：过滤选择性、QPS、维度、top-k、更新率、索引构建窗口、可用性、恢复时间、团队能力和总体成本。

一个务实路径是先用 pgvector 建立正确性基线和真实负载证据；当容量、隔离或扩缩指标证明达到边界，再用同一评测集和接口做 Milvus 对照。若一开始就已有明确平台约束，也可以直接选择 Milvus，但仍要保留精确基线与迁移/恢复方案。

## 十三、NotifyFlow 检索平面

NotifyFlow 的检索对象包括 Runbook、故障复盘、错误码说明和版本公告。示例查询“Java 消费者升级后 Kafka 重复通知增加”同时需要：

- sparse 命中 `Kafka`、类名、错误码和版本号。
- dense 命中“重复消费/幂等失败”的语义近邻。
- tenant、项目、环境、有效版本和 ACL 过滤。
- rerank 优先当前版本、同组件、直接包含修复步骤的证据。
- 最终只返回 evidence ID、引用位置和安全摘要给第 17/18 章链路。

检索服务不能直接执行重放、改消费组、清 DLT 或修改 Kubernetes。它只提供受权限约束的证据。

## 十四、常见反模式

- 只看 cosine 分数，不建立问题级标注集。
- 把所有 metadata 塞进 JSON，却没有可过滤字段和索引。
- ANN 后在 Java 中做 tenant/ACL 过滤。
- 把 BM25 分数与向量相似度直接相加。
- reranker 无超时、无候选上限、无 fallback 标记。
- 原地切换 Embedding 模型，导致新旧向量混在一个空间。
- 删除只删业务表，不撤销索引可见性。
- 缓存不包含 tenant/ACL/index version。
- 用生产文档做演示，却没有脱敏、隔离和审计。
- 用单次本机延迟宣称生产容量。

## 十五、证据等级

本章目前只有资料核验与设计初稿。所有 Recall、nDCG、P99、QPS、索引体积、成本、pgvector/Milvus 对比和 Java 故障恢复结论，都必须等待真实数据集、配置、原始结果和失败样本后才能填写。


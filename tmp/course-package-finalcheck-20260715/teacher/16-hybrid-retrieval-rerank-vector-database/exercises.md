# 第 16 章练习

## A. 概念与计算

1. 给出三个 dense retrieval 容易失败、BM25 更可能成功的 NotifyFlow 查询；再给出三个相反案例。每个案例必须解释失败机制。
2. 一个 Embedding 模型要求归一化向量并使用 inner product。解释为什么查询向量未归一化会破坏分数合同，以及为什么不能把该分数与 BM25 原始分数直接相加。
3. sparse 与 dense 分别返回五个候选。使用 `RRF k=60` 手算融合分数，并说明同一文档只出现在一路时如何处理。然后讨论 `k` 变大或变小的影响。
4. 设计一个包含 direct hit、同义改写、错误码、hard negative、无答案、旧版本和跨租户样本的最小检索评测集。写出 relevance 与权限标注格式。

## B. Schema、过滤与索引

5. 为 Runbook chunk 设计 pgvector 表，包含 tenant、ACL、版本、有效期、状态、Embedding 合同和引用位置。列出向量索引之外至少五个普通索引/约束及其用途。
6. 比较 pre-filter、post-filter 和 iterative filtering。给定“全库一千万 chunk，某租户仅一千 chunk”的场景，分析三种策略的正确性、召回和延迟风险。
7. 分别为 HNSW、IVFFlat 和 exact scan 设计调参实验。列出控制变量、改变变量、指标和停止条件，不填写未经运行的参数结论。
8. 设计 Milvus collection/partition 或 partition key 方案，处理大租户、小租户、公共文档和 user-private 文档。说明为什么“每用户一个 collection”通常不可取。

## C. 融合、重排与成本

9. 比较 RRF 与加权分数融合。说明什么情况下选择 RRF 作为默认基线，什么证据足以支持切换到加权融合。
10. 设计两级检索级联：sparse/dense 候选、融合、rerank 和最终 evidence。给出每级预算字段、超时/取消传播和 fallback 语义；候选数量只能标为待实验默认值。
11. 为 cross-encoder reranker 设计批处理与缓存。缓存 key 必须考虑哪些字段？哪些 ACL/版本变化必须立即使缓存失效？
12. 写出检索单请求成本模型，至少包含 embedding、dense query、sparse query、网络、rerank 和存储摊销。解释为什么只看向量库单次查询价格会低估成本。

## D. 生命周期、Java 与项目

13. 设计文档 v7 更新到 v8 的状态机。要求新版本未完全可用时继续稳定读取 v7；切换后不能混入 v7；失败可重试、可对账、可回滚。
14. 设计“ACL 撤销”和“用户请求删除”两条流程，覆盖数据库、sparse/dense index、缓存、rerank 请求记录、备份和审计。指出哪些步骤必须同步 fail closed。
15. 为 Java `RetrievalService` 定义请求、返回值和错误类型。说明 pool acquire、connect、read/request、overall deadline、取消、有限重试和 trace 如何配合。
16. 完成一份 pgvector 与 Milvus ADR。不能只比较功能列表，必须包含真实负载假设、实验、运维能力、RTO/RPO、成本和重新评估触发器。

## 作业提交物

- `retrieval-contract.md`：数据、Embedding、索引与 API 合同。
- `evaluation-dataset.jsonl`：查询、身份、过滤、相关 chunk 和禁止 chunk。
- `query-plan.md`：sparse/dense/filter/fusion/rerank 级联。
- `tenant-acl-test-matrix.md`：跨租户、权限变更和泄漏面。
- `index-lifecycle.md`：更新、删除、重嵌入、重建和回滚。
- `vector-store-adr.md`：pgvector/Milvus 选型与证据。
- 教师参考证据（提交后解锁）：环境、配置、原始结果和失败样本；未运行时保持 Pending。


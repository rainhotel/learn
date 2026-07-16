# 第 16 章：混合检索、重排与向量数据库工程

## 章节定位

- 类型：Retrieval Engineering + Vector Database + Java Integration + Project + Lab Design + Interview + Teach-back
- 难度：深入
- 建议学习时间：24-32 小时
- 先修章节：第 04 章 MySQL、第 09 章可观测性、第 11 章网络与连接池、第 14 章 LLM/RAG 基础、第 15 章文档处理与 ingestion
- 对应项目：NotifyFlow Knowledge Assistant 的检索平面

## 当前状态

- 阶段：八件套完整内容初稿，实验 Pending
- 编写日期：2026-07-15
- 已完成：混合召回、过滤、重排、向量库 schema/index、租户 ACL、索引生命周期、Java 集成与选型讲义设计
- 未完成：评测语料落盘、pgvector/Milvus 运行、Java 客户端压测、权限攻击用例与原始结果

本章不能标记为 `Lab Verified`、`Release Candidate` 或 `Released`。

## 与相邻章节的边界

- 第 14 章解释 Embedding、ANN、RAG 和 Agent 的基础概念；本章不重复 Transformer、KV cache 或通用 Agent Runtime。
- 第 15 章负责解析、去重、切分、metadata 生成与 ingestion；本章从“可检索 chunk 已产生”开始。
- 本章负责候选召回、过滤、融合、重排、索引与查询服务。
- 第 17 章负责端到端 RAG 评测、引用、拒答与安全；本章只定义检索层指标和证据接口。

## 核心问题

1. 为什么语义向量不能替代 BM25，BM25 也不能覆盖语义改写？
2. dense、sparse、filter 和 rerank 如何组成可解释的级联链路？
3. weighted fusion 与 Reciprocal Rank Fusion（RRF）分别在什么条件下可靠？
4. 向量距离、归一化、Embedding 模型和维度为什么必须成为索引合同？
5. 高选择性 metadata filter 为什么可能破坏 ANN 召回与延迟？
6. 如何设计 tenant、ACL、版本、有效期和删除状态，保证越权 chunk 永不进入模型上下文？
7. HNSW、IVF/IVFFlat 和精确扫描怎样在召回、延迟、内存与构建成本间取舍？
8. 文档更新、删除、重嵌入和索引迁移怎样避免新旧版本混用？
9. Java 服务如何处理连接池、超时、取消、有限重试、trace 和降级？
10. 什么时候 pgvector 足够，什么时候才值得引入 Milvus？

## 章节产物

- 一份检索需求与失败分类表。
- 一份 chunk/embedding/index schema 与兼容性合同。
- 一份 dense + sparse + filter + rerank 查询计划。
- 一份多租户 ACL 与越权测试矩阵。
- 一份更新、删除、重建、回滚和双读迁移方案。
- 一份 pgvector 与 Milvus 的可辩护 ADR。
- 一份 Java `RetrievalService` 接口、错误语义和可观测性设计。
- 一套可复现实验计划和原始证据目录。

## 建议学习顺序

1. 先建立精确检索基线和带标注的小型评测集。
2. 再分别实现 sparse 与 dense 召回，确认各自失败类型。
3. 增加 metadata filter，先证明权限正确，再优化速度。
4. 比较 RRF、归一化加权融合和 query routing。
5. 加入 reranker，测量增益、长尾延迟和成本。
6. 最后做索引调参、多租户隔离、更新删除和 Java 故障实验。

## 退出标准

- 能用失败样本解释 lexical、dense、filter、fusion 和 rerank 各自解决什么问题。
- 能说明 cosine、inner product、L2 与向量归一化的关系，避免混用分数。
- 能为 pgvector 或 Milvus 设计包含租户、ACL、版本和模型身份的 schema。
- 能设计以“权限正确性优先”为前提的 pre-filter、post-filter 或 iterative filtering。
- 能从 `Recall@k / nDCG / MRR`、P50/P95/P99、成本和索引体积联合评价方案。
- 能证明删除、过期、重嵌入和模型迁移不会把旧 chunk 返回给用户。
- 能用 Java 实现可取消、可超时、可追踪、有限重试的检索接口设计。
- 能给出 pgvector/Milvus 选型证据，而不是按“数据量大”一句话拍板。
- 能诚实区分设计完成、静态检查通过和真实运行验证。

## 发布前缺口

- 固定数据集、Embedding 模型、维度、距离函数和版本。
- 运行 exact、BM25、dense、hybrid、rerank 的对照实验。
- 运行不同过滤选择性、top-k 和索引参数的召回/延迟实验。
- 运行跨租户、ACL 变更、过期、删除和重建的安全回归。
- 运行 Java 客户端超时、连接池耗尽、取消和降级测试。
- 形成 pgvector/Milvus 原始结果、成本模型与最终 ADR。


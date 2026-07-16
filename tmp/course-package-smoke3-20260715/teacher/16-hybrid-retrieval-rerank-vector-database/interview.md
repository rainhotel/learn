# 第 16 章面试追问

## 为什么 RAG 不能只做向量检索？

向量适合语义改写，但错误码、类名、版本号和专有名词常由词法检索更稳定地命中。生产系统通常用 sparse + dense 互补，再融合和重排，并用同一评测集证明收益。

## cosine、inner product 和 L2 怎么选？

遵循 Embedding 模型训练与发布合同。归一化向量下 cosine 与 inner product 常有相同排序；未归一化时 inner product 还受模长影响。L2 是距离，方向与阈值语义不同。模型、归一化和距离必须一起版本化。

## 为什么不能把 BM25 分数和向量相似度直接相加？

两者尺度和分布不同，并且会随查询、语料和模型版本变化。直接相加可能让某一路仅因数值范围主导。可先用 RRF，或在冻结评测集上做校准后的加权融合。

## RRF 的优缺点是什么？

它只使用排名，对异构分数稳健，易作为基线；但忽略分数间距，参数、候选深度和各路质量仍影响结果，也不能替代权限过滤和 rerank。

## pre-filter 与 post-filter 怎么选？

权限和租户应在可信检索服务中 pre-filter 或使用能保证服务端过滤的策略。post-filter 可能导致 top-k 不足，而且越权正文可能已经进入应用、日志、缓存或 reranker。高选择性场景要结合分区、过滤索引、iterative search 或合法候选内 exact scan。

## HNSW 的调参关注什么？

关注构建参数、查询搜索宽度、Recall@k、P95/P99、内存、索引体积和构建时间，并在真实过滤分布与并发下测。参数名称和含义以具体数据库版本为准，不能把别的系统结果直接套用。

## IVFFlat 与 HNSW 的核心差异？

IVFFlat 通过聚类/倒排列表减少搜索范围，lists/probes 影响精度和成本，通常需要合适的数据与构建过程。HNSW 使用多层近邻图，通常有较高内存和构建开销。最终选择依赖数据、更新、过滤和运维实测。

## Rerank 为什么不直接对全库执行？

cross-encoder 或 LLM rerank 每个 query-document 对都要更贵计算。它适合在高召回候选上做精排，需要候选上限、batch、超时、fallback 和模型版本治理。

## 如何评测检索？

用人工标注问题集测 Recall@k、MRR、nDCG，同时测权限/版本正确率、zero-result、P50/P95/P99、成本和失败样本。评测集包含 hard negative、无答案、过期和跨租户，不用生成答案分数替代检索指标。

## 多租户向量库如何防止越权？

tenant 和 ACL 是服务端强制条件；身份映射由可信服务产生；返回、日志、缓存、reranker payload 全链路验证隔离。可结合数据库 RLS、partition key 或物理隔离，但都需要攻击测试，不能只靠 Java 事后过滤。

## 文档删除为什么复杂？

内容可能存在源库、词法索引、向量索引、缓存、rerank 日志、备份和离线数据。应先 tombstone 使查询立即不可见，再异步物理回收，并用对账验证收敛；备份按明确保留与删除策略处理。

## 更换 Embedding 模型怎么做？

新模型使用独立索引/collection version，回填后 shadow query，对同一评测集比较质量、延迟和成本。门禁通过后切换路由，保留旧版回滚。不能在同一向量空间中原地混合新旧模型。

## Java 检索服务如何做超时？

定义 overall deadline，再给 pool acquire、连接、sparse、dense、rerank 分配剩余预算。并行分支共享取消信号，只有幂等读且剩余时间足够时有限重试；fallback 必须在结果与指标中显式标记。

## pgvector 与 Milvus 怎么选？

pgvector 适合复用 PostgreSQL、需要 SQL/事务/关系过滤且规模边界可证明的场景；Milvus 适合独立向量数据平面和水平扩缩，但引入更多部署与运维概念。要用向量数、过滤分布、更新率、QPS、重建/恢复、团队能力和总体成本做 ADR。

## 为什么不建议每个用户一个 collection？

collection、索引、加载、监控、备份和升级都是运维对象。用户数量大时对象数会失控。应按隔离需求把大租户物理分离、小租户共享并使用强制 tenant/ACL 过滤。

## 如果 reranker 挂了怎么办？

在预先验证的质量门禁内回退到融合顺序，响应记录 `FUSION_FALLBACK`，并监控 fallback rate。不能无限重试，也不能把降级结果当成完整 rerank 结果。

## 如何证明检索系统“可恢复”？

以源文档和 metadata DB 为真实来源，保留可重放 ingestion、幂等 upsert、索引版本、备份/重建流程和 reconciliation。通过故障演练证明漏写可发现、重建可校验、路由可回滚，而不是只说索引可以重新生成。

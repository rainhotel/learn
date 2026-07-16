# 第 16 章参考答案与评分

## 1. Dense 与 BM25 的互补案例

BM25 更有利：`ERR-1042`、`KafkaConsumerConfig.java`、完整 task UUID。它们依赖精确 token，语义向量可能把编号弱化。dense 更有利：“消息已经处理但用户收到两次”“升级以后偶发重复发送”“如何避免重试造成副作用”，它们与文档中的“幂等、重复消费、重试放大”可能没有相同词面。

合格答案必须说明 analyzer、Embedding 训练分布、OOV/新术语、同义改写或 hard negative，而不是只说“BM25 精确、向量语义”。

## 2. 归一化与分数合同

若模型训练/检索合同假设单位向量，则 inner product 等价于 cosine 排序。查询未归一化会让模长改变分数尺度，阈值、校准和跨请求比较失效。BM25 分数来自词频、文档频率和长度归一化，数值分布与向量分数无共同尺度；直接相加会让某一路因数值范围而支配结果。应优先使用 RRF，或在冻结评测集上做可靠校准。

## 3. RRF 手算

每个检索器中 rank 为 `r` 的文档贡献 `1/(60+r)`；同一文档在两路出现则贡献求和，只在一路出现则只有一路贡献，不补虚拟分数。提交物应列出逐项分数和最终排序。

`k` 较小时更强调头部名次差异；`k` 较大时各名次贡献更接近，多个检索器共同出现的优势更明显。实际值要由评测集验证。

## 4. 最小评测集

参考字段：

```json
{
  "queryId": "q-001",
  "query": "ERR-1042 在 v3 如何处理",
  "identity": {"tenantId": "t1", "aclTokens": ["team:backend"]},
  "asOf": "2026-07-15T00:00:00Z",
  "relevant": [{"chunkId": "c8", "grade": 3, "reason": "当前版本直接答案"}],
  "acceptable": ["c9"],
  "forbidden": [{"chunkId": "c-old", "reason": "过期"}, {"chunkId": "c-t2", "reason": "跨租户"}],
  "answerable": true,
  "tags": ["error-code", "version"]
}
```

至少覆盖题目列出的七类，并由人工复核相关性等级与 forbidden 集合。

## 5. pgvector schema

核心字段可参考讲义。普通索引/约束至少包括：tenant + status、document + version、有效期、content hash 唯一性、ACL 可过滤结构、updated_at 对账游标、Embedding 模型/版本约束。还应约束 dimension、status 枚举、版本不可为负，并确保 chunk ID 不复用。

向量索引不能代替 metadata 索引；否则过滤可能导致全表代价或 ANN 候选不足。

## 6. 三种过滤

- pre-filter：先把候选限制为一千条，再精确或 ANN 搜索，权限边界最清晰；要确认数据库能有效利用过滤索引。
- post-filter：全库 ANN top-N 很可能全是其他租户，过滤后不足 K；扩大 N 增加延迟，并可能让越权正文流入应用、日志或 reranker，因此不应作为权限方案。
- iterative：服务端逐步扩大合法候选搜索，可平衡不足 K 与预算，但必须有最大扫描、deadline 和 fail closed。

该极端选择性场景还应比较按 tenant 分区、小租户 exact scan、公共/私有索引分离。

## 7. 索引实验

固定数据集、Embedding、维度、过滤分布、并发、硬件和查询集。HNSW 改变构建/查询搜索宽度和图连接参数；IVFFlat 改变列表数、probes 和训练/构建过程；exact scan 作为 ground truth。测 Recall@k、MRR/nDCG、P50/P95/P99、CPU、内存、索引体积、构建时间和过滤正确率。

停止条件应由 SLO、资源上限或收益递减定义，不能先写“某参数最佳”。

## 8. Milvus 多租户方案

大租户可依据隔离与容量使用独立 collection/database；大量小租户更适合共享 collection，并把 tenant 作为强制标量过滤或合适的 partition key；公共文档与敏感私有文档可分离。user-private 仍需 tenant + subject/ACL 过滤。

每用户一个 collection 会导致 metadata、index/load、监控、备份和生命周期对象数量膨胀，不能只看逻辑隔离直觉。

## 9. RRF 与加权融合

RRF 适合作为异构分数、校准数据不足时的稳健基线。切换加权融合需要：冻结评测集、明确归一化/校准、离线质量提升、在线或 shadow 稳定性、模型升级后的分布监控、失败样本和回滚方案。单个查询看起来排序更好不构成证据。

## 10. 两级级联

请求先分配 overall deadline，再为 sparse、dense 和 rerank 留子预算。两路召回必须带同一 tenant/ACL/version filter；融合去重后，在剩余时间内 rerank。取消信号向所有并行子请求传播。

fallback 示例：dense 超时时只在 sparse 达到既定质量门禁时返回 `SPARSE_FALLBACK`；rerank 超时返回 `FUSION_FALLBACK`。候选数和毫秒数应作为配置与实验变量，而非本章固定结论。

## 11. Rerank 缓存

key 至少包含 tenant、权限摘要、query fingerprint、候选 chunk/version、reranker 模型/版本、截断策略和排序配置。文档版本切换、ACL 撤销、删除/tombstone、模型升级和安全策略变化必须失效。不要缓存无权限正文，也不要跨身份共享 user-private 结果。

批处理要有最大 batch、最大等待、deadline 检查和取消；批量效率不能让短请求被长请求拖住。

## 12. 成本模型

```text
cost/request = query_embedding
             + dense_compute_and_storage
             + sparse_compute_and_storage
             + network
             + rerank(candidate_count, tokens)
             + index_build_amortization
             + operations_and_recovery
```

还应乘以缓存命中、重试、fallback、写入更新率和流量分布。只看向量查询价格遗漏了 embedding、词法引擎、reranker、跨区流量、索引重建和运维。

## 13. v7 到 v8 状态机

v8 经过 `PARSED -> CHUNKED -> EMBEDDED -> INDEXED -> VERIFIED`，在 `VERIFIED` 前读取路由保持 v7。校验包括 chunk 计数/哈希、权限字段、抽样查询和索引可用性。随后原子切换 active version；查询同时要求 version=active，避免混入 v7。失败任务幂等重试并进入对账；切换后若质量回退则路由回滚 v7，v8 保留供诊断。

## 14. ACL 撤销与删除

ACL 撤销必须先更新权威权限状态并让查询 fail closed，同步清理/禁用缓存可见性，再异步刷新各索引。删除同样先 tombstone，查询立即排除，再物理删除 sparse/dense、对象存储派生物和缓存。

rerank 请求记录与审计应最小化敏感正文并遵守保留策略；备份删除按法规/产品策略执行，不应虚构“立即从所有备份擦除”。对账证明各派生系统最终收敛。

## 15. Java RetrievalService

请求包含 query、identity、可信 scope、asOf 和 budget；返回 evidence、每阶段模式/耗时、index version 与是否 fallback。错误至少区分参数错误、拒绝授权、索引未就绪、deadline、依赖故障和部分降级。

pool acquire、connect、request/read 都必须小于 overall deadline；每阶段使用剩余预算。只有幂等读且剩余时间足够时有限重试。上游取消应取消并行检索与 rerank。trace 使用 ID、rank、配置版本和耗时，不默认记录正文/向量。

## 16. ADR

合格 ADR 包含现状、候选、真实负载假设、实验矩阵、质量/性能/成本、故障恢复、团队运维、迁移与回滚。结论应附重新评估触发器，例如容量、P99、重建窗口或资源争用超过已验证边界。没有运行证据时状态必须是 `Proposed`。

## 评分标准

| 维度 | 分值 |
|---|---:|
| 检索原理与失败分析 | 15 |
| Schema、Embedding 与索引合同 | 15 |
| 混合召回、融合与 rerank | 20 |
| tenant/ACL/版本正确性 | 20 |
| 更新、删除、迁移与对账 | 10 |
| Java 超时、取消与可观测性 | 10 |
| pgvector/Milvus ADR | 5 |
| 证据诚实与可复现性 | 5 |

出现任一情况，项目不得通过发布门槛：跨租户内容进入应用或 reranker、删除后仍可检索、Embedding 模型混用、无评测集却宣称质量、无原始结果却宣称容量。


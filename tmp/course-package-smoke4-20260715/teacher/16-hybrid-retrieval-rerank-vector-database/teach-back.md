# 第 16 章 Teach-back

## 5 分钟：为什么需要混合检索

只使用一张图讲清：

```text
query + identity
-> sparse / dense（同一强制 filter）
-> RRF 去重融合
-> rerank
-> evidence
```

必须各举一个错误码查询和一个同义改写查询，说明 BM25 与 dense 的互补；解释为什么原始分数不能直接相加。

## 15 分钟：一次安全查询计划

从 NotifyFlow 问题“升级后重复通知增加”开始，依次讲：

1. tenant、ACL、版本和有效期如何成为强制 filter。
2. sparse/dense 如何并行召回，overall deadline 如何向下传播。
3. RRF 如何融合，reranker 为什么只处理有限候选。
4. reranker 超时如何显式退化为 fusion fallback。
5. evidence 为什么只返回当前有效、可引用的 chunk。
6. 哪些字段可进入 trace，哪些敏感正文不能记录。

## 45 分钟：检索平台设计答辩

答辩结构：

1. 需求与失败分类：语义、词法、权限、版本、结构。
2. 数据合同：chunk、Embedding、distance、metadata 和 index version。
3. 召回计划：exact 基线、BM25、dense、filter、fusion、rerank。
4. 多租户：大/小租户、公共/私有文档、RLS/partition 的边界。
5. 生命周期：更新、ACL 撤销、删除、重嵌入、重建与对账。
6. Java 集成：接口、连接池、deadline、取消、重试、fallback 和 trace。
7. 指标：Recall@k、nDCG、权限正确率、P99、成本与索引滞后。
8. ADR：为什么当前选 pgvector 或 Milvus，什么证据会触发改选。
9. 诚实边界：哪些是资料结论、哪些是设计、哪些仍然 Pending。

## 追问卡

- ANN 搜索后再做 tenant 过滤有什么问题？
- 为什么 HNSW 参数不能从博客直接复制？
- ACL 撤销后缓存和 reranker 记录怎么处理？
- 为什么新旧 Embedding 不能放在同一空间比较？
- pgvector 拖慢 OLTP 时你有哪些隔离与迁移路径？
- Milvus collection 越多越隔离，为什么仍可能是坏设计？
- rerank 提升 nDCG 却让 P99 超标，你怎么决策？
- 删除事件一边成功一边失败，如何恢复？

## 演示证据

正式教学演示必须展示：

- 冻结评测集和标注说明。
- exact/sparse/dense/hybrid/rerank 原始结果。
- 跨租户、旧版本、tombstone 的负向断言。
- 参数、硬件、数据量、过滤分布和并发。
- Java 超时/取消/fallback 的日志或 trace。
- pgvector/Milvus ADR 和未选择方案的代价。

当前上述运行证据均为 Pending，不能用设计文档代替演示结果。

## 评分 Rubric

| 等级 | 表现 |
|---|---|
| 不通过 | 把向量库当黑盒；忽略 ACL/删除；声称未测性能 |
| 基础 | 能解释 sparse/dense，但缺少生命周期或 Java 故障语义 |
| 合格 | 能完整解释查询计划、隔离、评测、更新删除和选型 |
| 优秀 | 能用失败样本和原始证据辩护参数，清楚说明回滚与限制 |


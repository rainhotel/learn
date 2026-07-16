# NotifyFlow Retrieval Plane 项目应用

## 1. 项目目标

为 NotifyFlow Knowledge Assistant 提供一个独立检索平面：在租户、ACL、版本和时间约束内，对 Runbook、故障复盘、错误码说明与发布公告执行 sparse + dense 召回、融合和 rerank，并返回可引用 evidence。检索平面不负责生成最终答案，也不执行任何恢复动作。

## 2. 非目标

- 不直接读取或修改通知任务业务状态。
- 不让模型生成 SQL/filter 后原样执行。
- 不把跨租户文档、Secret、完整用户 payload 写入索引。
- 不以“向量数据库已返回结果”替代权限、版本和引用校验。
- 不在本章宣称未经运行的召回率、QPS 或成本。

## 3. 系统边界

```text
Document Source / Metadata DB
        |
        v
Ingestion Outbox -> Embed Worker -> Sparse/Dense Index -> Reconciliation
                                             ^
                                             |
API -> Identity/AuthZ -> Query Planner -> Retrieval Fan-out
                                      -> Fusion -> Rerank -> Evidence API
```

查询链路与 ingestion 链路独立限流。源文档和 metadata DB 是真实来源，向量/词法索引是可重建派生数据。

## 4. Evidence schema

```json
{
  "chunkId": "uuid",
  "documentId": "uuid",
  "documentVersion": 12,
  "tenantId": "uuid",
  "aclTokens": ["team:backend", "role:oncall"],
  "title": "Kafka 重复通知排查",
  "content": "...",
  "sourceLocator": {"section": "3.2", "paragraph": 4},
  "validFrom": "instant",
  "validTo": null,
  "embeddingModel": "model-id",
  "embeddingVersion": "pipeline-v3",
  "contentHash": "sha256",
  "status": "ACTIVE"
}
```

`content` 只在检索服务确认权限后返回。日志和 trace 默认记录 `chunkId/documentId/version`，不记录正文。

## 5. 查询合同

```json
{
  "query": "升级后 Kafka 重复通知为什么增加",
  "scope": {"product": "notifyflow", "environment": "prod"},
  "identity": {"tenantId": "t-1", "subjectId": "u-7"},
  "asOf": "instant",
  "budget": {
    "deadlineMs": 900,
    "denseCandidates": 60,
    "sparseCandidates": 60,
    "rerankCandidates": 24,
    "resultLimit": 8
  }
}
```

数字只是接口示例，不是生产推荐值。真实默认值必须由本章实验和 SLO 决定。

## 6. Java 服务设计

```text
RetrievalController
  -> IdentityResolver
  -> QueryPolicyValidator
  -> QueryPlanner
      -> SparseRetriever
      -> DenseRetriever
  -> FusionService
  -> RerankGateway
  -> EvidenceSanitizer
  -> RetrievalAuditService
```

关键对象：

- `RetrievalRequest`：不可缺省 tenant、subject、deadline 和 result limit。
- `RetrievalFilter`：由可信代码根据 scope 与身份生成，不接收任意用户表达式。
- `Candidate`：记录 source、raw rank、raw score、fusion score 和 index version。
- `Evidence`：记录引用位置、文档版本、权限判定与内容摘要。
- `RetrievalDiagnostics`：记录每阶段耗时、候选数、fallback 和截断原因。

## 7. 查询计划

1. 校验身份、租户、允许文档域、时间和最大预算。
2. 生成服务端 tenant/ACL/status/version filter。
3. 并行执行 sparse 与 dense；共享 overall deadline，可取消。
4. 对 `chunkId + documentVersion` 去重，默认使用 RRF 融合。
5. 对候选执行确定性权限复核，但复核不是事后过滤的替代品。
6. 在剩余预算内调用 reranker；超时则返回显式 fusion fallback。
7. 再次校验 ACTIVE、版本和有效期，生成 evidence。
8. 写入不含敏感原文的指标、trace 与审计。

## 8. ACL 与多租户设计

- tenant 是每次查询的强制条件，不能由客户端省略或覆盖。
- ACL token 由身份服务映射；用户不能直接提交任意角色名。
- public、team、user-private 文档采用显式可见域，不用空 ACL 表示“默认可见”。
- ACL 撤销发布高优先级 index update，并清理包含权限摘要的缓存。
- 对同一 query 构造 tenant A/B 镜像样本，断言候选正文、ID、日志和 reranker payload 都不泄漏。
- 若使用 PostgreSQL RLS，连接用户和 session context 必须纳入连接池测试，防止身份残留。

## 9. 更新、删除与模型迁移

### 文档更新

1. 新版本写入 metadata DB 和 Outbox。
2. 生成稳定 chunk ID 或明确的新旧映射。
3. 完成 sparse/dense 索引和计数/哈希校验。
4. 原子切换 active version。
5. 旧版本标为 TOMBSTONED，再异步物理回收。

### ACL 撤销与删除

先撤销查询可见性，再删除物理数据。任何 cache、rerank 缓存和离线导出都必须按相同事件清理。对账任务验证源状态与索引可见性一致。

### Embedding 迁移

新模型写入独立 index/collection version。离线回填后做 shadow query，对同一评测集比较质量、延迟和成本；通过门禁后切换读路由，可快速回滚到旧版本。

## 10. pgvector 与 Milvus ADR 模板

ADR 必须填写：

- 向量数、增长率、维度、更新率、过滤分布和并发。
- exact 基线、目标 Recall@k、P95/P99 与可用性。
- 索引构建/恢复时间、备份与 RTO/RPO。
- Java SDK/JDBC、连接池、部署和监控成熟度。
- 对 OLTP 的资源隔离影响。
- 三年存储、计算、运维和迁移成本。
- 触发重新评估的阈值，而不是永久结论。

初始假设可以选择 pgvector 作为正确性基线，但在真实实验完成前，ADR 状态只能是 `Proposed`。

## 11. 故障与降级

| 故障 | 行为 | 不允许的行为 |
|---|---|---|
| dense 超时 | 在质量门禁允许时返回 sparse fallback，并标记模式 | 无限重试拖垮 deadline |
| sparse 超时 | 返回 dense fallback 或明确失败 | 隐藏组件故障 |
| rerank 超时 | 使用融合顺序，记录 fallback | 返回未标记的“完整结果” |
| index 未加载 | 返回 `INDEX_NOT_READY` 或切换已验证旧版 | 查询半建索引 |
| ACL 服务不可用 | fail closed | 把空 ACL 当作公开 |
| 新索引质量回退 | 路由回滚旧版 | 原地覆盖无法回滚 |
| 删除积压 | 查询强制排除 tombstone并告警 | 等待物理删除才隐藏 |

## 12. 验收场景

1. 同义问题由 dense 命中，错误码由 sparse 命中，hybrid 排名优于单路基线。
2. hard negative 在 rerank 后下降，但 rerank 超时仍可显式降级。
3. tenant A 无法在返回、日志、trace、cache 或 reranker 请求中看到 tenant B 内容。
4. 旧版本、过期文档和 tombstone 在任何索引模式下都不可见。
5. Embedding v1/v2 可 shadow 对照并独立回滚。
6. Java 请求取消后不继续无界占用连接和 rerank 预算。
7. reconciliation 能发现漏索引、重复 chunk、错误版本和删除积压。

以上均为待执行验收项，当前状态全部为 Pending。

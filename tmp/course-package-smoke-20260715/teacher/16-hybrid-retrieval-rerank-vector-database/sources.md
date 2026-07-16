# 第 16 章资料与验证状态

## 1. 信息检索与融合

1. Robertson, Zaragoza, *The Probabilistic Relevance Framework: BM25 and Beyond*：<https://www.nowpublishers.com/article/Details/INR-019>
2. Cormack, Clarke, Buettcher, *Reciprocal Rank Fusion Outperforms Condorcet and Individual Rank Learning Methods*：<https://dl.acm.org/doi/10.1145/1571941.1572114>
3. Manning, Raghavan, Schütze, *Introduction to Information Retrieval*：<https://nlp.stanford.edu/IR-book/>
4. Nogueira, Cho, *Passage Re-ranking with BERT*：<https://arxiv.org/abs/1901.04085>
5. Sentence Transformers CrossEncoder 文档：<https://www.sbert.net/docs/cross_encoder/usage/usage.html>

用途：解释 BM25、排名融合、MRR/nDCG 与 cross-encoder rerank。论文结果不能直接当作 NotifyFlow 的效果结论。

## 2. Dense retrieval 与 ANN

1. Karpukhin et al., *Dense Passage Retrieval for Open-Domain Question Answering*：<https://arxiv.org/abs/2004.04906>
2. Malkov, Yashunin, *Efficient and Robust Approximate Nearest Neighbor Search Using HNSW*：<https://arxiv.org/abs/1603.09320>
3. Johnson, Douze, Jégou, *Billion-scale Similarity Search with GPUs*：<https://arxiv.org/abs/1702.08734>

用途：解释 dense retrieval、HNSW 和 ANN 的基本机制。索引参数必须以具体数据库版本文档与本章实验为准。

## 3. PostgreSQL 与 pgvector

1. pgvector 官方仓库与索引说明：<https://github.com/pgvector/pgvector>
2. pgvector-java 官方仓库：<https://github.com/pgvector/pgvector-java>
3. PostgreSQL Full Text Search：<https://www.postgresql.org/docs/current/textsearch.html>
4. PostgreSQL Row Security Policies：<https://www.postgresql.org/docs/current/ddl-rowsecurity.html>
5. PostgreSQL Partial Indexes：<https://www.postgresql.org/docs/current/indexes-partial.html>
6. PostgreSQL JDBC 文档：<https://jdbc.postgresql.org/documentation/>

用途：核对 vector 类型、距离操作、HNSW/IVFFlat、JDBC、全文检索、普通索引与 RLS。实际 DDL、扩展版本、执行计划和过滤行为尚未在本章运行。

## 4. Milvus

1. Milvus 官方文档：<https://milvus.io/docs>
2. Milvus Schema：<https://milvus.io/docs/schema.md>
3. Milvus Vector Index：<https://milvus.io/docs/index-vector-fields.md>
4. Milvus Filtered Search：<https://milvus.io/docs/filtered-search.md>
5. Milvus Multi-tenancy：<https://milvus.io/docs/multi_tenancy.md>
6. Milvus Partition Key：<https://milvus.io/docs/use-partition-key.md>
7. Milvus Java SDK API Reference：<https://milvus.io/api-reference/java>

用途：核对 collection schema、标量过滤、索引、多租户策略和 Java 客户端。Milvus 版本、部署拓扑、SDK 调用与性能尚未在本章运行。

## 5. 多租户与数据生命周期

1. OWASP Authorization Cheat Sheet：<https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html>
2. NIST SP 800-53 Rev. 5, Access Control：<https://csrc.nist.gov/pubs/sp/800/53/r5/upd1/final>

用途：支持 deny by default、least privilege、服务端鉴权与审计原则。具体合规和删除期限必须由项目法律/安全要求确定，本章不自行假设。

## 6. Java 集成资料

1. Java 21 `HttpClient` API：<https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html>
2. Java 21 `CompletableFuture` API：<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html>
3. OpenTelemetry Java 文档：<https://opentelemetry.io/docs/languages/java/>

用途：设计并行请求、取消、超时与 trace。具体 SDK、框架、线程模型和取消传播仍需 Java 实验验证。

## 来源使用规则

- 优先以锁定版本的官方数据库文档和 API 为实现依据。
- 论文用于解释机制，不把论文数据、硬件或参数直接复制到项目。
- 博客只能帮助发现问题，不能单独证明安全、性能或选型结论。
- 所有性能结论必须附数据集、模型、维度、过滤分布、参数、硬件、并发和原始结果。
- 所有权限结论必须附跨租户与 ACL 变更的负向测试。
- 产品版本升级时重新核对链接、参数名、默认值和兼容性。

## 当前验证状态

| 项目 | 状态 | 当前证据 |
|---|---|---|
| BM25/dense/RRF/rerank 原理 | 资料核验 + 讲义初稿 | 论文与官方资料 |
| pgvector schema/index/filter | 设计初稿 | 官方文档；无本章执行计划与运行结果 |
| Milvus schema/index/filter | 设计初稿 | 官方文档；无本章集群与运行结果 |
| 多租户 ACL | 设计初稿 | 安全原则；无攻击测试输出 |
| 更新/删除/模型迁移 | 设计初稿 | 状态机与验收项；无故障演练 |
| Java 客户端 | 接口设计初稿 | JDK/JDBC/SDK 资料；无编译与运行证据 |
| pgvector/Milvus ADR | Proposed | 缺少同数据集对照与成本数据 |

本章实验全部 Pending，不能标记为 `Lab Verified`、`Release Candidate` 或 `Released`。


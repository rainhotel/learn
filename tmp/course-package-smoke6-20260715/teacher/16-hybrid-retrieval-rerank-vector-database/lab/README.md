# 第 16 章实验：混合检索、重排与向量库

## 当前状态

- 总状态：Pending
- 已完成：实验问题、变量、断言、指标、证据目录和发布门禁设计
- 未完成：评测数据落盘、服务启动、索引构建、Java 编译运行、压测和原始结果
- Verified 实验数：0

下列所有实验均未真实运行。文档中的候选数量、超时和参数只能作为待验证变量，不能当成推荐值或性能结论。

## 1. 固定环境合同

每次运行前记录：

```text
OS / CPU / memory / disk
JDK / Maven / Java client version
PostgreSQL / pgvector version
Milvus / Java SDK / deployment topology
Embedding model / revision / dimension
distance / normalization
dataset hash / query-set hash
chunk count / tenant distribution / update rate
index type / complete parameters
concurrency / warmup / duration
```

环境任一关键项改变，应创建新的 run，不覆盖旧结果。

## 2. 数据集合同

`queries.jsonl` 每条包含：

- `queryId`、query、language、intent tags。
- tenant、subject、ACL token、as-of time 和允许 scope。
- 分级相关 chunk、acceptable chunk、forbidden chunk。
- direct、paraphrase、error-code、hard-negative、no-answer、stale-version、cross-tenant 等标签。

`chunks.jsonl` 每条包含：

- chunk/document/version/tenant/ACL/status/effective time。
- title/content/source locator/content hash。
- Embedding 模型与 pipeline version。

数据必须是脱敏合成数据或获授权的隔离测试数据。严禁把真实 Secret、完整用户 payload 或跨租户生产正文加入实验。

## 3. 实验矩阵

### L16-01 Exact ground truth

- 状态：Pending
- 目标：使用合法候选内 exact vector scan 建立 ANN 对照。
- 变量：query、filter、distance、top-k。
- 断言：forbidden chunk 永不出现；ground truth 可重复生成。
- 证据：SQL/请求、结果 ID、距离、耗时、数据集 hash。

### L16-02 Sparse baseline

- 状态：Pending
- 目标：测试 BM25/全文检索对错误码、类名、版本号和普通语言的表现。
- 变量：analyzer、字段权重、query 类型。
- 指标：Recall@k、MRR、nDCG、zero-result、P50/P95/P99。
- 断言：tenant/ACL/version filter 与 dense 路径一致。

### L16-03 Dense ANN

- 状态：Pending
- 目标：比较 exact、HNSW 与 IVFFlat/可用 ANN 索引。
- 变量：索引参数、搜索参数、top-k、并发、过滤选择性。
- 指标：Recall@k 相对 exact、延迟、CPU、内存、体积、构建时间。
- 断言：记录完整参数；不把单次平均延迟当容量结论。

### L16-04 Hybrid fusion

- 状态：Pending
- 目标：比较 sparse、dense、RRF 和校准后的 weighted fusion。
- 变量：各路候选深度、RRF k、融合权重/校准方法。
- 指标：Recall@k、MRR、nDCG、按 query tag 的失败分布。
- 断言：不直接相加未校准原始分数；保留逐路 rank/score。

### L16-05 Filter selectivity

- 状态：Pending
- 目标：比较 pre-filter、服务端 iterative filter 和 post-filter 风险。
- 变量：tenant 占比、ACL 命中率、时间/version 选择性、result k。
- 指标：合法 Recall@k、候选不足率、扫描量、P99。
- 阻断断言：任何越权 ID、正文、日志、cache 或 reranker payload 泄漏即失败。

### L16-06 Rerank cascade

- 状态：Pending
- 目标：测量 reranker 相对 fusion 的增益与代价。
- 变量：候选数、batch、截断、模型版本、deadline。
- 指标：MRR/nDCG 增益、P95/P99、timeout、成本、fallback rate。
- 断言：超时返回显式 `FUSION_FALLBACK`；无剩余 deadline 时不发起 rerank。

### L16-07 Multi-tenant ACL red team

- 状态：Pending
- 目标：验证 tenant、team、user-private、public 和 ACL 撤销。
- 用例：伪造 tenant、伪造 ACL、连接池身份残留、cache key 碰撞、trace 泄漏、rerank payload 泄漏。
- 阻断断言：deny by default；任一跨租户泄漏立即停止发布。
- 证据：请求身份、返回 ID、服务端审计和负向断言，不保存敏感正文。

### L16-08 Update, tombstone and delete

- 状态：Pending
- 目标：验证 v7/v8 原子可见性、ACL 撤销、tombstone 和物理删除收敛。
- 故障：sparse 成功/dense 失败、Outbox 重投、重复 upsert、删除 worker 中断。
- 指标：index lag、tombstone lag、reconciliation mismatch、恢复时间。
- 阻断断言：旧版与 tombstone 在切换后不可返回。

### L16-09 Embedding/index migration

- 状态：Pending
- 目标：验证 v1/v2 独立索引、shadow query、切换与回滚。
- 变量：模型、预处理、维度、距离、index version。
- 断言：新旧向量不在同一空间混合比较；切换有可审计版本。
- 证据：双读差异、质量/延迟/成本、失败样本、路由事件。

### L16-10 Java client resilience

- 状态：Pending
- 目标：验证 JDBC/Milvus SDK 的连接池、deadline、取消、有限重试与 trace。
- 故障：pool acquire timeout、connect failure、slow read、server timeout、client cancel、partial branch failure。
- 指标：活跃连接、等待线程、取消后占用、重试次数、overall latency。
- 断言：不超过 overall deadline；取消后无无界后台工作；fallback 显式。

### L16-11 pgvector versus Milvus ADR

- 状态：Pending
- 目标：在同一数据、Embedding、查询集、过滤分布和质量目标下比较两种实现。
- 指标：质量、P95/P99、吞吐、索引体积、构建/恢复、更新、资源、运维步骤和成本。
- 断言：结果注明拓扑与版本；不把本机单节点结果外推为集群生产结论。

### L16-12 Capacity and cost envelope

- 状态：Pending
- 目标：建立 top-N、过滤、rerank、并发与成本的联合边界。
- 变量：查询结构、并发、候选数、cache、更新率。
- 指标：质量、长尾、拒绝/超时、资源、每请求成本和月度情景成本。
- 断言：报告适用范围和失效点，不输出脱离配置的“最大 QPS”。

## 4. 统一指标定义

```text
Recall@k = 有关文档在 top-k 中被召回的比例
MRR      = 第一个相关结果倒数排名的平均
nDCG@k   = 考虑分级相关性与位置折损的排序质量
ACL correctness = forbidden chunk 返回次数必须为 0
version correctness = 非 active/无效时间 chunk 返回次数必须为 0
fallback rate = fallback 请求数 / 总请求数
index lag = 源版本提交到可查询 verified index 的时间
```

指标脚本、零相关文档处理、分级规则和平均方式必须版本化，防止同名指标口径不同。

## 5. 证据目录

```text
evidence/<run-id>/
  environment.md
  dataset-manifest.json
  query-set.jsonl
  schema.sql-or-json
  index-config.yaml
  application-config.yaml
  commands.md
  raw-results.jsonl
  latency.csv
  quality.json
  resource.csv
  failure-cases.md
  acl-negative-tests.md
  correctness.md
  conclusion.md
```

`commands.md` 记录实际执行命令而不是计划命令；`raw-results` 不得手工改写。需要脱敏时保留脱敏规则和原始证据的安全位置引用。

## 6. 发布门禁

只有同时满足以下条件，单个实验才能从 Pending 改为 Verified：

1. 固定环境、版本、数据集 hash、完整参数和命令。
2. 保存原始结果、指标计算、失败样本与重复运行。
3. 权限、版本、tombstone 的负向断言全部通过。
4. 质量结果有 exact 或人工标注基线。
5. 延迟包含分位数、并发和测量窗口，不只平均值。
6. 结论明确适用范围、限制和未验证项。

章节标记 `Lab Verified` 还要求 L16-01 至 L16-12 按课程发布规范全部完成，或由课程产品规范明确记录可接受的删减与理由。目前不满足。

## 7. 安全与清理

- 只使用隔离测试 tenant 和可清理 run ID。
- 不在 Git、日志、trace 或 evidence 中保存 Secret。
- 实验删除先验证绝对目标属于实验资源；不执行未经确认的生产清理。
- ACL 服务不可用时 fail closed。
- 模型/reranker 接收的候选必须已经通过权限过滤和脱敏。
- 未运行不得填写 Recall、P99、QPS、成本或“通过”结论。

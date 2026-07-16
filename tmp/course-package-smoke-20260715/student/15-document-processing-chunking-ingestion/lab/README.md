# 第 15 章实验：文档解析、Chunk 与可靠 Ingestion

## 当前状态

- 状态：Pending
- 已完成：实验目标、样本矩阵、断言、证据目录和安全边界设计
- 未完成：组件版本固定、测试语料落盘、Java 服务实现、真实运行和故障注入

未真实运行前，不得填写解析准确率、吞吐、P95/P99、OCR 置信度改进、chunk 最优参数、删除时延或最大容量。

## 一、建议固定环境

运行前在 evidence 中记录：

- 日期、Windows/容器环境、CPU、内存、磁盘和 JDK 21 具体版本。
- Tika/PDFBox/POI/jsoup、OCR 引擎/语言包、数据库、对象存储、Kafka 和 tokenizer 版本。
- 源码 commit/file hash、配置、线程池、资源限制和样本 hash。
- 是否允许网络、是否使用 GPU、临时目录和容器配额。

版本未固定时只允许做设计评审，不允许跨机器比较数字。

## 二、Golden Corpus

测试数据必须脱敏、自建或许可明确。建议至少覆盖：

| 样本 | 关键断言 |
|---|---|
| 单栏数字 PDF | 标题、页码、关键句和顺序 |
| 双栏 PDF | 阅读顺序不交叉 |
| 扫描 PDF | OCR 页、坐标、置信度和错误样本 |
| 混合 PDF | 逐页选择 text/OCR，不重复文本 |
| 含表格 PDF | 表头、行列关系和页锚点 |
| DOCX | 标题、段落、表格、页眉/脚注策略 |
| HTML/Wiki | 标题树、列表、代码、canonical URL |
| XLSX | sheet、表头、类型、公式和值 |
| 图片 | 语言、旋转、低分辨率 warning |
| 加密/损坏文件 | 明确永久失败，不生成 active chunk |
| 超大压缩容器 | 在资源限额内拒绝 |
| 含敏感字段样本 | 脱敏层生效，原始层权限更严格 |

每个样本保存来源/许可证、SHA-256、预期结构和人工审核人。真实企业文件不得直接进入课程仓库。

## 三、实验矩阵

### 实验 1：格式识别与安全入口

- 变量：扩展名与魔数一致/不一致、大小、压缩率、加密、损坏。
- 断言：错误类型稳定；拒绝文件不进入解析；无外部网络访问；资源不超过限额。
- 故障：伪造 MIME、嵌套压缩、超页数、parser timeout。

### 实验 2：PDF 与 OCR 路由

- 比较：数字、扫描、混合 PDF；整文档判断与逐页判断。
- 断言：不重复 OCR 文本；保留页码/坐标；低置信度关键字段产生 warning。
- 证据：逐页路由表、原图 crop、OCR 原始输出和人工标注差异。

### 实验 3：Canonical Document

- 断言：标题树、段落、列表、表格、代码和来源锚点可序列化/反序列化。
- 回归：parser 升级前后比较节点数量、关键短语、表格结构和 warnings。
- 边界：缺失结构显式为 unknown/warning，不伪造页码或标题。

### 实验 4：清洗规则

- 变量：Unicode NFC/NFKC、页眉页脚、断行、连字符、空白和脱敏。
- 断言：代码、错误码、负号、版本号和单位不被误改。
- 证据：逐规则 before/after diff、rule version 和回滚结果。

### 实验 5：Chunk 消融

- 策略：固定 token 窗口、结构优先、结构 + parent/child、不同 overlap。
- 控制：同一 tokenizer、语料、Embedding/检索配置和评测集。
- 指标：Recall@k/MRR、引用定位、context 重复率、chunk token 分布、存储/Embedding 成本。
- 边界：本实验结果只能解释固定语料和配置，不能推出普遍最优 chunk size。

### 实验 6：ACL 传播与越权

- 场景：允许、拒绝、组成员变化、授权服务超时、跨租户 ID 猜测、空 ACL。
- 断言：未授权 chunk 不进入候选集/模型上下文；依赖失败时 fail closed。
- 对账：所有 active chunk 都能回溯到有效 policy reference。

### 实验 7：幂等、崩溃与 UNKNOWN

- 重复投递同一 stage 事件。
- 在“写产物后、提交状态前”终止 worker。
- 调用对象存储/索引超时但远端可能成功。
- 断言：只有一个有效 manifest；恢复通过 hash/状态查询；attempt 可审计。

### 实验 8：v1/v2 原子激活

- v1 正常服务时构建 v2。
- 在 parse、chunk、validate、activate 前分别注入失败。
- 断言：失败时 v1 保持 active；成功切换后查询不混读 v1/v2；旧代际可恢复清理。

### 实验 9：删除与撤权

- 对文档删除、单版本删除、ACL 撤回和保留期到期分别测试。
- 故障：某个派生存储暂时不可用、重复 tombstone、删除 worker 崩溃。
- 断言：查询先不可见；最终所有目标存储对账完成；保留例外有明确审计。

### 实验 10：容量与背压

- 工作负载：小文件、大 PDF、OCR 密集和突发更新混合。
- 观察：stage queue age、CPU、内存、临时磁盘、worker 并发、依赖延迟和 rejection。
- 验证：不同资源池隔离；过载时有界排队/拒绝；恢复不形成重试风暴。

## 四、建议 Java 项目边界

```text
lab/
  README.md
  corpus/
  ingestion-control/
  parser-worker/
  chunk-builder/
  docker-compose.yml
  scripts/
  evidence/
```

当前只存在实验说明。上述目录和代码只有在实际创建后才能列为交付物。

建议模块合同：

- `IngestionController`：认证、幂等受理、状态查询和删除请求。
- `StageJobService`：状态迁移、lease、attempt、错误分类和 Outbox。
- `ArtifactStore`：按 tenant/version/stage/versioned algorithm 写入并校验 hash。
- `ParserClient`：超时、取消、结果 schema 和资源错误映射。
- `ChunkCompiler`：确定性结构切分、token 计算、lineage 和 manifest。
- `GenerationService`：build/validate/activate/retire。
- `DeletionReconciler`：派生存储清理与完成证明。

## 五、证据目录

```text
evidence/<run-id>/
  environment.md
  dependencies.md
  corpus-manifest.csv
  config/
  commands.md
  raw-stdout.log
  raw-stderr.log
  stage-timeline.csv
  metrics.csv
  parser-diffs/
  chunk-manifest.jsonl
  acl-cases.md
  deletion-reconciliation.md
  failure-injection.md
  correctness.md
  conclusion-and-limits.md
```

## 六、最低断言

一次可接受的运行至少证明：

1. 输入样本和组件版本可追溯。
2. 同一幂等请求/重复消息不会产生多个 active 版本。
3. 解析失败或 v2 校验失败不破坏 v1。
4. chunk 全部携带 tenant、version、ACL、source anchor 和算法版本。
5. 未授权内容不进入下游检索上下文。
6. 删除/撤权在各派生存储有对账结果。
7. 故障注入后任务收敛到 ACTIVE、REJECTED、FAILED_CASE 或 DELETED，不无限重试。
8. 所有结论都不超出固定语料、环境和运行证据。

## 七、安全约束

- 只使用脱敏/许可明确数据，不上传真实 Secret、用户 payload 或跨租户文档。
- Parser/OCR worker 禁止默认访问公网和宿主敏感目录。
- 临时目录按租户/run 隔离并在保留策略内清理。
- 日志不输出全文；证据中的样本和截图也需脱敏。
- 安全攻击样本只在隔离环境运行，不在共享生产基础设施测试。
- 未完成以上实验前，本章状态保持 Pending。

# 第 15 章参考答案与评分

## 一、关键答案

### 1. 身份与分层

- source 是连接边界；document 是跨版本逻辑身份；version 是不可变快照。
- parse artifact 必须绑定 parser/config 版本；chunk 必须绑定 chunker/tokenizer 版本和 source node lineage。
- generation 是一批可校验、可切换的索引视图，不能等同于单个 chunk。
- 原始 bytes 用于审计和重建；canonical artifact 用于统一结构；脱敏/清洗/chunk 是可再生派生物。

只用文件名或 URL 作为唯一版本证据不得分满。答案应同时考虑重命名、重复上传、源 revision 和 bytes hash。

### 2. 格式与 OCR

数字 PDF 的主要风险是阅读顺序和版面结构，不是“有没有文字”；扫描 PDF 需要 OCR；混合 PDF 应逐页决策。DOCX 不能只读 paragraph；HTML 不能只去标签；XLSX 不能丢表头、类型与公式；OCR 必须保留引擎版本、置信度、坐标和原图引用。

声称 OCR 后文本等同原文，或只用整份平均置信度决定是否可信，属于错误答案。

### 3. Canonical Model 与清洗

合格模型应是结构树，不是单一 text 字段。节点至少具有 type、text/structured payload、parent/order、source anchor 和 warning。清洗规则要版本化，保留输入输出 hash，并在 golden corpus 上做差异检查。

### 4. Chunk 策略

推荐先按结构切分，再在超长节点内按固定 tokenizer 控制上限。表格重复标题/表头，代码保留语法边界，child chunk 可回溯 parent。overlap 只有在评测提升大于重复噪声和成本时启用。

“统一使用 500 字符 + 50 字符 overlap”若无 tokenizer、语料和评测，只能算待验证假设。

### 5. Metadata 与 ACL

tenant、document/version、source、heading/page/anchor、hash、语言、parser/chunker/tokenizer version、时间有效性、ACL policy、敏感等级和删除状态应随 chunk 传播。授权失败必须 fail closed，不能先检索越权内容再让模型自行忽略。

### 6. 去重

bytes hash 识别完全相同文件；normalized hash 识别正文等价候选；near-duplicate 只用于发现和计算复用。任何去重都必须保留多个来源、ACL、租户和有效期映射，不能因为文本相同就合并可见性。

### 7. 状态机、幂等与 UNKNOWN

合格设计把 stage job 与 attempt 分开，使用 `tenant + version + stage + algorithmVersion` 唯一约束。外部产物先写临时 generation，再提交 manifest。调用超时后先查询 object/index 的 hash 与状态，不盲目重复写。

DLT 是待处理 case，不是成功终态。受控重放应限定 version/stage，使用 preview、审批、速率限制和审计。

### 8. 原子激活

新版本必须在隔离 generation 内完成构建与校验，再由单一控制面切换 active pointer。失败时旧版本保持 ACTIVE。旧代际异步回收，但查询层在切换后不能混读新旧版本。

### 9. 删除与撤权

删除对象至少包括原始层（受保留策略约束）、canonical artifact、chunk、Embedding、关键词/向量索引、缓存、摘要和测试副本。撤权应优先让内容不可见，再异步物理清理。完成证据包括状态、各存储删除回执、孤儿扫描和审计记录。

### 10. Java 边界

Java 控制面负责认证授权、状态机、事务/Outbox、队列、幂等、审计、配额、激活和对账。解析/OCR worker 可使用不同运行时，但必须受 Java 下发的 schema、timeout、tenant、hash 和权限约束。不可信文件不应在 Web 请求线程无限制解析。

## 二、设计题评分锚点

### Chunk 消融实验（10 分）

- 2 分：固定同一语料、tokenizer、Embedding/检索配置和评测集。
- 2 分：至少比较固定窗口、结构切分和一种父子/表格策略。
- 2 分：同时看检索、引用定位、重复率、token/成本和失败样本。
- 2 分：包含表格、代码、跨段问题、错误码和 hard negative。
- 2 分：记录配置与原始结果，不提前宣称最优参数。

### ACL 与删除设计（10 分）

- 3 分：ACL 从 source 到 chunk/query 全链路传播且 fail closed。
- 2 分：处理组成员变化、授权依赖超时和跨租户测试。
- 3 分：删除覆盖全部派生存储并有 tombstone/对账。
- 2 分：定义完成证明、审计与保留期边界。

### 故障恢复（10 分）

- 2 分：job/attempt 与 lease/fencing 或等价接管机制。
- 2 分：幂等键和数据库唯一约束。
- 2 分：暂时/永久/资源/UNKNOWN 分类。
- 2 分：v1 继续服务、v2 隔离构建和原子激活。
- 2 分：DLT 后有人处理、重放受控且避免恢复风暴。

## 三、总评分

| 维度 | 分值 |
|---|---:|
| 文档格式、OCR 与结构模型 | 20 |
| 清洗、Chunk 与 Lineage | 20 |
| Metadata、ACL、版本与删除 | 20 |
| 队列、幂等、失败恢复与 Java 边界 | 20 |
| 质量观测、实验可重复性 | 15 |
| 表达、来源与证据诚实性 | 5 |

## 四、发布判定

- 没有 ACL、版本或删除设计：最高 60 分。
- 没有 golden corpus、失败样本和原始运行证据：不得标记 Lab Verified。
- 静态 schema/DDL 正确但未运行：可以评价设计，不可宣称工程已验证。
- 实验数字没有环境、语料、版本和原始输出：不得写入简历或销售材料。


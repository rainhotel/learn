# 第 15 章讲义：把企业文档变成可信、可重建的知识输入

## 一、Ingestion 不是一次文件上传

企业 ingestion 是持续的数据生命周期：发现知识源、抓取版本、隔离风险、解析结构、清洗内容、切分、补全 metadata、交给 Embedding/索引、原子激活，并在更新、撤权和删除时保持一致。

```text
source discovery
-> fetch immutable bytes
-> quarantine/security checks
-> parse to canonical document
-> normalize and validate
-> chunk + metadata/ACL
-> EMBED_PENDING
-> build index generation
-> validate
-> atomically activate
-> reconcile/update/delete
```

“某次任务返回 200”只证明请求被接受，不证明内容正确、权限正确、版本生效或旧数据已删除。

## 二、先区分六种身份

1. `source`：知识源连接，如某个对象存储 bucket、Wiki space 或文件目录。
2. `document`：跨版本稳定的逻辑文档，如一份 Runbook。
3. `document_version`：一次不可变内容快照，关联 source revision、ETag 或内容哈希。
4. `parse_artifact`：某个解析器版本产生的 canonical document。
5. `chunk`：由确定切分策略产生的可索引单元，保留 lineage。
6. `index_generation`：一批已构建、已校验、可原子切换的索引代际。

不要用文件名当唯一 ID。文件会重命名，同名文件可能不同，URL 查询参数也可能变化。推荐把 source-scoped external ID 作为逻辑身份，把原始 bytes 的 SHA-256、源 revision、大小和抓取时间作为版本证据。

## 三、原始层不可变，派生层可重建

原始 bytes 应写入隔离对象存储，并记录哈希、MIME、大小、抓取身份和审计。解析文本、清洗结果、chunk、Embedding 都是派生物，应带上算法/模型/配置版本，可随时从原始层重建。

这个分层解决三类问题：

- 解析器升级后可以回放，而不是重新向数据源请求已经消失的版本。
- 质量事故可以比较“原文件、旧解析、新解析”，定位在哪一层退化。
- 删除时可以按保留策略区分业务删除、索引删除和法务保留。

原始层不是无限保留的借口。保留期、加密、访问审计和删除证明必须由合规策略决定。

## 四、不可信文件先隔离

文件名后缀不等于真实格式。上传入口至少检查大小、允许类型、MIME/魔数、压缩展开比例、嵌套深度、加密文件、宏/外链和恶意内容扫描。解析器应运行在受限进程或容器中，设置 CPU、内存、临时空间、页数、字符数和墙钟超时。

典型失败包括：

- zip bomb 导致磁盘或内存耗尽。
- 构造 PDF 让解析器长时间占用 CPU。
- 文档包含外部资源，解析时触发意外网络请求。
- 多租户共用临时目录造成数据泄露。

业务 Java 服务不应把不可信文档直接在请求线程和主 JVM 内无限制解析。

## 五、格式解析的真实边界

### 5.1 PDF

PDF 描述页面绘制，不天然保存“段落阅读顺序”。数字 PDF 可以提取字符，但双栏、浮动文本、页眉页脚、脚注、表格和连字符可能打乱顺序。扫描 PDF 只有图像；混合 PDF 可能部分页面有文本层、部分需要 OCR。

处理建议：

- 逐页检测可用字符密度，而不是只检测整份文件是否存在文本层。
- 保存页码、字符框、块顺序、图片引用和解析警告。
- 表格优先保留为行列结构，同时生成可检索文本表示。
- 删除重复页眉页脚前保存规则与原文位置，防止删掉正文。

### 5.2 DOCX

DOCX 是 ZIP 容器中的 XML。正文段落之外还有标题级别、表格、页眉页脚、脚注、批注、文本框、图片、超链接和修订痕迹。只遍历 paragraph 会漏内容；是否采纳修订必须成为显式策略。

### 5.3 HTML/Wiki

应保留 DOM 层级、标题、列表、代码块、表格、链接和 canonical URL。导航、广告、侧栏和评论可能是噪声，但基于 CSS 类的清洗规则容易随站点改版失效。内部 Wiki 还需保存 space/page ID、版本、祖先路径和 ACL。

### 5.4 XLSX/CSV

电子表格的语义通常来自 sheet 名、表头、单元格类型、公式、合并单元格和单位。把整张表按纯文本拼接会丢掉行列关系。大表应按业务行组切分，并把表头重复注入每个 chunk；公式值和公式文本应分别记录。

### 5.5 图片与 OCR

OCR 是有损识别，不是“把图片变成事实”。分辨率、倾斜、压缩、语言包、字体、手写、表格和背景都会影响结果。需要保存 OCR 引擎/模型版本、语言、页图哈希、字符或词置信度、坐标和预处理参数。

低置信度不能仅靠一个全局阈值解决：错误码、数字、小数点和单位即使总体置信度高，也可能造成严重业务错误。对高风险字段需要规则校验或人工复核。

## 六、Canonical Document Model

解析器输出不应只有一条字符串。推荐先转换为统一的结构树：

```json
{
  "documentId": "runbook:provider-timeout",
  "versionId": "sha256:...",
  "parser": {"name": "parser-service", "version": "pinned-version"},
  "language": "zh-CN",
  "nodes": [
    {"id": "n1", "type": "heading", "level": 1, "text": "供应商超时处理"},
    {"id": "n2", "type": "paragraph", "text": "...", "page": 2},
    {"id": "n3", "type": "table", "cells": [["错误码", "动作"]], "page": 3}
  ],
  "warnings": ["page_5_ocr_low_confidence"]
}
```

模型应容纳 heading、paragraph、list、table、code、image、caption、footnote、page break 和坐标。不是所有格式都能填满所有字段；缺失必须显式，而不是伪造结构。

## 七、清洗必须可解释、可回滚

清洗常见动作包括 Unicode 规范化、空白归并、断行/连字符修复、页眉页脚去重、控制字符处理、语言检测和敏感字段脱敏。

边界：

- Unicode NFKC 可能把兼容字符合并，影响代码、标识符和法律文本。
- 正则删除页码可能误删正文中的编号。
- 自动拼接连字符可能破坏负号、版本号和英文复合词。
- 脱敏后文本适合检索，但原始访问仍需更严格权限；二者不能混为一份数据。

每个转换应记录 `rule_version`、输入/输出哈希和 warnings，golden corpus 升级回归失败时能够切回旧规则。

## 八、Chunking 是可评测的编译过程

chunk 的目标不是“越小越好”或“越大越好”，而是在检索可命中、上下文完整、引用可定位、成本可控之间取舍。

推荐默认路径：

1. 按标题、段落、列表、表格和代码块保留结构。
2. 小节点合并到父标题语境内。
3. 超长节点再按 token 上限递归切分。
4. 表格按行组切分，并重复表头与标题路径。
5. 代码按类/方法/语法边界切分，避免在字符串或语句中间截断。
6. 仅在评测证明有收益时加入 overlap。

固定字符数不能准确代表 token；中文、英文、代码和 JSON 比例不同。chunk 参数至少记录 tokenizer、目标/最大 token、overlap、结构规则和策略版本。

### 父子 Chunk

检索可以命中较小 child chunk，再向生成上下文扩展到 parent section。这样兼顾定位与完整性，但会增加存储、去重和引用映射复杂度。父子关系必须保留稳定 node ID。

### 稳定性与 Lineage

不要要求正文前面插入一句话后所有 chunk ID 永久不变，这在固定窗口下很难做到。更可行的是保留：

- `documentId`、`versionId`、`strategyVersion`。
- `sourceNodeIds`、标题路径、页码/坐标。
- `chunkOrdinal` 与 `contentHash`。
- `supersedesChunkIds` 或版本级 lineage。

## 九、Metadata 与 ACL 是正文的一部分

每个 chunk 至少携带：tenant、document/version、source URI、标题路径、页码/锚点、内容哈希、语言、parser/chunker 版本、创建/生效/失效时间、ACL policy reference、敏感等级和删除状态。

ACL 有两类实现：

- 快照展开：把用户/组或权限标签写入索引。查询快，但组成员变化需要重建或更新。
- 查询时求值：索引保存 policy reference，检索时调用授权系统。更及时，但依赖延迟和可用性。

任何方案都必须 fail closed。不能因为授权服务超时就跳过过滤，也不能先把越权 chunk 交给模型再在答案层删除。

## 十、版本、增量与删除

更新不是覆盖旧 chunk。安全做法是为新版本构建新的 manifest/index generation，完成数量、ACL、哈希、质量和抽样校验后，原子切换 active pointer，再异步回收旧代际。

```text
v1 ACTIVE
-> ingest v2 BUILDING
-> validate v2
-> activate v2 and retire v1 in one control-plane decision
-> garbage collect v1 with reconciliation
```

源删除、撤权和到期必须生成高优先级 tombstone 事件。删除范围要覆盖 chunk、Embedding、关键词索引、缓存、派生摘要和测试副本。删除任务需要状态、重试、审计、对账和完成证明；“数据库行删了”不代表所有派生数据已消失。

## 十一、去重不是删除相似知识

- bytes hash：识别完全相同文件。
- normalized text hash：识别格式不同但正文相同的版本。
- near-duplicate：使用 shingles、MinHash/SimHash 等发现高度相似内容。

去重不能丢失 provenance 和 ACL。两个租户拥有相同内容，不代表可以共享可见性；两个来源的相同政策也可能有不同有效期。通常保留逻辑来源映射，只对底层派生计算做安全复用。

## 十二、异步状态机、幂等与恢复

建议状态：

```text
DISCOVERED -> FETCHED -> QUARANTINED -> PARSED -> CHUNKED
-> EMBED_PENDING -> INDEX_BUILDING -> VALIDATED -> ACTIVE
        |                 |                  |
      FAILED            FAILED             REJECTED

ACTIVE -> DELETING -> DELETED
```

`job` 与 `attempt` 分离。幂等键可使用 `tenant + document + version + stage + algorithmVersion`，数据库唯一约束负责最后防线。消息至少一次投递时，消费者先检查阶段产物是否已存在且哈希匹配。

失败分类：

- 永久失败：不支持/加密格式、违反策略、文件损坏，进入人工 case。
- 暂时失败：对象存储、OCR、Embedding 服务超时，按预算退避重试。
- 资源失败：超页数、超内存、解析超时，隔离并记录限额。
- UNKNOWN：调用方超时但远端可能成功，先查询产物/状态再决定是否重试。

DLT 只是隔离区，不代表恢复完成。受控重放必须指定 document/version/stage/algorithmVersion，先 preview 影响范围，防止全量恢复风暴。

## 十三、Java 后端实现边界

Java 适合承担确定性控制面：API、身份与 ACL、任务状态机、事务、Outbox、队列消费、幂等、限流、审计、版本激活和删除对账。

解析器可使用 Apache Tika、PDFBox、POI 等 Java 组件，但不可信/重型解析最好隔离为受限 worker。OCR 或版面模型即使使用 Python/GPU 服务，Java 仍负责超时、租户、任务身份、结果 schema、哈希校验和失败收敛。

线程池按资源类型隔离：下载 I/O、解析 CPU/OCR、对象存储写、Embedding RPC 不共用一个无界池。并发由 CPU、内存、临时磁盘、外部配额和队列延迟共同约束。

事务边界示例：数据库事务内写入 stage 状态与 Outbox；大文件和向量写入不放进数据库长事务。外部产物先写临时 generation，校验后由控制面激活。

## 十四、质量与可观测性

至少观察：

- 业务：ingestion success、端到端 freshness、active version、deletion lag。
- 解析：页数、空页率、字符/页、结构节点数、warning 类型、OCR 使用率与置信度分布。
- 切分：chunk 数、token 长度分布、超限、过短、重复率、标题/页码/ACL 缺失率。
- 系统：各 stage 队列年龄、吞吐、失败类别、重试次数、CPU/内存/临时磁盘、依赖延迟。
- 正确性：源文档数与 active manifest 对账、孤儿 chunk、旧代际残留、删除未完成数。

documentId、fileName、URI 不进入指标 tag；它们写日志/Trace 并通过受控查询定位。日志不得包含全文、Secret 或敏感 OCR 内容。

## 十五、测试与升级门禁

golden corpus 应覆盖正常、边界和攻击样本，并为关键结构维护预期：页数、标题树、表格行列、关键短语、ACL、warning、chunk 数量范围和引用坐标。

解析器、OCR、清洗规则、tokenizer 或 chunk 策略升级时：

1. 新旧版本并行生成派生物。
2. 比较结构覆盖、文本差异、关键字段和资源消耗。
3. 用后续检索评测集做端到端回归。
4. 人工检查高风险差异。
5. 通过后创建新 generation；失败则保留旧 active 版本。

本章所有实验在真实运行前均为 Pending，不得填写解析准确率、吞吐、成本或最大容量。


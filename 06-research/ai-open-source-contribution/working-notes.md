# Working Notes

## 2026-06-30 Milvus 贡献方向细化

### 研究问题

用户想把 Milvus 作为第一个重点贡献项目，因为正在学习向量数据库。需要判断哪些方向既能产生真实代码贡献，又适合从学习者身份进入。

### Milvus 官方贡献入口

Milvus `CONTRIBUTING.md` 明确说明欢迎所有人贡献，并把入口分为：

- Go developers: `milvus`
- C++ developers: `milvus`
- Other languages: `pymilvus`, `milvus-sdk-node`, `milvus-sdk-java`
- Docs: `milvus-docs`

开发要求要点：

- 源码开发主要面向 Linux/macOS。
- Go >= 1.21。
- 推荐资源：8GB RAM，50GB free disk。
- 提交前建议运行 `make fmt`、`make static-check`、`make test-go` 或相关包级单测。
- PR 需要 DCO sign-off，即 commit 使用 `git commit -s`。

### 当前 issue 信号

GitHub issue search 抽查：

- open `good first issue`: 25
- open `help wanted`: 12
- open `kind/bug`: 199
- open `kind/feature`: 444
- open `kind/enhancement`: 80
- open `triage/accepted`: 198

重要观察：

- `good first issue` 多为 feature，并不一定都小。
- `good first issue` 与 `triage/accepted` 的交集为 0。
- 对新贡献者更稳的路线是先做复现、测试、日志、错误信息或 SDK/CLI 小能力，再尝试核心 feature。

### 适合用户的贡献方向

#### 方向 1: Go Client / CLI / SDK 小能力

适合阶段：第一批代码 PR。

理由：

- 比核心引擎容易建立本地反馈。
- 能学习 Milvus API、索引参数、搜索请求结构。
- 影响范围较可控。

可关注 issue：

- `#44635` HNSW_SQ in Go Client
- `#27468` Improve milvus cli

#### 方向 2: 日志、错误信息、可观测性

适合阶段：第一批代码 PR。

理由：

- 改动小，容易验证。
- 能熟悉 Milvus 节点、组件 ID、日志链路。

可关注 issue：

- `#21728` Log Module support print NodeID

#### 方向 3: API 行为和限流

适合阶段：第二批 PR。

理由：

- 能学习 Milvus 服务端 API、proxy、quota/rate limit。
- 代码价值比单纯文档更高。

可关注 issue：

- `#24346` Adding rate limiting to the Flush API

#### 方向 4: 查询过滤表达式

适合阶段：学习收益最高，但难度中高。

理由：

- 能深入理解向量数据库里 scalar filter、boolean expression、array、text match 与 vector search 的组合。
- 对“向量数据库怎么从玩具系统变成数据库系统”这个问题很有帮助。

可关注 issue：

- `#24490` bitwise AND and OR operators in filter expression
- `#23867` Support ALL, ANY for Array types
- `#39629` arithmetic operations between multiple fields
- `#50920` fuzzy text matching

#### 方向 5: 新数据类型

适合阶段：中后期。

理由：

- 涉及 schema、序列化、存储、查询、SDK、测试。
- 标了 good first issue，但实际可能跨模块。

可关注 issue：

- `#27577` Support date && datetime type
- `#27467` Implement Timestamp Data Type
- `#27578` Support Blob datatype

#### 方向 6: 存储与部署适配

适合阶段：有云存储、Kubernetes、对象存储经验后。

可关注 issue：

- `#25713` Oracle Cloud Object Storage
- `#26189` CubeFS
- `#40478` apt/yum/homebrew/ansible deployment

### 第一周建议

1. 不急着改核心 C++。
2. 先用 Docker 跑 Milvus standalone。
3. 用 PyMilvus 写 3 个小脚本：
   - create collection + insert + search
   - scalar filter + vector search
   - array/json/text match 如果当前版本支持
4. 选一个低风险 issue：
   - 首选 `#21728`
   - 备选 `#44635`
   - 想挑战查询表达式则看 `#24490`
5. 在 issue 下留言：

```text
Hi, I am learning Milvus and vector databases. I would like to work on this issue.
I will first reproduce/check the current behavior and then propose a small implementation plan.
```

## 2026-06-30 Milvus 实习生友好任务重排

### 用户偏好更新

用户希望一开始从非核心贡献切入，尤其是：

- 文档整理；
- 日志优化；
- 其他一名实习生能够完成的小任务。

不再优先看“远古 good first issue”。筛选标准调整为：

- 近期更新，优先 2026 年仍有活动；
- 非核心引擎路径；
- 1-3 天内能形成 PR；
- 不需要完整理解 QueryNode/DataCoord/StorageV2 等复杂链路；
- 最好能用 Docker + PyMilvus 或纯 Markdown 验证。

### 最适合第一批尝试的文档任务

1. `milvus-io/milvus-docs#3481`
   - 标题：Clarify list_collections usage vs has_collection in MilvusClient docs
   - 类型：文档澄清
   - 实习生友好度：高
   - 理由：范围小，围绕 API 使用场景，能通过 PyMilvus 小脚本验证。

2. `milvus-io/milvus-docs#3491`
   - 标题：Improve documentation for bulk_import
   - 类型：SDK/API reference 文档补充
   - 实习生友好度：中高
   - 理由：需要对照参数列表和示例，适合学习 API，但可能涉及生成文档来源。

3. `milvus-io/milvus-docs#3502`
   - 标题：CDC overview architecture diagram is out-of-date
   - 类型：架构图/文档一致性
   - 实习生友好度：中
   - 理由：文档改动本身不难，但需要确认 Milvus CDC 2.6 架构，不适合作为完全零背景的第一个 PR。

4. `milvus-io/milvus-docs#3393`
   - 标题：Elaborate AiSAQ parameters usage and tuning
   - 类型：索引参数说明
   - 实习生友好度：中低
   - 理由：需要理解 AiSAQ 参数和调优，不建议作为第一 PR。

### 日志/可观测性方向

1. `milvus-io/milvus#21728`
   - 标题：Log Module support print NodeID
   - 更新时间：2026-06-29
   - 实习生友好度：中高
   - 理由：虽然 issue 创建较早，但近期有活动，主题非核心，适合学习日志字段、节点 ID、组件初始化。

2. `milvus-io/milvus#33492`
   - 标题：Add one field in access log to distinguish system error from user error
   - 更新时间：2026-06-16
   - 实习生友好度：中
   - 理由：access log 字段设计较清晰，但要理解错误码分类和 proxy access log。

3. `milvus-io/milvus#50206`
   - 标题：Support dynamic updates for access log configuration
   - 更新时间：2026-06-01
   - 实习生友好度：中低
   - 理由：配置热更新涉及运行时配置传播，可能比表面看起来更深。

4. `milvus-io/milvus#49195`
   - 标题：Introduce unified gRPC metrics and gRPC logging across all Milvus components
   - 更新时间：2026-06-28
   - 实习生友好度：低
   - 理由：跨组件统一方案，适合作为长期观察，不适合作为第一 PR。

### 当前推荐第一 PR

首选：

- `milvus-io/milvus-docs#3481`

备选：

- `milvus-io/milvus-docs#3491`
- `milvus-io/milvus#21728`

### 认领留言草稿

```text
Hi, I am new to Milvus and currently learning vector databases.
I would like to work on this issue as my first contribution.
I will first verify the current docs/behavior and then submit a small PR with the proposed clarification.
```

### 实际执行状态

已在本地临时工作副本完成 `milvus-io/milvus-docs#3481` 的文档修改。

- 工作副本：`codex-tmp/milvus-docs-3481`
- 分支：`codex/clarify-list-vs-has-collection`
- 修改文件：`site/en/userGuide/collections/view-collections.md`
- 改动：在 `List Collections` 示例结果后增加 note，说明 `list_collections()` 用于返回所有 collection 名称，单个 collection 存在性检查应使用 `has_collection()`。
- 检查：`git diff --check` 通过。

建议 PR 标题：

```text
docs: clarify collection existence checks
```

建议 PR 描述：

```text
## What

Clarifies that `list_collections()` is intended to retrieve all collection names, and that `has_collection()` should be used when checking whether a specific collection exists.

## Why

This addresses #3481 and helps users avoid scanning the full collection list for a single existence check.

## Test

- Ran `git diff --check`.
```

# NotifyFlow 企业知识 Ingestion 平台

## 1. 项目目标

把 Runbook、故障手册、发布说明和脱敏技术文档变成可被 Knowledge Assistant 使用的可信知识输入。系统要支持更新、撤权、删除、失败恢复和证据追踪，而不是只做一次性上传 Demo。

## 2. 范围与非目标

本章实现到 `EMBED_PENDING`、索引生成合同和激活控制面。Embedding 模型、向量索引参数、混合检索、rerank 和回答评测由后续章节完成。

首期格式范围建议：数字 PDF、扫描/混合 PDF、DOCX、HTML、XLSX。其他格式显式返回 `UNSUPPORTED_FORMAT`，不静默生成空内容。

## 3. 服务边界

```text
Connector/Upload API
-> Ingestion Control Service (Java)
-> immutable object store
-> metadata DB + Outbox
-> stage queues
-> sandboxed Parser/OCR Workers
-> canonical artifact store
-> Chunk Builder
-> Embedding/Index contract
-> Generation Validator/Activator
-> Deletion Reconciler
```

- Java 控制服务：租户、ACL、状态机、事务、Outbox、幂等、配额、审计和激活。
- Parser Worker：受限进程，输出 canonical document 与 warnings。
- Chunk Builder：确定性策略，输出 chunk manifest 和 lineage。
- Index Adapter：写入指定 generation，不能自行决定 active 版本。

## 4. 核心数据模型

以下 DDL 是设计草案，尚未在数据库运行：

```sql
CREATE TABLE knowledge_document (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  source_id BIGINT NOT NULL,
  external_id VARCHAR(512) NOT NULL,
  active_version_id BIGINT NULL,
  lifecycle_state VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  UNIQUE (tenant_id, source_id, external_id)
);

CREATE TABLE document_version (
  id BIGINT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  source_revision VARCHAR(512),
  byte_sha256 CHAR(64) NOT NULL,
  raw_object_key VARCHAR(1024) NOT NULL,
  mime_type VARCHAR(128) NOT NULL,
  size_bytes BIGINT NOT NULL,
  acl_policy_ref VARCHAR(512) NOT NULL,
  valid_from TIMESTAMP NULL,
  valid_to TIMESTAMP NULL,
  state VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  UNIQUE (document_id, byte_sha256)
);

CREATE TABLE ingestion_stage_job (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  document_version_id BIGINT NOT NULL,
  stage VARCHAR(32) NOT NULL,
  algorithm_version VARCHAR(128) NOT NULL,
  state VARCHAR(32) NOT NULL,
  attempt_count INT NOT NULL,
  artifact_hash CHAR(64),
  lease_until TIMESTAMP NULL,
  last_error_code VARCHAR(64),
  updated_at TIMESTAMP NOT NULL,
  UNIQUE (tenant_id, document_version_id, stage, algorithm_version)
);

CREATE TABLE chunk_manifest (
  id BIGINT PRIMARY KEY,
  document_version_id BIGINT NOT NULL,
  generation_id BIGINT NOT NULL,
  chunker_version VARCHAR(128) NOT NULL,
  chunk_count INT NOT NULL,
  manifest_hash CHAR(64) NOT NULL,
  acl_policy_ref VARCHAR(512) NOT NULL,
  state VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  UNIQUE (document_version_id, generation_id)
);
```

实际实现还应补外键、索引、时间精度、分区/归档和审计表，并通过真实查询计划验证。

## 5. Chunk 合同

```json
{
  "chunkId": "documentVersion:strategy:ordinal:contentHash",
  "tenantId": "t-001",
  "documentId": "runbook:provider-timeout",
  "documentVersionId": "v-20260715",
  "sourceNodeIds": ["n-21", "n-22"],
  "headingPath": ["通知故障", "供应商超时"],
  "pageAnchors": [{"page": 4, "x": 20, "y": 120}],
  "text": "...",
  "tokenCount": 0,
  "contentHash": "sha256:...",
  "aclPolicyRef": "acl:runbook:oncall",
  "validFrom": "2026-07-15T00:00:00Z",
  "validTo": null,
  "parserVersion": "pinned-version",
  "chunkerVersion": "structure-v1",
  "warnings": []
}
```

`tokenCount` 由固定 tokenizer 实际计算，示例中的 `0` 不是运行结果。

## 6. API 合同

### 提交版本

```http
POST /v1/knowledge/documents/{documentId}/versions
Idempotency-Key: sourceId:externalId:sourceRevision
```

响应返回 `ingestionId` 和当前状态。调用方超时后使用同一幂等键查询，不生成新版本。

### 查询状态

```http
GET /v1/knowledge/ingestions/{ingestionId}
```

返回各 stage、attempt、warning、activeVersion 和错误分类，不返回原始全文。

### 删除

```http
DELETE /v1/knowledge/documents/{documentId}
Idempotency-Key: delete:documentId:requestId
```

删除是异步状态机，返回 deletionId。只有各派生存储对账完成后才进入 `DELETED`。

## 7. 更新与原子激活

1. 抓取 v2 原始 bytes 并保存哈希。
2. 解析、清洗、切分，写入独立 generation。
3. 校验 manifest 数量、ACL、hash、warning 阈值和抽样引用。
4. 数据库事务中切换 `active_version_id` 与 generation pointer，并写 Outbox。
5. 查询端只读取 active generation。
6. 延迟回收 v1；回收失败由 reconciliation 扫描。

如果 v2 构建失败，v1 继续服务。不能先删除 v1 再构建 v2。

## 8. ACL 与撤权

ACL 在 source discovery 时读取并写入 version。每次 stage 输入与输出都校验 tenant 和 policy reference。撤权事件优先级高于普通更新：先让旧 generation 在查询层不可见，再异步重建/删除派生物。

授权服务不可用时查询 fail closed；ingestion 可以进入等待/失败状态，但不能用空 ACL 继续发布。

## 9. 幂等与消息语义

- 业务去重：`document + byte hash` 避免同一内容重复建版本。
- stage 去重：数据库唯一键锁定 version/stage/algorithmVersion。
- 产物写入：先写临时 object/generation，再以 manifest hash 提交。
- 消费成功：状态与 Outbox 同事务；消息提交不是业务真相。
- UNKNOWN：先查询远端 object/index 是否存在匹配 hash，再补状态或重试。

## 10. 失败与恢复场景

| 故障 | 预期状态 | 恢复 |
|---|---|---|
| 加密 PDF | FAILED_PERMANENT | 人工提供密码或拒绝 |
| 第 5 页 OCR 超时 | FAILED_RETRYABLE | 有预算重试该 stage，不激活半成品 |
| Parser worker 崩溃 | lease 过期 | 新 worker 按幂等键接管 |
| Chunk 已写但 ack 丢失 | UNKNOWN | 比较 manifest hash 后补完成 |
| v2 校验失败 | REJECTED | v1 保持 ACTIVE |
| ACL 撤回 | DELETING/RESTRICTED | 查询先不可见，后台清除并对账 |
| 重复删除事件 | 同一 deletion job | 幂等返回当前进度 |

## 11. 质量门禁

激活前至少检查：

- 文档、version、tenant、ACL、来源和算法版本不为空。
- 页面/节点/文本量与源文件在合理边界内，异常进入人工复核。
- chunk 没有超过固定 tokenizer 的最大限制。
- 表格 chunk 带表头，代码 chunk 保留语言/文件锚点。
- manifest 数量、hash 与实际写入一致。
- golden query 的关键证据仍能定位到正确页/节点。

阈值必须基于真实语料建立；本章没有给出已验证数值。

## 12. 验收场景

1. 同一事件重复投递，最终只有一个有效 stage 产物。
2. 文档前部插入一段后，lineage 仍能解释新旧 chunk 对应关系。
3. v2 解析失败时 v1 继续可用，新旧版本不混合返回。
4. 用户组撤权后，旧 chunk 在规定时间内不可检索，并有删除对账证据。
5. Parser worker 在写产物后崩溃，恢复不产生两套 active manifest。
6. 扫描 PDF 的低置信度错误码被 warning/规则校验发现。
7. 解析器升级对 golden corpus 产生差异报告，未通过时不切 active generation。

这些场景当前均为设计，实验状态 Pending。


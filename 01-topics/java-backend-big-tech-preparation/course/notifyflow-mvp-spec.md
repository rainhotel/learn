# NotifyFlow 课程级最小纵切规格

## 0. 文档状态

- 状态：`Specification Draft`。
- 目标版本：课程主项目 `V0.1` 的第一条可运行纵切。
- 当前证据：本文件只定义实现与验收合同，不代表源码、镜像、Compose、Kafka、Provider、指标或故障实验已经完成。
- 发布限制：在本文的核心验收测试真实运行、原始证据归档并经复核前，不得标记 `Lab Verified`，不得对外声称“可靠通知链路已经实现”。

本文把第 04、05、07、08、09、11、12 章分散的设计收敛为一个可实现的最小系统。它不是最终生产架构，而是学习者第一次能够从 API 一直追踪到 MySQL、Kafka、Provider、UNKNOWN、对账和 Metrics 的课程级纵切。

## 1. 最小完成定义

学习者在一台满足环境要求的机器上执行一条启动命令后，应得到以下可检查链路：

```text
POST 创建通知任务
-> MySQL 同一事务写 notification_task + event_outbox
-> Outbox Publisher 至少一次发布 TaskAcceptedV1
-> Kafka Consumer 条件领取任务并创建 delivery_attempt
-> Provider Stub 执行可观察的幂等副作用
-> 明确成功时收敛为 SUCCEEDED
-> 响应超时时先收敛为 UNKNOWN
-> Reconciler 查询 Provider Stub
-> UNKNOWN 收敛为 SUCCEEDED / FAILED / MANUAL_REVIEW
-> Actuator/Prometheus 暴露低基数指标
```

“一键启动”只表示所有必要进程可由同一个入口启动并通过健康检查，不自动证明业务正确性。业务正确性必须由独立的验收命令和证据包证明。

### 1.1 核心不变量

1. API 返回 `202 Accepted` 时，任务行与初始 Outbox 行必须同时存在。
2. API 返回失败时，不允许只留下任务或只留下 Outbox 的半提交状态。
3. 相同 `tenantId + requestId` 重复创建时返回同一个任务，不新增业务任务。
4. Outbox 和 Kafka 均允许重复，Provider 可见副作用不能因同一 `attemptId` 重复发生。
5. 数据库事务不得跨越 Kafka 发送或 Provider HTTP 调用。
6. Provider 请求已可能生效但客户端未收到响应时，结果必须是 `UNKNOWN`，不能直接写成失败或立即盲重试。
7. 对账只能依据 Provider 查询结果推进状态；模型、日志文本或异常类型不能替代业务事实。
8. 每次任务、尝试和对账状态更新均使用状态前置条件与 `version` 乐观锁。
9. 任何单任务 ID、requestId、attemptId、recipient、traceId 都不得成为 Metrics 标签。
10. 任意测试结束后必须满足：终态任务数 + 未决任务数 = 成功创建的唯一任务数。

## 2. 范围与非目标

### 2.1 本纵切包含

- Java 21。
- Spring 应用、Spring 事务、Spring Kafka、Micrometer 和 Actuator。
- MySQL 中的任务、投递尝试、Outbox 和对账事实。
- 单节点 Kafka 的发布与消费路径。
- 独立 Provider Stub，支持成功、明确拒绝、提交副作用后延迟响应三种确定性场景。
- `timeout -> UNKNOWN -> reconciliation -> terminal state` 的完整时间线。
- Flyway 管理的 Schema。
- Docker Compose 本地编排、健康检查和验收入口。
- 面向课程证据的日志、数据库快照、Kafka 记录、Provider 副作用计数和 Prometheus 指标。

### 2.2 明确非目标

- 不承诺 MySQL、Kafka 和 Provider 之间的全局 exactly-once。
- 不实现完整的指数退避、Retry Topic、DLT、批量重放、审批和恢复控制台。
- 不引入 Redis、Elasticsearch、向量数据库、RAG 或 Agent。
- 不实现真实短信、邮件、企业微信等供应商接入。
- 不实现 Kubernetes、多机租约、跨区域容灾和生产级高可用 Kafka。
- 不实现前端管理台；本阶段使用 HTTP API、SQL、Metrics 和验收报告观察系统。
- 不做用户登录、复杂 RBAC、计费、配额和完整多租户隔离；只保留 `tenantId` 作为数据模型与幂等边界。
- 不把 Stub 的确定性结果当成真实供应商 SLA、吞吐或可靠性结论。
- 不发布任何 QPS、P99、成功率或优化百分比，除非后续在固定环境真实测量并保存证据。

## 3. 运行拓扑

MVP 采用“模块化单体 + 独立 Stub”，避免为了展示微服务而增加无关复杂度。

```text
                         +---------------------+
Client ---------------->| notifyflow-app      |
                         | API                 |
                         | Outbox Publisher    |----+
                         | Kafka Consumer      |<---|---- Kafka
                         | Delivery Worker     |    |
                         | Reconciler          |    |
                         | Actuator/Metrics    |    |
                         +----------+----------+    |
                                    |               |
                                    v               |
                                  MySQL             |
                                    |               |
                                    +---------------+
                                    |
                                    v
                             provider-stub
```

`notifyflow-app` 可以在后续课程拆成独立 API、Publisher、Worker 和 Reconciler 进程；MVP 先保持一个部署单元，但代码模块和线程池必须分开，避免业务边界与部署边界混为一谈。

## 4. 建议工程模块

```text
notifyflow-mvp/
  pom.xml
  notifyflow-domain/
  notifyflow-application/
  notifyflow-infrastructure/
  notifyflow-boot/
  provider-stub/
  acceptance-tests/
  db/migration/
  deploy/compose.yaml
  deploy/prometheus/
  scripts/course.cmd
  scripts/verify.ps1
  scripts/collect-evidence.ps1
```

| 模块 | 职责 | 依赖约束 |
|---|---|---|
| `notifyflow-domain` | Task、Attempt、状态机、错误分类、领域不变量 | 不依赖 Spring、JDBC、Kafka 或 HTTP 客户端 |
| `notifyflow-application` | 创建任务、领取事件、记录结果、对账用例与事务边界接口 | 只依赖 domain 和抽象端口 |
| `notifyflow-infrastructure` | MySQL Repository、Flyway、Kafka、Provider HTTP Client、Micrometer | 实现 application 定义的端口 |
| `notifyflow-boot` | Spring 配置、REST API、调度器、Listener、健康检查 | 负责组装，不放核心状态规则 |
| `provider-stub` | 幂等副作用、结果查询和故障场景控制 | 独立 Spring 应用和独立内存/本地持久状态 |
| `acceptance-tests` | 黑盒 API、数据库、Provider、Kafka 与 Metrics 断言 | 不通过生产接口修改业务表 |

Maven 必须统一管理 Java release、插件和依赖版本。Spring 依赖由同一个 BOM 管理，禁止子模块各自声明不一致的 Spring/Kafka/Micrometer 版本。

## 5. 版本与环境锁

### 5.1 基线

- Java：21 LTS。
- Spring：与 Java 21 兼容的统一 Spring Boot/Spring Framework 版本线。
- MySQL：课程已有实验基线 `8.0.40`。
- Kafka：与课程第 07 章实验合同一致的单节点 KRaft 版本线。
- 构建：Maven Wrapper 或仓库约定的 `mvn.cmd`。
- 容器：Docker Compose V2。

本规格不把未经下载和运行的镜像标签写成已验证版本。开始实现时必须生成 `environment-lock.md`，记录 Maven 解析后的依赖树、Docker/Compose 版本、镜像 tag 与 digest、操作系统、CPU/内存和运行日期。进入 `Lab Verified` 前，Compose 中的基础镜像必须固定 digest。

### 5.2 数据库 Schema 版本

- 只允许 Flyway 前向迁移创建和修改表结构。
- 本地、测试和 Compose 都必须使用同一组 migration。
- Spring/Hibernate 只做 Schema 校验，不使用自动建表或自动更新。
- 事件结构独立使用 `eventVersion`，不能与数据库 migration 版本混用。

## 6. API 合同

### 6.1 创建任务

```http
POST /api/v1/tasks
Content-Type: application/json
Idempotency-Key: req-20260715-0001
```

```json
{
  "tenantId": "tenant-course",
  "channel": "COURSE_STUB",
  "recipientRef": "recipient-fixture-001",
  "templateCode": "WELCOME_V1",
  "variables": {
    "name": "fixture-user"
  }
}
```

首次创建返回：

```http
HTTP/1.1 202 Accepted
Location: /api/v1/tasks/10001
```

```json
{
  "taskId": 10001,
  "requestId": "req-20260715-0001",
  "status": "ACCEPTED",
  "version": 0
}
```

相同租户和 `Idempotency-Key`、相同规范化请求体再次提交时，返回原任务并标记 `replayed=true`。相同键但请求体不同返回 `409 Conflict`，不能静默复用旧任务。

### 6.2 查询任务

```http
GET /api/v1/tasks/{taskId}
GET /api/v1/tasks/{taskId}/attempts
```

任务详情至少返回：

```json
{
  "taskId": 10001,
  "tenantId": "tenant-course",
  "status": "UNKNOWN",
  "currentAttemptNo": 1,
  "lastErrorCategory": "UNKNOWN",
  "version": 2,
  "createdAt": "2026-07-15T10:00:00Z",
  "updatedAt": "2026-07-15T10:00:04Z"
}
```

API 不返回原始收件人、Provider Secret、完整异常栈或内部连接信息。

### 6.3 课程专用对账入口

生产逻辑以调度器自动扫描为准。为了让验收测试确定性运行，`course` profile 可以开放：

```http
POST /internal/course/v1/reconciliation/drain-one
```

该入口一次只处理一条到期 case，返回处理前后状态；非 `course` profile 不注册此端点。它不能接受任意 SQL、跳过版本锁或直接指定最终结果。

### 6.4 健康与指标

```http
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/prometheus
```

Readiness 至少检查 MySQL 和 Kafka 必要连接。Provider 故障不应让创建任务 API 整体失去 readiness，因为发送是异步链路；Provider 状态通过业务指标单独表达。

## 7. 事件合同

MVP 只要求一个业务事件：`TaskAcceptedV1`。

```json
{
  "eventId": "01J2EXAMPLEEVENT",
  "eventType": "TaskAccepted",
  "eventVersion": 1,
  "occurredAt": "2026-07-15T10:00:00Z",
  "taskId": 10001,
  "tenantId": "tenant-course",
  "channel": "COURSE_STUB",
  "traceparent": "optional-w3c-trace-context"
}
```

- Topic：`notifyflow.task.accepted.v1`。
- Message key：十进制 `taskId` 字符串，保证同任务落入同一分区。
- Kafka Header：`eventId`、`eventType`、`eventVersion`、`contentType`。
- 事件中不放原始 recipient、模板变量或 Secret；Worker 按 `taskId` 从 MySQL 读取受控数据。
- Consumer 不支持的 `eventVersion` 必须停止该记录的业务处理并输出明确错误；MVP 不实现完整 DLT，但不能把未知版本当作 V1 继续执行。

## 8. Provider Stub 合同

### 8.1 投递

```http
POST /provider/v1/deliveries
Idempotency-Key: task-10001-attempt-1
```

```json
{
  "recipientRef": "recipient-fixture-001",
  "templateCode": "WELCOME_V1",
  "variables": {
    "name": "fixture-user"
  }
}
```

Stub 对相同 `Idempotency-Key` 只记录一次可见副作用，并返回相同 `providerRequestId`。这只模拟“供应商支持幂等键”的合同，不证明所有真实供应商都具备该能力。

### 8.2 查询

```http
GET /provider/v1/deliveries/by-idempotency-key/{key}
```

查询结果限定为：`SUCCEEDED`、`REJECTED`、`PENDING`、`NOT_FOUND`。NotifyFlow 只有在查询明确返回 `SUCCEEDED` 或 `REJECTED` 时才能推进到相应终态；`PENDING` 和缺乏明确语义的 `NOT_FOUND` 继续保持 UNKNOWN，超过对账时限进入人工复核。

### 8.3 故障场景

课程 profile 提供仅供本地验收的管理接口：

```http
PUT /internal/course/v1/scenarios/{idempotencyKey}
GET /internal/course/v1/effects/{idempotencyKey}
DELETE /internal/course/v1/scenarios
```

必须支持：

| 场景 | Stub 行为 | NotifyFlow 预期 |
|---|---|---|
| `SUCCEED` | 记录一次副作用并立即返回 2xx | Task/Attempt -> `SUCCEEDED` |
| `REJECT` | 不记录副作用并返回确定性 4xx | Attempt -> `PERMANENT_FAILED`，Task -> `FAILED` |
| `COMMIT_THEN_DELAY` | 先记录成功，再延迟响应到客户端 deadline 之后 | Attempt/Task -> `UNKNOWN`，对账后 -> `SUCCEEDED` |

`effects` 接口至少返回该幂等键的 `sideEffectCount`。UNKNOWN 验收必须证明计数始终为 1，而不是只看最终任务状态。

## 9. DDL 概要

以下字段是实现下限，不代替最终 Flyway SQL。所有业务时间统一以 UTC 写入 `DATETIME(6)`；枚举值由应用层验证，数据库索引按实际查询建立。

### 9.1 `notification_task`

```sql
CREATE TABLE notification_task (
  id                    BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id             VARCHAR(64)  NOT NULL,
  request_id            VARCHAR(64)  NOT NULL,
  request_fingerprint   CHAR(64)     NOT NULL,
  channel               VARCHAR(32)  NOT NULL,
  recipient_ref         VARCHAR(128) NOT NULL,
  template_code         VARCHAR(64)  NOT NULL,
  variables_json        JSON         NOT NULL,
  status                VARCHAR(32)  NOT NULL,
  current_attempt_no    INT          NOT NULL DEFAULT 0,
  last_error_category   VARCHAR(32)  NULL,
  last_error_code       VARCHAR(64)  NULL,
  version               BIGINT       NOT NULL DEFAULT 0,
  created_at            DATETIME(6)  NOT NULL,
  updated_at            DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_request (tenant_id, request_id),
  KEY idx_task_status_updated (status, updated_at, id)
);
```

`request_fingerprint` 是规范化请求体的 SHA-256，用于识别“同幂等键、不同请求体”。它不能包含 Secret，也不能替代对敏感字段的加密与访问控制。

### 9.2 `delivery_attempt`

```sql
CREATE TABLE delivery_attempt (
  attempt_id             VARCHAR(64)  NOT NULL,
  task_id                BIGINT       NOT NULL,
  attempt_no             INT          NOT NULL,
  provider_code          VARCHAR(64)  NOT NULL,
  idempotency_key        VARCHAR(128) NOT NULL,
  provider_request_id    VARCHAR(128) NULL,
  status                 VARCHAR(32)  NOT NULL,
  deadline_at            DATETIME(6)  NOT NULL,
  error_category         VARCHAR(32)  NULL,
  error_code             VARCHAR(64)  NULL,
  version                BIGINT       NOT NULL DEFAULT 0,
  started_at             DATETIME(6)  NOT NULL,
  finished_at            DATETIME(6)  NULL,
  PRIMARY KEY (attempt_id),
  UNIQUE KEY uk_task_attempt (task_id, attempt_no),
  UNIQUE KEY uk_provider_idempotency (provider_code, idempotency_key),
  KEY idx_attempt_recovery (status, deadline_at, task_id),
  CONSTRAINT fk_attempt_task FOREIGN KEY (task_id) REFERENCES notification_task(id)
);
```

### 9.3 `event_outbox`

```sql
CREATE TABLE event_outbox (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  event_id         VARCHAR(64)  NOT NULL,
  aggregate_type   VARCHAR(64)  NOT NULL,
  aggregate_id     VARCHAR(64)  NOT NULL,
  event_type       VARCHAR(128) NOT NULL,
  event_version    INT          NOT NULL,
  partition_key    VARCHAR(128) NOT NULL,
  payload          JSON         NOT NULL,
  status           VARCHAR(16)  NOT NULL,
  attempt_count    INT          NOT NULL DEFAULT 0,
  next_attempt_at  DATETIME(6)  NOT NULL,
  lease_owner      VARCHAR(128) NULL,
  lease_until      DATETIME(6)  NULL,
  published_at     DATETIME(6)  NULL,
  last_error       VARCHAR(512) NULL,
  version          BIGINT       NOT NULL DEFAULT 0,
  created_at       DATETIME(6)  NOT NULL,
  updated_at       DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_outbox_event (event_id),
  KEY idx_outbox_publish (status, next_attempt_at, id),
  KEY idx_outbox_lease (lease_until, id)
);
```

### 9.4 `reconciliation_case`

```sql
CREATE TABLE reconciliation_case (
  case_id               VARCHAR(64) NOT NULL,
  task_id               BIGINT      NOT NULL,
  attempt_id            VARCHAR(64) NOT NULL,
  status                VARCHAR(32) NOT NULL,
  query_count           INT         NOT NULL DEFAULT 0,
  next_query_at         DATETIME(6) NOT NULL,
  deadline_at           DATETIME(6) NOT NULL,
  last_provider_status  VARCHAR(32) NULL,
  version               BIGINT      NOT NULL DEFAULT 0,
  created_at            DATETIME(6) NOT NULL,
  updated_at            DATETIME(6) NOT NULL,
  resolved_at           DATETIME(6) NULL,
  PRIMARY KEY (case_id),
  UNIQUE KEY uk_reconciliation_attempt (attempt_id),
  KEY idx_reconciliation_due (status, next_query_at, case_id),
  CONSTRAINT fk_case_task FOREIGN KEY (task_id) REFERENCES notification_task(id),
  CONSTRAINT fk_case_attempt FOREIGN KEY (attempt_id) REFERENCES delivery_attempt(attempt_id)
);
```

Outbox 重复发布由任务条件领取和 Attempt 唯一约束共同吸收。若后续引入多个不同 consumer，应再增加独立的 `consumed_event` inbox 表，不能假设任务状态能替所有消费组去重。

## 10. 状态机

### 10.1 Task

```text
ACCEPTED
  -> SENDING

SENDING
  -> SUCCEEDED
  -> FAILED
  -> UNKNOWN

UNKNOWN
  -> SUCCEEDED
  -> FAILED
  -> MANUAL_REVIEW
```

| 状态 | 含义 | 是否终态 |
|---|---|---|
| `ACCEPTED` | task 与 Outbox 已提交，等待消费 | 否 |
| `SENDING` | 已创建唯一 Attempt，Provider 调用可能正在进行 | 否 |
| `UNKNOWN` | Provider 副作用可能已经发生，等待查询对账 | 否 |
| `SUCCEEDED` | Provider 明确确认成功 | 是 |
| `FAILED` | Provider 明确拒绝或对账确认失败 | 是 |
| `MANUAL_REVIEW` | 超过对账时限仍无明确结果 | 课程 MVP 的人工终止状态 |

禁止 `UNKNOWN -> SENDING` 的直接转换。未来若要重新发送，必须由带审批、全新 attempt 和明确业务规则的恢复控制面实现。

### 10.2 Attempt

```text
SENDING
  -> SUCCEEDED
  -> PERMANENT_FAILED
  -> UNKNOWN

UNKNOWN
  -> SUCCEEDED
  -> PERMANENT_FAILED
```

对账超时进入人工复核时，Attempt 保持 `UNKNOWN`，由 `reconciliation_case=MANUAL_REVIEW` 表达处置状态，避免伪造一个并不存在的 Provider 结果。

### 10.3 Outbox

```text
PENDING -> PUBLISHED
PENDING -> RETRY -> PUBLISHED
PENDING/RETRY -> FAILED
```

领取依靠 `lease_owner + lease_until`，不是把长时间 Kafka I/O 放进数据库事务。发布确认后更新 `PUBLISHED`；若 Kafka 已接收但数据库标记前进程崩溃，允许再次发布同一 `eventId`。

### 10.4 Reconciliation case

```text
OPEN -> QUERYING -> OPEN
OPEN/QUERYING -> RESOLVED
OPEN/QUERYING -> MANUAL_REVIEW
```

查询返回 `PENDING` 或语义不确定的 `NOT_FOUND` 时，以有上限的退避更新 `next_query_at`。超过 `deadline_at` 后只进入 `MANUAL_REVIEW`，不自动重发。

## 11. 关键事务和并发规则

### 11.1 创建事务

```text
BEGIN
  INSERT notification_task(status=ACCEPTED, version=0)
  INSERT event_outbox(status=PENDING, eventVersion=1)
COMMIT
```

唯一键冲突时读取既有任务并比对 `request_fingerprint`。事务中不发送 Kafka，不调用 Provider。

### 11.2 Consumer 领取

Consumer 收到事件后先开启短事务：

1. 读取 task。
2. 若 task 已是 `SENDING`、`UNKNOWN` 或终态，视为重复或已被其他实例处理，不再创建 Attempt。
3. 计算 `attemptNo=currentAttemptNo+1` 和确定性 `idempotencyKey=task-{taskId}-attempt-{attemptNo}`。
4. 条件更新 `ACCEPTED -> SENDING` 并 `version=version+1`。
5. 插入 `delivery_attempt(status=SENDING)`。
6. 提交后才调用 Provider。

领取成功的 SQL 必须类似：

```sql
UPDATE notification_task
SET status = 'SENDING',
    current_attempt_no = ?,
    version = version + 1,
    updated_at = ?
WHERE id = ?
  AND status = 'ACCEPTED'
  AND version = ?;
```

更新行数不是 1 时，不得继续调用 Provider。

### 11.3 Provider 结果事务

明确成功、明确失败和 UNKNOWN 都使用新的短事务，同时条件更新 Attempt 与 Task。任一乐观锁失败时整个结果事务回滚，重新读取后按当前状态决定是否已经由其他线程完成，不能无条件覆盖。

超时路径必须在同一事务中：

1. `delivery_attempt: SENDING -> UNKNOWN`。
2. `notification_task: SENDING -> UNKNOWN`。
3. 插入唯一 `reconciliation_case(status=OPEN)`。

### 11.4 进程崩溃恢复

定时扫描 `delivery_attempt.status=SENDING AND deadline_at < now`。这类记录不能被当作明确失败，恢复事务必须条件更新为 UNKNOWN，并创建或复用 reconciliation case。该扫描覆盖“Provider 调用期间 JVM 崩溃，Kafka 记录随后重复投递”的窗口。

### 11.5 对账版本锁

Reconciler 领取 case 后查询 Provider，查询发生在数据库事务之外。写回时必须同时检查 case、attempt 和 task 的当前状态及 version。例如：

```sql
UPDATE delivery_attempt
SET status = 'SUCCEEDED',
    provider_request_id = ?,
    version = version + 1,
    finished_at = ?
WHERE attempt_id = ?
  AND status = 'UNKNOWN'
  AND version = ?;
```

旧查询结果晚到且版本已变化时，更新行数为 0；旧结果不得覆盖新事实。

## 12. Metrics 合同

应用使用 Micrometer 语义名，Prometheus 端点会转换为下划线命名。最小指标如下：

| Micrometer 名称 | 类型 | 允许标签 | 说明 |
|---|---|---|---|
| `notifyflow.task.created` | Counter | `channel`, `result` | 创建、幂等复用、冲突 |
| `notifyflow.task.state` | Gauge | `status` | 各状态当前数量 |
| `notifyflow.outbox.publish` | Counter | `result`, `eventType` | 发布成功、失败、重复 |
| `notifyflow.outbox.pending` | Gauge | 无或 `eventType` | 待发布行数 |
| `notifyflow.outbox.oldest.age` | Gauge | 无或 `eventType` | 最老未发布事件秒数 |
| `notifyflow.kafka.consume` | Counter | `result`, `eventType` | claimed、duplicate、unsupported |
| `notifyflow.provider.request` | Timer | `provider`, `result` | success、reject、timeout、io_error |
| `notifyflow.attempt.unknown` | Gauge | `provider` | 当前 UNKNOWN 数量 |
| `notifyflow.reconciliation` | Counter | `provider`, `result` | succeeded、failed、pending、manual |
| `notifyflow.reconciliation.age` | Timer | `provider`, `result` | 从 UNKNOWN 到本次处置的时长 |

标签值必须来自有限白名单。禁止把 `taskId`、`requestId`、`attemptId`、`eventId`、recipient、异常消息或动态 HTTP path 放入 Metrics；这些关联字段只进入受控日志和 Trace。

最小结构化日志字段：

```text
timestamp level service version environment
traceId requestId eventId taskId attemptId
channel provider stateTransition result errorCategory errorCode
```

敏感值不得直接进入日志；验收证据使用固定 fixture，不使用真实个人信息。

## 13. Docker Compose 合同

`deploy/compose.yaml` 至少包含：

| 服务 | 作用 | 健康条件 |
|---|---|---|
| `mysql` | 业务事实和 Flyway Schema | `mysqladmin ping` 成功 |
| `kafka` | 单节点 KRaft Topic | Broker API 可用，目标 Topic 已创建 |
| `provider-stub` | 幂等副作用与故障场景 | readiness 为 UP |
| `notifyflow-app` | API、Publisher、Consumer、Reconciler、Metrics | MySQL/Kafka 就绪且应用 readiness 为 UP |
| `prometheus` | 抓取课程指标 | HTTP ready；可放入 `observability` profile |

约束：

- `notifyflow-app` 和 `provider-stub` 使用 Java 21 runtime image。
- MySQL 和 Kafka 使用 named volume；验收脚本必须提供显式的“保留数据重启”和“全新环境重建”两种模式。
- 应用不得使用固定 sleep 等待依赖，必须依靠 healthcheck、连接重试和有界启动时限。
- Provider Stub 的管理接口只在课程网络和课程 profile 暴露。
- Secret 使用本地 `.env.example` 中的非敏感 fixture；真实 Secret 不进入仓库或证据包。
- Compose 配置通过 `docker compose config` 静态检查后，仍必须实际启动才算运行证据。

Windows 学习者入口设计为：

```text
scripts\course.cmd doctor
scripts\course.cmd up
scripts\course.cmd verify
scripts\course.cmd evidence
scripts\course.cmd down
```

`course.cmd up` 内部执行固定 Compose 文件的 `up --build --wait`，成功后打印 API、Provider 和 Metrics 地址。`doctor` 检查 Docker Engine、Compose V2、端口、磁盘和内存，不自动修改全局系统配置。

## 14. 核心验收测试

所有测试使用唯一 run ID 和隔离 fixture。下表中的“通过条件”是未来实现的验收合同，不表示目前已经通过。

| ID | 场景 | 操作 | 必须证明 |
|---|---|---|---|
| A01 | 干净启动 | 清空课程专用 volume 后执行 `course.cmd up` | 所有必需服务在有界时间内 healthy；Flyway 只执行一次；无人工点选步骤 |
| A02 | 创建原子性 | 正常创建一次；在集成测试中让 Outbox insert 失败一次 | 正常路径 task=1/outbox=1；失败路径 task=0/outbox=0 |
| A03 | API 幂等 | 同键同请求并发提交；再用同键不同请求提交 | 前者只生成一个 task/event；后者 409；返回 taskId 一致 |
| A04 | 成功纵切 | Stub=`SUCCEED` 后创建任务 | Task/Attempt 最终 SUCCEEDED；Outbox PUBLISHED；Provider `sideEffectCount=1` |
| A05 | Outbox 重复发布 | 注入“Kafka ack 后、标记 PUBLISHED 前崩溃”，重启 Publisher | 同 eventId 可重复出现；只创建一个 Attempt；Provider 副作用仍为 1 |
| A06 | Consumer 重复与竞争 | 两个 Listener 并发处理同一事件 | 只有一个条件更新成功；只有一个 Provider 调用；失败领取者不执行副作用 |
| A07 | 明确拒绝 | Stub=`REJECT` | Attempt=PERMANENT_FAILED；Task=FAILED；无 reconciliation case；副作用为 0 |
| A08 | timeout -> UNKNOWN | Stub=`COMMIT_THEN_DELAY` 且延迟大于客户端 deadline | HTTP 客户端超时后 Task/Attempt=UNKNOWN，并且恰有一个 OPEN case |
| A09 | UNKNOWN 对账收敛 | 对 A08 执行自动扫描或 `drain-one` | Provider 查询返回成功；Task/Attempt=SUCCEEDED；case=RESOLVED；副作用仍为 1 |
| A10 | 旧版本写入 | 保存旧 version，在对账完成后尝试用旧 version 更新 | 更新行数为 0；终态和 Provider 事实不被覆盖 |
| A11 | 崩溃窗口恢复 | 在 Attempt=SENDING 后、Provider 结果写回前终止应用并等待 deadline | 恢复扫描将其置为 UNKNOWN 并建立唯一 case；不盲目创建第二 Attempt |
| A12 | Kafka 暂停恢复 | 停止 Kafka 后创建任务，再恢复 Kafka | API 仍提交 task+outbox；Outbox age 增长；恢复后发布并最终收敛 |
| A13 | Metrics 契约 | 抓取 `/actuator/prometheus` | 核心指标存在；计数与数据库快照一致；不存在禁止的高基数标签键 |
| A14 | 守恒审计 | 汇总本 run 的 task、attempt、outbox、case 和 Provider effects | 唯一任务数守恒；每个成功/失败事实可追溯；无孤儿 Attempt/Case |
| A15 | 保留数据重启 | 不删除 volume，重启全部 Compose 服务 | 已完成任务保持终态；未决 Outbox/UNKNOWN 可继续恢复；不会重复可见副作用 |

### 14.1 验收脚本退出规则

- 任一断言失败，`verify` 必须返回非零退出码。
- 不能因为最终状态正确而忽略中间重复副作用、非法状态转换或孤儿数据。
- 等待异步收敛必须有明确 deadline，并在超时时输出最后一次数据库、Provider 和 Metrics 快照。
- 重试只用于等待异步状态推进，不得用无限重试掩盖不稳定测试。
- 测试报告必须逐项记录 A01-A15，而不是只输出一个总成功字符串。

## 15. 证据目录合同

每次完整验收写入独立目录：

```text
release-evidence/notifyflow-mvp/<run-id>/
  scope.md
  environment-lock.md
  commands.log
  compose-config.yaml
  container-images.txt
  git-state.txt
  acceptance-summary.md
  test-results/
  api/
    requests.jsonl
    responses.jsonl
  database/
    task-snapshot.csv
    attempt-snapshot.csv
    outbox-snapshot.csv
    reconciliation-snapshot.csv
    invariant-query-results.txt
  kafka/
    topic-description.txt
    event-samples.jsonl
    consumer-group.txt
  provider/
    scenarios.json
    effects.json
  metrics/
    prometheus.txt
    metric-contract-report.md
  fault-timelines/
    outbox-duplicate.md
    timeout-unknown-reconciliation.md
    kafka-recovery.md
    process-crash-recovery.md
  logs/
  checksums.sha256
  known-limitations.md
```

证据要求：

- `commands.log` 保存命令、开始/结束时间和退出码，但必须脱敏。
- 原始日志与整理后的时间线同时保留；整理报告不能替代原始输出。
- 数据库快照必须带 run ID 过滤，不能混入其他运行的数据。
- 每条 fault timeline 写清注入、检测、状态变化、恢复、最终正确性和不证明什么。
- `git-state.txt` 记录 commit 或工作树状态；脏工作树运行必须明确列出差异风险。
- `checksums.sha256` 覆盖证据包中的稳定文件，便于复核过程中发现改动。

只有 A01-A15 全部通过、证据目录完整、环境可重现，且至少一名非作者按 README 独立运行成功后，才可以申请把该纵切标记为 `Lab Verified`。这仍不等于整门课程 `Released`。

## 16. 分阶段实现顺序

### Phase 0：合同与环境锁

1. 确定 Maven 模块、Spring BOM、MySQL/Kafka 镜像和 digest。
2. 固定 API、事件、状态枚举、错误类别和 migration 命名。
3. 建立 `doctor`、测试 run ID、证据目录和 Secret 排除规则。
4. 先写状态机单元测试与 A01-A15 的空测试清单，不能用空断言制造绿色结果。

退出条件：版本锁文件可审查，状态转换表和验收 ID 不再含糊。

### Phase 1：MySQL 与创建事务

1. 实现 Flyway migration。
2. 实现 `POST/GET task`。
3. 完成 task + outbox 同事务与幂等冲突。
4. 完成 A02、A03 和数据库守恒查询。

退出条件：不启动 Kafka 也能证明 API 成功只承诺“持久化等待异步处理”。

### Phase 2：Outbox、Kafka 与成功路径

1. 实现带 lease 的 Publisher 和 `TaskAcceptedV1`。
2. 实现 Consumer 条件领取和唯一 Attempt。
3. 实现 Provider Stub `SUCCEED/REJECT`。
4. 完成 A04、A05、A06、A07。

退出条件：重复发布和并发消费不会产生重复 Provider 副作用。

### Phase 3：UNKNOWN 与对账

1. 实现 Provider 客户端 deadline 和结构化错误分类。
2. 实现 `COMMIT_THEN_DELAY`。
3. 实现 UNKNOWN 原子写入、SENDING 超时扫描和 Reconciler。
4. 完成 A08、A09、A10、A11。

退出条件：至少一条真实本地 HTTP 时间线证明“副作用已发生、响应超时、对账成功、总副作用为 1”。

### Phase 4：Metrics 与运行可诊断性

1. 增加 Actuator、Prometheus、结构化日志和 run ID。
2. 实现 DB-backed Gauge 时控制查询频率，避免 Metrics 抓取压垮数据库。
3. 增加高基数标签自动检查和不变量报告。
4. 完成 A13、A14。

退出条件：状态、积压、UNKNOWN 和对账能够由 Metrics 观察，且 Metrics 与数据库事实不矛盾。

### Phase 5：Compose 与一键验收

1. 实现 healthcheck、依赖等待、固定端口和 named volume。
2. 实现 `course.cmd doctor/up/verify/evidence/down`。
3. 完成 Kafka 暂停恢复与保留数据重启测试 A12、A15。
4. 在全新环境生成第一份完整证据包。

退出条件：作者不手工进入容器、不修改数据库，仍可完成启动、验收和证据收集。

### Phase 6：独立复现与课程回写

1. 由非作者依据文档在干净环境运行。
2. 记录环境检查失败、等待时间、误解点和测试失败原因。
3. 将修订回写到第 04、05、07、08、09、11、12、13 章。
4. 完成技术审校、教学审校和发布门禁复核。

退出条件：陌生学习者可独立复现；仍未通过的整课门禁继续保持 Pending。

## 17. 实现评审清单

提交实现前逐项回答：

- [ ] API 成功是否真的意味着 task 与 outbox 同时提交？
- [ ] 幂等键冲突是否比对请求指纹？
- [ ] Kafka ack 后崩溃是否允许安全重复发布？
- [ ] Consumer 条件更新失败后是否绝不调用 Provider？
- [ ] Provider HTTP 是否在数据库事务之外？
- [ ] timeout 是否进入 UNKNOWN 而不是普通失败或立即重试？
- [ ] SENDING 崩溃窗口是否有 deadline 扫描？
- [ ] 对账结果写回是否同时受状态和 version 保护？
- [ ] `COMMIT_THEN_DELAY` 是否证明副作用为 1？
- [ ] Metrics 是否没有高基数和敏感标签？
- [ ] Compose 静态通过与真实启动是否被分别记录？
- [ ] 验收失败是否返回非零退出码并保留现场？
- [ ] 所有对外描述是否区分设计、静态检查、本地运行和故障验证？

## 18. 完成后允许与不允许的表达

只有本文核心验收真实通过后，才可以在独立项目中受限表达：

> 使用 Java 21、Spring、MySQL Transactional Outbox 与 Kafka 实现 NotifyFlow 最小可靠通知纵切；通过 Provider Stub 注入“副作用已提交但响应超时”，将结果建模为 UNKNOWN，并使用供应商查询对账和乐观锁收敛状态；以数据库快照、Provider 副作用计数和低基数 Metrics 验证重复发布、并发消费与崩溃恢复下的数据正确性。

即使通过，也不允许表达为：生产系统上线、全局 exactly-once、零消息丢失、真实供应商验证、生产级高可用、达到固定 QPS，或属于大烨实习经历。它是独立课程工程项目，结论范围必须跟随证据环境。

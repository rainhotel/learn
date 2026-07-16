# NotifyFlow MVP Phase 0-1 证据

## 1. 范围

运行日期：2026-07-15。

本轮完成的是代码初稿与受限验证：

- 五模块 Maven/Spring 工程骨架。
- 纯 Java domain/application 状态机和创建任务用例。
- JDBC/Flyway task + outbox 原子持久化实现与测试源码。
- REST API 和 Provider Stub 实现与测试源码。
- H2 对 V1 migration 的真实执行。

尚未完成 Maven/JUnit 全量运行、Spring Context 启动、MySQL/Kafka/Compose 和 A01-A15 验收。

## 2. 工程规模

```text
Maven modules: 5
POM files: 6
Java files: 75
Flyway SQL: 1
```

## 3. 已运行证据

### 3.1 纯 Java 核心合同

命令：

```powershell
& course/notifyflow-mvp/scripts/verify-core.ps1
```

真实输出：

```text
NOTIFYFLOW_CORE_CONTRACT_CHECK_PASSED
```

覆盖边界：

- `ACCEPTED -> SENDING` 与非法直接成功转换。
- Map 顺序不影响规范化 SHA-256 指纹。
- Store 的 CREATED/REPLAYED/CONFLICT 结果不会被应用层篡改。

不覆盖 Spring、数据库事务、HTTP、Kafka 或 Provider 网络行为。

### 3.2 Flyway V1 SQL 的 H2 执行

使用 H2 2.3.232 MySQL mode 运行 migration，再查询 `information_schema.tables`。

真实输出：

```text
table_name
delivery_attempt
event_outbox
notification_task
reconciliation_case
(4 rows, 31 ms)
```

这证明 SQL 在该 H2 模式下创建四张表，不证明 MySQL 8.0 migration、索引行为或事务测试已经通过。

### 3.3 静态主源码编译

- domain + application main 使用 Java 21 `javac --release 21` 联合编译成功。
- boot/infrastructure/provider-stub 使用本机缓存依赖完成 `javac -proc:none` 静态编译，退出码 0。

静态编译不等于 Maven reactor、JUnit、Spring Context 或运行态通过。

## 4. Maven 阻塞证据

尝试使用工作区 Maven repository 下载并运行测试时，升级审批服务返回 403；没有绕过审批。

使用本机缓存离线运行时，Spring Boot parent 被 Maven 判定为来自当前不可用仓库，构建失败：

```text
Non-resolvable parent POM
Cannot access aliyunmaven in offline mode
BUILD FAILURE
```

因此本轮不得标记 Maven tests、Spring tests 或 Phase 1 为绿色。

## 5. 当前判断

```text
Domain/application core contract: verified with Java 21 standalone check
V1 schema on H2 MySQL mode: verified
Maven/JUnit/Spring runtime: Pending
MySQL task + outbox transaction: Pending runtime
Provider Stub HTTP behavior: Pending runtime
Kafka/UNKNOWN/reconciliation/metrics/Compose: Not implemented or Pending
```

当前项目状态仍是 `Implementation Draft / Partial Evidence`，不是 `Lab Verified`。

## 6. Phase 2-3 源码推进（尚未运行）

已继续落盘以下纯 Java 用例和适配器边界：

- Outbox lease、发布结果和重试端口/用例。
- `TaskAccepted` 条件领取、Attempt 创建和 Provider 结果分类。
- Provider `SUCCESS/REJECTED/UNKNOWN` 命令与查询合同。
- UNKNOWN 对账 case、版本推进和人工复核边界。
- JDBC Outbox、Delivery、Reconciliation Store 初稿。
- JDK HttpClient Provider adapter 和 Boot 定时 worker 初稿。

课程模式暂用内存 `CourseTaskAcceptedEventBridge` 代替 Kafka，仅用于验证“数据库事务外发布、消费和 Provider 调用”的边界；它不证明 Kafka 运行、分区、rebalance 或消息持久性。Kafka adapter、Compose 和 A01-A15 仍为 Pending。

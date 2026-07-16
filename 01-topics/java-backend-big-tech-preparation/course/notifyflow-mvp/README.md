# NotifyFlow MVP

## 当前状态

- 阶段：Phase 0-1 implementation draft / partial evidence
- Java：21
- Spring Boot：3.5.11（选择本机已有依赖缓存的 Java 21 兼容版本；仍需完整 Maven 验证）
- 当前范围：领域状态机、任务创建幂等、task + outbox 原子持久化、REST API、Provider Stub
- 尚未完成：Kafka Publisher/Consumer（当前仅有课程内存 bridge）、真实 Spring/JDBC runtime、Compose 一键启动和 A01-A15 全验收
- Phase 2-3 初稿：Outbox lease/重试、TaskAccepted 消费、Provider 结果分类、UNKNOWN 对账、JDK HttpClient adapter、定时 worker 和低基数指标接口已落盘
- 已验证：Java 21 核心合同检查；V1 migration 在 H2 2.3.232 MySQL mode 创建四张表
- 仍 Pending：Maven/JUnit/Spring runtime，审批服务 403 阻止依赖下载与全量测试

本目录是独立课程工程项目，不属于大烨实习。只有真实运行并归档的能力才能写入项目成果。

完整合同见 [NotifyFlow MVP 规格](../notifyflow-mvp-spec.md)。

## 模块

```text
notifyflow-domain          纯 Java 状态与不变量
notifyflow-application     用例和端口
notifyflow-infrastructure  JDBC/Flyway 持久化
notifyflow-boot            REST API 与应用装配
provider-stub              确定性供应商故障模拟
```

## 构建

```powershell
mvn.cmd test
```

如果依赖解析因网络或本地 Maven 仓库权限失败，必须保存退出码和原始错误，不能标记构建通过。

## Phase 1 验收

1. 领域状态机拒绝非法转换。
2. 相同 `tenantId + requestId` 和相同 payload 返回同一任务。
3. 相同幂等键但不同 payload 返回冲突。
4. 首次创建在同一事务写入一条 task 和一条 outbox。
5. Outbox 写入失败时 task 不得残留。
6. `POST /api/v1/tasks` 返回 `202 Accepted`；查询 API 能读取事实状态。

## Phase 2-3 当前边界

- 已有 Outbox lease/重试、TaskAccepted 条件领取、唯一 Attempt、Provider 结果分类和 UNKNOWN 对账用例/适配器初稿。
- 课程模式使用 `CourseTaskAcceptedEventBridge` 作为 Kafka adapter 之前的内存边界；它只用于学习状态机和事务外调用，不提供消息持久性、分区、rebalance 或跨进程语义。
- SENDING deadline 扫描、真实 Kafka、MySQL Spring runtime、Provider HTTP runtime、Metrics 抓取和 Compose 仍为 Pending。

这些条件必须由测试和数据库快照证明，README 本身不是证据。

# NotifyFlow Infrastructure

本模块实现 application 端口的 Spring JDBC 适配器，并由 Flyway 管理数据库结构。

`V1__create_notifyflow_schema.sql` 使用 MySQL 8 语法子集；模块测试使用 H2 的
`MODE=MySQL`、`DATABASE_TO_LOWER=TRUE` 和 UTC 会话时区执行同一份 migration。
H2 测试只能证明事务、唯一键竞争和映射逻辑，正式 MySQL 兼容性仍需在 MySQL 8.0.40
集成环境中单独归档证据。

两种数据库都保留原生 `JSON` 列。H2 会把普通字符串参数编码成 JSON 字符串，适配器
因此仅在 H2 下按 UTF-8 字节绑定 JSON 文档；MySQL 仍使用字符串参数并由 JSON 列验证。

创建路径必须遵守：

1. 在同一 Spring 事务中插入 `notification_task` 和 `event_outbox`。
2. `(tenant_id, request_id)` 唯一键冲突后读取已存在任务。
3. 指纹相同返回 replay；指纹不同返回 conflict。
4. 任意 Outbox 写入失败都回滚任务行。

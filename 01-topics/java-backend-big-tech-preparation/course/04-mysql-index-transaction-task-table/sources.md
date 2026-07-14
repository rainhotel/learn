# 来源、版本与验证记录

## A 级：MySQL 官方文档

- [Clustered and Secondary Indexes](https://dev.mysql.com/doc/refman/8.4/en/innodb-index-types.html)
- [Multiple-Column Indexes](https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html)
- [Optimization and Indexes](https://dev.mysql.com/doc/refman/8.4/en/optimization-indexes.html)
- [EXPLAIN Statement](https://dev.mysql.com/doc/refman/8.4/en/explain.html)
- [Consistent Nonlocking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html)
- [Locking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)
- [InnoDB Transaction Isolation Levels](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)
- [InnoDB Locking](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking.html)
- [Deadlocks in InnoDB](https://dev.mysql.com/doc/refman/8.4/en/innodb-deadlocks.html)
- 访问日期：2026-07-14。

## B 级

- 《高性能 MySQL》：查询、索引、事务和运行实践。
- 《MySQL 技术内幕：InnoDB 存储引擎》：聚簇索引、MVCC 和锁。
- 《数据密集型应用系统设计》：事务、消息和外部副作用。

## C 级

- 真实 Java 后端面经中慢 SQL、Explain、索引失效、事务隔离、Redis/MQ 与项目任务设计为高频追问。
- 面经用于决定练习重点，不定义 MySQL 语义。

## 实验环境

- 实验服务：MySQL Community Server 8.0.40。
- 资料核对：MySQL 8.4 Reference Manual。
- 隔离端口：33306，仅绑定 127.0.0.1。
- 数据库：`notifyflow_course`，仅用于课程实验。
- 数据量：50,000 条 `notification_task`。

## 已验证结果

1. 索引前目标查询扫描约 1,000 行并排序，实际约 1.59ms。
2. 复合索引后覆盖扫描，实际约 0.036ms。
3. 选择 JSON payload 时仍走索引但需行访问，实际约 0.054ms。
4. 缺失最左 tenant 条件时扫描 50,000 行并排序，实际约 15.2ms。
5. 两个 `SKIP LOCKED` worker 分别得到不重叠的五条任务。
6. RR 第一次和第二次 count 均为 0；RC 第二次 count 变为 1。
7. 反向锁顺序一个事务成功，另一个收到 `ERROR 1213 (40001)`。

## 限制

- Explain 的成本和耗时受机器、缓存、统计信息、数据分布影响。
- 8.0.40 与 8.4 文档存在版本差异，发布课程时需标注版本。
- `SKIP LOCKED` 适合队列式领取，不应作为所有一致性查询的默认选项。
- 实验 SQL 的 DROP DATABASE 只允许在隔离实例执行。


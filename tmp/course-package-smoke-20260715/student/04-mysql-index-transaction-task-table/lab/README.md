# MySQL 索引与事务实验

## 推荐环境

- MySQL Community Server 8.0.40 或兼容 MySQL 8.0 版本。
- 课程资料依据 MySQL 8.4 文档核对；实验结果记录具体使用 8.0.40。
- 所有实验使用独立数据库 `notifyflow_course`。

## 实验顺序

1. `01-schema-and-seed.sql`：创建任务表并生成 50,000 条数据。
2. `02-index-experiments.sql`：比较建索引前后、覆盖与非覆盖、最左前缀和领取索引。
3. 并行运行 `03` 和 `04`：验证 `FOR UPDATE SKIP LOCKED`。
4. 并行运行 `05` 与插入脚本：验证 REPEATABLE READ 快照。
5. 并行运行 `06` 与插入脚本：验证 READ COMMITTED 每次一致性读使用新快照。
6. 执行 `08`，再并行运行 `09` 和 `10`：制造死锁并观察一个事务成为 victim。

## 连接示例

```powershell
& 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe' `
  '--protocol=TCP' '--host=127.0.0.1' '--port=33306' '--user=root'
```

端口和路径应按自己的环境调整。不要对包含业务数据的数据库执行本实验。

## 关键预期

- 无复合索引时目标查询执行全表扫描和排序。
- 创建 `(tenant_id, status, created_at DESC, id)` 后，目标查询使用复合索引并可覆盖选择列。
- 查询 payload 时需要访问聚簇记录，不再是纯覆盖访问。
- 缺失最左列 `tenant_id` 时，该索引不能直接承担相同的查找路径。
- 两个 worker 使用 `SKIP LOCKED` 时得到不重叠的任务集合。
- REPEATABLE READ 同一事务两次一致性读看到同一快照；READ COMMITTED 第二次看到新提交行。
- 反向锁顺序会产生死锁，InnoDB 回滚一个事务；应用必须能重试整个事务。

## 安全

- `01` 会删除并重建 `notifyflow_course`。
- `08` 会删除并重建 `deadlock_demo`。
- 实验只允许在课程隔离实例执行。


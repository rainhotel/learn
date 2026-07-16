# NotifyFlow 项目应用：任务表与领取协议

## 1. 状态与租约字段

任务领取不能只更新 status，还要记录 owner 与 lease：

```sql
ALTER TABLE notification_task
    ADD COLUMN worker_id VARCHAR(64) NULL,
    ADD COLUMN lease_until DATETIME(6) NULL,
    ADD COLUMN attempt_count INT UNSIGNED NOT NULL DEFAULT 0;
```

课程实验 schema 已包含这些字段。

## 2. 领取协议

### 短事务

```sql
START TRANSACTION;

SELECT id
FROM notification_task
WHERE status = 'PENDING'
  AND next_attempt_at <= NOW(6)
ORDER BY next_attempt_at, id
LIMIT 10
FOR UPDATE SKIP LOCKED;

UPDATE notification_task
SET status = 'SENDING',
    worker_id = :worker,
    lease_until = NOW(6) + INTERVAL 30 SECOND,
    attempt_count = attempt_count + 1,
    version = version + 1,
    updated_at = NOW(6)
WHERE id IN (...);

COMMIT;
```

### 事务外执行

worker 使用任务快照调用供应商。数据库锁已经释放，其他任务可以继续领取。

### 条件回写

```sql
UPDATE notification_task
SET status = 'SUCCESS',
    worker_id = NULL,
    lease_until = NULL,
    version = version + 1,
    updated_at = NOW(6)
WHERE id = :id
  AND status = 'SENDING'
  AND worker_id = :worker
  AND version = :version;
```

如果更新行数为 0，说明租约或版本已经失效，当前 worker 不应覆盖别人的结果。

## 3. 租约恢复

扫描：

```sql
UPDATE notification_task
SET status = 'PENDING', worker_id = NULL, lease_until = NULL,
    next_attempt_at = NOW(6), updated_at = NOW(6)
WHERE status = 'SENDING'
  AND lease_until < NOW(6);
```

恢复操作也必须考虑多个实例竞争、重试次数和外部副作用可能已完成的情况。数据库状态恢复不能自动证明供应商未发送。

## 4. 幂等提交

API 使用 `(tenant_id, request_id)` 唯一约束：

- 首次提交插入任务。
- 重复请求捕获 duplicate key，查询并返回原任务 ID。
- 不依赖应用先查再插的竞态流程。

## 5. 索引设计表

| 查询 | 索引 | 备注 |
|---|---|---|
| 租户最近任务 | tenant_id,status,created_at,id | 可覆盖摘要查询 |
| 待领取 | status,next_attempt_at,id | 锁定读与排序 |
| 幂等提交 | tenant_id,request_id UNIQUE | 事实约束 |
| 单任务详情 | PRIMARY(id) | 聚簇定位 |

每个索引都要关联一次 `EXPLAIN ANALYZE` 和写入代价说明。

## 6. 多实例边界

- `SKIP LOCKED` 只协调同一数据库中的事务。
- worker 进程崩溃后依赖 lease_until 恢复。
- 外部发送可能在进程崩溃前已成功、数据库回写前失败，必须使用供应商幂等键或业务去重。
- 多机一致性不是“加一把 synchronized 锁”能解决的。

## 7. ADR 作业

编写三条 ADR：

1. 为什么任务状态使用数据库而不是只用 Redis。
2. 为什么领取事务不包住外部调用。
3. 为什么 `SKIP LOCKED` 适合 worker 队列但不适合普通报表查询。


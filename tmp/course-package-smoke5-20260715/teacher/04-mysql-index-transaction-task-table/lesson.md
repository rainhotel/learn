# MySQL 索引、事务与任务表设计

## 1. 学习目标

完成本章后，你能够：

1. 以 NotifyFlow 的查询和写入模式设计任务表。
2. 解释 InnoDB 聚簇索引和二级索引如何定位数据。
3. 根据过滤、排序、选择列和写入代价设计联合索引。
4. 使用 `EXPLAIN` 和 `EXPLAIN ANALYZE` 验证而不是猜测索引效果。
5. 区分快照读、当前读、隔离级别和锁定读。
6. 设计并发任务领取、租约、幂等和状态转换。
7. 识别死锁、处理死锁并重试完整事务。

## 2. 为什么要学

NotifyFlow 最重要的状态不能只存在内存：

- API 重试不能创建重复任务。
- 任务必须在服务重启后恢复。
- 多实例 worker 不能同时发送同一条外部通知。
- 查询“某租户待发送任务”不能每次扫描几十万行。
- 任务领取不能长时间持有数据库锁等待供应商响应。

数据库既是事实状态源，也是并发协调的一部分。索引决定读取和锁定范围，事务边界决定状态是否可靠，表结构决定幂等与恢复是否有落点。

## 3. 先从工作负载而不是字段列表开始

对 `notification_task` 先列出真实操作：

### 写入

- 创建批次/单条任务。
- 依据 `(tenant_id, request_id)` 防止重复提交。
- 更新状态、版本、尝试次数和租约。
- 写入错误和下一次重试时间。

### 读取

- 查询某租户最近任务。
- 查找待领取任务。
- 查询单个任务详情。
- 查询失败审计。

### 并发

- 多实例同时领取。
- API 与 worker 同时更新。
- 回调和超时重试同时到达。

索引必须服务这些具体模式，不能按照“每个 WHERE 字段都建索引”的直觉设计。

## 4. InnoDB 聚簇索引

InnoDB 表的聚簇索引保存行数据。通常主键就是聚簇索引。

如果没有主键，InnoDB 会尝试使用第一个满足条件的非空唯一索引；再没有则生成隐藏聚簇索引。业务表应显式定义稳定、短、非空主键。

聚簇索引的含义：

- 主键查找直接定位包含行数据的页。
- 主键值会出现在二级索引记录中。
- 主键过长会放大所有二级索引。

因此 NotifyFlow 使用 `BIGINT` 数字主键并把业务幂等键单独建唯一索引，是工程上的空间与语义分离。

## 5. 二级索引与回表

二级索引记录包含：

- 二级索引列。
- 主键列。

如果查询需要的列不在二级索引中，MySQL 先找到二级索引项，再根据主键回到聚簇索引取完整行，这就是回表。

如果查询所需列都在索引中，可以直接从索引返回，形成覆盖索引。覆盖索引减少回表，但索引更宽、写入维护成本更高。

## 6. 联合索引与最左前缀

对于 `(tenant_id, status, created_at, id)`：

- 可以利用 `tenant_id`。
- 可以利用 `tenant_id + status`。
- 可以利用 `tenant_id + status + created_at`。
- 不能把它当作只以 `status` 开始的索引。

索引列顺序应由查询选择性、等值/范围条件、排序和复用场景共同决定。

### 选择一个任务查询

```sql
WHERE tenant_id = ?
  AND status = 'PENDING'
  AND created_at >= ?
ORDER BY created_at DESC
LIMIT 20
```

课程实验的索引：

```sql
CREATE INDEX idx_task_tenant_status_created
ON notification_task (tenant_id, status, created_at DESC, id);
```

它同时覆盖过滤和排序所需的主要路径，并使 `id/status/created_at` 查询成为覆盖访问。

注意：一旦出现范围条件，后续列能否同时充分参与排序和过滤需要查看真实执行计划，不要只套“最左前缀”口诀。

## 7. 索引不是越多越好

索引带来：

- 读取路径改善。
- 插入/更新/删除额外维护。
- 磁盘和 buffer pool 占用。
- 优化器选择复杂度。
- 更宽的二级索引和回表成本。

MySQL 官方文档明确指出，不必要的索引浪费空间，也增加写操作成本。应通过真实查询、执行计划和压测判断。

## 8. EXPLAIN 与 EXPLAIN ANALYZE

`EXPLAIN` 告诉你优化器计划如何处理语句；`EXPLAIN ANALYZE` 在支持的版本中执行语句并提供实际执行信息。

重点观察：

- access type：const、ref、range、index、ALL 等。
- possible keys 与实际 key。
- used key parts。
- estimated rows 与 actual rows。
- Filter、Sort、Table scan、Index range scan。
- 是否 Covering index。
- 真实耗时和 loops。

不能只看到 `key != NULL` 就认为查询优化了。索引可能扫描了大量记录，再过滤少量结果；也可能仍然需要排序或回表。

### 课程实验结果

50,000 条任务数据上，查询 tenant=1 的 PENDING 任务：

- 建复合索引前：使用唯一键的 tenant 前缀，扫描约 1,000 行并排序，实际约 1.59ms。
- 建复合索引后：覆盖索引范围扫描，取 20 行实际约 0.036ms。
- 选择 payload 后：仍使用索引，但需要读取行数据，实际约 0.054ms。
- 缺失最左 tenant 条件：扫描 50,000 行并排序，实际约 15.2ms。

这些是本机、小数据量和特定数据分布的实验结果，不是生产性能承诺。

## 9. 任务表设计

核心字段：

```text
id              技术主键
tenant_id       租户隔离
request_id      客户幂等请求号
status          状态机状态
priority        调度优先级
scheduled_at    计划时间
next_attempt_at 下一次尝试时间
worker_id       当前租约持有者
lease_until     租约截止时间
attempt_count   尝试次数
payload         任务参数
version         乐观锁版本
created_at      创建时间
updated_at      更新时间
```

### 唯一幂等键

```sql
UNIQUE KEY uq_task_tenant_request (tenant_id, request_id)
```

唯一约束是数据库层事实保障，不能只在 Java `containsKey` 后再 insert。

### 状态机

```text
PENDING -> SENDING -> SUCCESS
                    `-> FAILED -> PENDING（可重试）
```

状态转移应有条件和版本，避免晚到回调把 SUCCESS 改回 PENDING。

## 10. 事务与 ACID

### Atomicity

事务内操作要么全部提交，要么回滚。它不能自动覆盖外部短信/邮件副作用。

### Consistency

约束、状态规则和应用逻辑共同维护业务不变量。

### Isolation

并发事务之间的可见性和锁行为由隔离级别与访问类型决定。

### Durability

提交后的数据在数据库配置和故障模型下持久化；应用不能把“收到 commit 返回”理解为外部副作用已经完成。

## 11. MVCC 与一致性读

InnoDB 一致性读使用某个时间点的快照：

- 看见此前已提交的变更。
- 看不见之后提交或未提交的变更。
- REPEATABLE READ（默认）事务内一致性读通常复用第一次读建立的快照。
- READ COMMITTED 每次一致性读建立新快照。

课程实验：

- RR 会话第一次 count=0，另一会话插入 tenant=999 后，第二次仍为 0。
- RC 会话第一次 count=0，另一会话插入 tenant=998 后，第二次为 1。

快照读不是“锁住当前结果”。需要协调领取时使用当前读/锁定读。

## 12. 锁定读与任务领取

```sql
SELECT id, status, next_attempt_at
FROM notification_task
WHERE status = 'PENDING'
  AND next_attempt_at <= NOW(6)
ORDER BY next_attempt_at, id
LIMIT 10
FOR UPDATE SKIP LOCKED;
```

`FOR UPDATE` 获取锁定读，`SKIP LOCKED` 遇到其他事务锁住的行时跳过。它适合任务队列式场景，让多个 worker 领取不同任务；不适合需要完整一致结果集的普通业务查询。

### 正确边界

1. 开启短事务。
2. 领取并更新 `worker_id/lease_until/status`。
3. 提交，释放数据库锁。
4. 在事务外调用供应商。
5. 使用租约/版本条件回写结果。

不要在持有数据库锁时等待远程 API。

## 13. 隔离级别

| 隔离级别 | 典型现象 | NotifyFlow 使用判断 |
|---|---|---|
| READ UNCOMMITTED | 脏读 | 通常不选 |
| READ COMMITTED | 每次一致性读新快照，间隙锁减少 | 适合部分领取/后台场景，需验证 |
| REPEATABLE READ | 默认，事务内快照稳定，next-key 锁 | 普通状态读取常见 |
| SERIALIZABLE | 更强阻塞，吞吐下降 | 只在明确需要时 |

不要把“隔离级别越高越好”当成设计原则。要结合读写模式、锁范围、延迟和死锁风险。

## 14. 记录锁、间隙锁与 next-key lock

- Record lock：锁住索引记录。
- Gap lock：锁住索引记录之间的间隙，阻止插入。
- Next-key lock：记录锁 + 前方间隙锁。
- Insert intention lock：插入前表达插入意图。

InnoDB 的行锁实际上锁的是索引记录。没有索引时可能扫描并锁住隐藏聚簇索引范围，扩大影响。因此 `UPDATE ... WHERE` 和 `SELECT ... FOR UPDATE` 的条件应有合理索引。

## 15. 死锁不是“数据库坏了”

如果事务 A 先锁 id=1 再锁 id=2，事务 B 先锁 id=2 再锁 id=1，就可能循环等待。

MySQL/InnoDB 会检测死锁并回滚一个 victim。应用必须：

- 捕获死锁错误。
- 关闭当前事务上下文。
- 按有限次数和退避重试整个事务。
- 保证事务操作顺序一致。
- 缩短事务并建立合适索引。

不能只重试失败的第二条 SQL，因为前一条状态可能已经回滚。

## 16. 乐观锁

```sql
UPDATE notification_task
SET status = 'SUCCESS', version = version + 1, updated_at = NOW(6)
WHERE id = ?
  AND version = ?
  AND status = 'SENDING';
```

受影响行数为 1 才表示当前 worker 仍拥有合法状态版本。为 0 时可能是租约过期、状态已改变或另一个 worker 先完成。

## 17. 深分页与排序

```sql
WHERE id > ?
ORDER BY id
LIMIT 100
```

已知顺序且需要批量扫描时，seek pagination 通常比 `OFFSET 100000` 更稳定。真实查询仍需根据排序键、过滤和业务一致性设计。

## 18. 事务边界

推荐：

- 事务内只做本地数据库读写和状态转换。
- 外部调用放在事务外。
- 用 Outbox/MQ/状态机连接数据库事实与外部副作用。
- 明确 commit 前后失败路径。

危险：

```text
BEGIN
  锁任务
  调短信供应商（2 秒）
  更新成功
COMMIT
```

它会长时间占锁，放大并发阻塞和死锁。

## 19. 常见错误

### 错误一：字段多就建索引

索引服务查询模式，不服务字段数量。

### 错误二：看到 key 就认为用了好索引

需要看扫描行数、过滤、排序、覆盖和实际耗时。

### 错误三：事务能保证短信发送成功

数据库事务不能回滚外部供应商副作用。

### 错误四：事务锁住任务直到远程调用结束

应使用短领取事务 + 租约/版本回写。

### 错误五：死锁后只重试一条语句

要重试完整事务。

### 错误六：把 RR 快照当成当前任务队列

领取任务要使用正确锁定读和状态协议。

## 20. 本章小结

- InnoDB 的主键、二级索引和回表决定访问成本。
- 联合索引顺序必须来自真实查询模式。
- 覆盖索引减少回表，但会增加写维护和空间成本。
- EXPLAIN ANALYZE 用实际运行验证计划，而不是看索引名称猜测。
- MVCC 快照读和锁定当前读服务不同需求。
- 多实例任务领取需要短事务、锁定读、租约、幂等和版本回写。
- 死锁是可预期的并发结果，应用必须重试整个事务。


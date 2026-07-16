# NotifyFlow 项目应用：从大事务拆成可靠状态机

## 业务目标

NotifyFlow 接收通知请求后，需要可靠完成：

1. 请求去重。
2. 创建通知任务。
3. 发布待执行事件。
4. 调用短信、邮件或企业 IM 供应商。
5. 记录发送结果并支持重试、恢复和对账。

数据库事务只能保证其管理资源内的原子性。供应商调用、MQ 和 Agent 工具执行都属于外部副作用，必须通过工程协议连接，而不是尝试放进一个长事务。

## 失败的第一版

```java
@Transactional
public void send(NotificationCommand command) {
    taskRepository.insert(command);
    auditRepository.insert("CREATED");
    providerClient.send(command); // 网络调用
    taskRepository.markSent(command.taskId());
}
```

### 主要问题

- 供应商超时期间一直占用连接和锁。
- 短信成功后，`markSent` 失败并回滚，外部短信无法撤销。
- 客户端重试可能再次发送。
- 服务进程在供应商成功后、状态更新前崩溃，会形成“结果未知”。
- 事务注解若因自调用没有生效，问题更隐蔽。

## 推荐链路

```text
HTTP Request
    |
    v
CreateTaskService  --短事务-->
  idempotency_record
  notification_task(PENDING)
  outbox_event(NEW)
    |
    v commit
Outbox Publisher --事务外--> MQ
    |
    v
Worker --短事务领取--> task(SENDING, owner, lease, version)
    |
    v commit
Provider Client --事务外--> 短信/邮件/Agent Tool
    |
    v
ResultService --短事务条件更新--> SENT / RETRY_WAIT / FAILED
```

## 数据模型

### `notification_task`

关键字段：

- `id`
- `tenant_id`
- `idempotency_key`
- `channel`
- `status`
- `provider_request_id`
- `worker_id`
- `lease_until`
- `version`
- `attempt_count`
- `next_attempt_at`
- `last_error_code`

建议约束：

- `(tenant_id, idempotency_key)` 唯一约束。
- 状态更新附带当前状态和 `version` 条件。
- 供应商请求使用稳定幂等键，而不是每次重试生成新键。

### `outbox_event`

关键字段：

- `id`
- `aggregate_type`
- `aggregate_id`
- `event_type`
- `payload`
- `status`
- `attempt_count`
- `next_attempt_at`
- `published_at`

任务和 Outbox 事件必须在同一物理事务中写入。

## 服务边界

### 1. 创建任务

```java
@Transactional
public CreateTaskResult create(CreateTaskCommand command) {
    IdempotencyRecord existing = idempotencyRepository.find(command.key());
    if (existing != null) {
        return existing.result();
    }

    NotificationTask task = taskRepository.insertPending(command);
    outboxRepository.insertTaskCreated(task);
    idempotencyRepository.insert(command.key(), task.id());
    return CreateTaskResult.accepted(task.id());
}
```

这一事务只负责数据库原子写入，不调用供应商。

### 2. 发布 Outbox

```text
领取 NEW 事件 -> 提交领取状态 -> 发送 MQ -> 短事务标记 PUBLISHED
```

MQ 发送和状态更新之间仍可能崩溃，因此允许重复发布，消费者必须幂等。

### 3. 领取任务

```text
BEGIN
SELECT ... FOR UPDATE SKIP LOCKED
UPDATE task
  SET status='SENDING', worker_id=?, lease_until=?, version=version+1
COMMIT
```

事务提交后才调用供应商，避免持锁执行慢 I/O。

### 4. 确认结果

```sql
UPDATE notification_task
SET status = 'SENT',
    provider_request_id = ?,
    version = version + 1
WHERE id = ?
  AND status = 'SENDING'
  AND worker_id = ?
  AND version = ?;
```

如果更新行数为 0，说明 owner、version 或状态已变化。旧 worker 不能覆盖新 worker 的结果。

## 异常分类与回滚规则

| 异常 | 示例 | 当前数据库事务 | 后续动作 |
|---|---|---|---|
| 参数/业务校验失败 | 模板不存在 | 回滚或不创建 | 返回明确错误 |
| 数据库运行时异常 | 唯一约束、连接失败 | 默认回滚 | 有限重试或返回失败 |
| 检查异常 | 批量导入文件损坏 | 需明确 `rollbackFor` | 整批回滚或分块处理 |
| 供应商可重试失败 | 超时、429、5xx | 供应商调用不处于领取事务 | 记录 `RETRY_WAIT` |
| 供应商不可重试失败 | 无效号码、权限拒绝 | 独立确认事务 | 记录 `FAILED` |
| 结果未知 | 超时但供应商可能成功 | 不盲目重发 | 查询、对账、稳定幂等键 |

## 自调用重构

### 问题结构

```java
public void createBatch(List<Command> commands) {
    commands.forEach(this::createOne);
}

@Transactional
public void createOne(Command command) {
    // ...
}
```

### 推荐结构

```text
BatchCreateCoordinator
    |
    +--> SingleTaskTransactionService.createOne()
              @Transactional
```

编排和单条事务拆成两个 Bean。每次调用经代理进入，事务边界也更容易测试和调整。

如果业务要求整批原子提交，应把事务放到批量服务的外部入口，而不是依赖内部单条方法注解。

## `REQUIRES_NEW` 决策

### 可以考虑

- 主业务失败也必须保留的安全审计记录。
- 少量、短小、没有反向锁依赖的失败记录。

### 不建议默认使用

- 每条通知都用新事务写普通日志。
- 为了绕过 `UnexpectedRollbackException` 随意改传播行为。
- 在高并发外层事务中循环调用 `REQUIRES_NEW`。
- 依赖它协调数据库与供应商调用。

### 评审问题

1. 内层独立提交是否会造成无法解释的半状态？
2. 外层和内层会不会按不同顺序锁相同数据？
3. 最大并发时，每个请求需要几条连接？
4. 审计是否可以改为外层结束后的独立命令或 Outbox？

## Agent 工具执行映射

Agent 调用外部工具与通知供应商调用具有相同边界：

- 工具可能超时，但实际上已经执行。
- 重试可能重复创建工单、发送消息或控制设备。
- 数据库回滚不能撤销真实世界副作用。
- 工具结果需要 request id、幂等键、状态查询和人工补偿入口。

建议把一次工具执行建模为：

```text
PLANNED -> DISPATCHING -> SUCCEEDED
                      -> RETRY_WAIT
                      -> UNKNOWN -> RECONCILING -> SUCCEEDED / FAILED
```

Agent Runtime 只负责决策还不够，后端必须持久化执行状态并提供恢复协议。

## 测试策略

### 单元测试

- 异常分类和状态转换。
- 幂等键生成。
- 重试次数与退避计算。

### Spring 集成测试

- 代理是否存在。
- 自调用与外部调用差异。
- 默认回滚和 `rollbackFor`。
- 传播行为和 rollback-only。

### 数据库集成测试

- 唯一约束并发竞争。
- 条件更新与 version 防覆盖。
- `SKIP LOCKED` 领取。

### 故障测试

- Outbox 发送成功后、标记成功前杀进程。
- 供应商成功后、确认状态前杀进程。
- 连接池容量小于 `REQUIRES_NEW` 最坏需求。
- Worker lease 过期后旧 worker 返回。

## 验收标准

- 创建任务事务不包含供应商或 MQ 网络调用。
- 任务和 Outbox 事件原子写入。
- MQ 和供应商链路允许重复，但业务结果幂等。
- 任务领取与确认均使用短事务和条件更新。
- 所有事务入口由 Spring Bean 外部调用，或有实验说明例外。
- 每类异常的回滚、重试和最终状态都有测试。
- 能解释数据库提交成功、消息重复、供应商结果未知三类故障如何恢复。

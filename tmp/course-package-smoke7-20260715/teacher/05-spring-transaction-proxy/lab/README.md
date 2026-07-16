# Spring 事务与代理实验

## 实验目标

用真实 Spring AOP 代理和 H2 数据库验证事务语义，不通过手写假事务或只检查注解完成实验。

## 环境

- Windows PowerShell
- Oracle JDK 21.0.6
- Maven 3.9.9
- Spring Framework 7.0.8
- H2 2.3.232
- JUnit Jupiter 5.12.2

## 运行方式

```powershell
mvn.cmd -q test
```

如果本机 Maven 默认缓存不可写，可以在有权限的项目环境中指定本地仓库：

```powershell
mvn.cmd -q "-Dmaven.repo.local=.m2" test
```

课程正式验收必须保留完整测试输出，不能只提供 IDE 绿色截图。

## TDD 顺序

每个实验按以下顺序推进：

1. 写一个只描述目标行为的测试。
2. 运行并确认它因目标能力缺失而失败。
3. 写最小实现。
4. 运行并确认新测试与已有测试全部通过。
5. 重构并再次运行。

## 实验清单

### 1. 代理存在

- 调用：从 Spring 容器取得带 `@Transactional` 方法的服务。
- 断言：`AopUtils.isAopProxy(bean)` 为真。
- 目的：证明注解由运行时基础设施解释，而不是普通 Java 方法自动获得事务。

### 2. 运行时异常默认回滚

- 事务中插入一条事件。
- 抛出 `IllegalStateException`。
- 断言：事件数量为 0。

### 3. 自调用绕过代理

- 外部调用一个非事务方法。
- 该方法使用 `this` 调用带 `@Transactional` 的内部方法。
- 内部方法插入后抛出运行时异常，外部方法捕获。
- 断言：插入被提交，证明内部调用没有建立预期事务。

注意：实验结论是“这一次调用没有经过代理”，不是“Spring 偶尔丢失注解”。

### 4. 检查异常默认提交

- 事务中插入一条事件。
- 抛出自定义检查异常。
- 断言：事件数量为 1。

### 5. `rollbackFor` 改变规则

- 方法配置 `rollbackFor = CheckedBusinessException.class`。
- 插入后抛出该检查异常。
- 断言：事件数量为 0。

### 6. `REQUIRES_NEW` 独立提交

- 外层 `REQUIRED` 插入 `outer`。
- 内层 `REQUIRES_NEW` 插入 `inner` 并提交。
- 外层随后抛出运行时异常。
- 断言：`outer` 不存在，`inner` 存在。

必须通过另一个 Spring Bean 调用内层方法，避免自调用干扰传播行为。

### 7. `UnexpectedRollbackException`

- 外层和内层都使用 `REQUIRED`。
- 内层发生运行时异常并把共享物理事务标记 rollback-only。
- 外层捕获业务异常并正常返回。
- 断言：代理提交阶段抛出 `UnexpectedRollbackException`，所有写入回滚。

### 8. 新线程不继承事务

- 外层事务写入后启动新线程。
- 新线程记录 `TransactionSynchronizationManager.isActualTransactionActive()`。
- 断言：主线程为真，新线程为假。
- 扩展：验证主线程回滚不能自动回滚新线程已经独立提交的写入。

## 预期结果表

| 实验 | 预期 | 实际 | 状态 |
|---|---|---|---|
| 代理存在 | AOP proxy = true | 待运行 | Pending |
| 运行时异常 | 回滚 | 待运行 | Pending |
| 自调用 | 内部事务 advice 未执行 | 待运行 | Pending |
| 检查异常 | 默认提交 | 待运行 | Pending |
| rollbackFor | 回滚 | 待运行 | Pending |
| REQUIRES_NEW | 内层保留、外层回滚 | 待运行 | Pending |
| rollback-only | 抛 `UnexpectedRollbackException` | 待运行 | Pending |
| 新线程 | 不继承事务 | 待运行 | Pending |

## 当前状态

- `pom.xml` 已固定依赖版本。
- 第一个代理测试已写入。
- 当前环境的 Maven 子进程写入被沙箱拒绝，权限审批服务返回 403。
- 因没有真实测试输出，当前不能把任何实验标记为通过。

## H2 与 MySQL 复验

H2 用于快速验证 Spring 代理、异常规则和传播主路径。完成 H2 实验后，还需要使用 MySQL/Testcontainers 复验：

- 实际隔离级别。
- 锁与死锁。
- 连接池容量和 `REQUIRES_NEW`。
- 数据库方言、DDL 和错误码差异。

## 实验报告要求

每个实验记录：

- 测试名称。
- RED 失败原因。
- GREEN 关键实现。
- 数据库最终行数。
- 当前线程是否存在事务。
- 代理类型仅作为观察值，不作为永久契约。
- Spring、Java、数据库和依赖版本。

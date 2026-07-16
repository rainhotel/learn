# 第 5 章资料与验证记录

## 版本基线

- 调研日期：2026-07-14
- Java：Oracle JDK 21.0.6
- Spring Framework 文档版本：7.0.8
- Spring Boot 当前参考文档版本：4.1.0
- 实验计划版本：Spring Framework 7.0.8、H2 2.3.232、JUnit Jupiter 5.12.2

本章优先使用 Spring 官方参考文档和 Javadoc。实验固定依赖版本，避免课程代码随最新版本漂移。

## 一手资料

### 1. Declarative Transaction Management

- URL：https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html
- 用于支持：
  - Spring 声明式事务由 AOP 支持。
  - 默认回滚行为遵循“未检查异常回滚、检查异常不自动回滚”的约定。
  - Spring 不把事务上下文跨远程调用传播；通常也不应让数据库事务横跨远程调用。

### 2. Understanding the Declarative Transaction Implementation

- URL：https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-decl-explained.html
- 用于支持：
  - 声明式事务的核心是 AOP 代理、事务元数据、`TransactionInterceptor` 和 `TransactionManager`。
  - 事务拦截器围绕方法调用开启、提交或回滚事务。

### 3. Using `@Transactional`

- URL：https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
- 用于支持：
  - `@Transactional` 本身只是元数据，必须由事务基础设施激活。
  - 默认 `proxy` 模式只拦截经代理进入的外部方法调用。
  - 同类自调用不会再次经过代理，因此被调用方法上的事务注解不会按预期生效。
  - 接口代理要求事务方法是接口中声明的 `public` 方法；类代理自 Spring 6.0 起可处理部分非公开方法，但不能据此忽略代理边界。
  - 官方建议优先把事务注解放在具体类的方法上，并通过回滚实验验证。

### 4. Rolling Back a Declarative Transaction

- URL：https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html
- 用于支持：
  - 默认对 `RuntimeException` 和 `Error` 回滚。
  - 检查异常默认不回滚。
  - `rollbackFor`、`noRollbackFor` 及其类名形式可以定制规则。
  - 类型安全的异常类型规则优先于容易误匹配的字符串模式规则。

### 5. Transaction Propagation

- URL：https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html
- 用于支持：
  - `REQUIRED` 的多个逻辑事务范围通常映射到同一个物理事务。
  - 内层把共享物理事务标记为 rollback-only、外层仍尝试提交时，会抛出 `UnexpectedRollbackException`。
  - `REQUIRES_NEW` 使用独立物理事务，可独立提交或回滚。
  - `REQUIRES_NEW` 会在外层仍占用连接时再申请连接；连接池过小可能耗尽甚至形成等待死锁。
  - `NESTED` 通常基于 JDBC Savepoint，不等于独立物理事务。

### 6. Proxying Mechanisms

- URL：https://docs.spring.io/spring-framework/reference/core/aop/proxying.html
- 用于支持：
  - 目标实现接口时，Spring 核心框架通常可使用 JDK 动态代理；没有接口时使用 CGLIB 类代理。
  - `final` 类不能被 CGLIB 继承，`final`、`private` 等不可覆盖方法不能被该代理方式增强。
  - `this.method()` 的自调用落在目标对象本身，不经过代理，因此绕过 advice。
  - 官方优先建议重构以避免自调用；`AopContext.currentProxy()` 会让业务代码耦合 Spring AOP，不应作为常规方案。
  - Spring Boot 的默认代理类型可能受配置影响，因此课程不把某个运行时代理类名当作稳定契约。

### 7. `@Transactional` Javadoc

- URL：https://docs.spring.io/spring-framework/docs/7.0.8/javadoc-api/org/springframework/transaction/annotation/Transactional.html
- 用于支持：
  - 传统 `PlatformTransactionManager` 管理的事务通常绑定当前执行线程。
  - 事务上下文不会自动传播到方法中新启动的线程。
  - 默认回滚规则与检查异常、未检查异常的差异。
  - `rollbackFor` 的类型安全规则及字符串模式规则的误匹配风险。

### 8. Spring Boot Reference

- URL：https://docs.spring.io/spring-boot/reference/index.html
- 用于支持：
  - 2026-07-14 访问时当前参考文档版本为 4.1.0。
  - 本章实验刻意直接使用 Spring Framework，减少自动配置对事务机制观察的遮蔽；后续在 NotifyFlow 中再映射到 Spring Boot 自动配置。

## 浏览器核验方式

- 工具：Playwright CLI
- 核验内容：页面标题、文档版本、正文关键段落和 Javadoc 线程边界说明。
- 浏览器快照：保存在仓库现有 `.playwright-cli/` 临时状态目录中，不作为课程长期引用入口。

## 实验验证状态

- 已写入第一个测试：事务 Bean 应由 AOP 代理包装。
- RED 尚未得到有效测试输出：Maven 在受限环境中无法写入本机默认依赖缓存；改到课程目录后，子进程写入仍被沙箱拒绝。
- 尝试请求正常 Maven 写权限时，审批服务返回 `403 Forbidden`，因此当前不能声称测试已失败或通过。
- 下一步：获得 Maven 构建写权限后，先运行现有 RED 测试，再按测试先行补充最小实现。

## 待补资料

- Spring TestContext 事务测试边界。
- HikariCP 连接池容量与 `REQUIRES_NEW` 的工程化实验。
- Testcontainers + MySQL 的方言差异复验。
- Outbox 与事务同步回调的官方资料。

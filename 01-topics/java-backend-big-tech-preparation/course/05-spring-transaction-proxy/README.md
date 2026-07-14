# 第 5 章：Spring 事务、AOP 代理与业务边界

## 章节定位

- 类型：Concept + Lab + Project + Incident + Interview + Teach-back
- 难度：进阶
- 建议学习时间：16-22 小时
- 资料版本：Spring Framework 7.0.8
- 实验版本：Java 21、Spring Framework 7.0.8、H2 2.3.232
- 对应项目：NotifyFlow 任务创建、Outbox、任务领取、供应商调用和状态确认
- 当前状态：完整内容初稿；实验已进入 RED 阶段，等待 Maven 写权限后验证

## 本章解决的问题

你在一个 `@Transactional` 方法中插入通知任务、写审计日志并调用短信供应商。随后数据库更新失败。此时会出现几个关键问题：

1. 哪些数据库操作会回滚？
2. 已发送的短信能不能回滚？
3. 为什么同类方法调用使 `@Transactional` 失效？
4. 为什么捕获内层异常后，外层提交仍可能抛出 `UnexpectedRollbackException`？
5. `REQUIRES_NEW` 为什么可能耗尽连接池？
6. 新线程和 `@Async` 能否继承当前事务？

## 学习顺序

1. `lesson.md`
2. `lab/README.md`
3. `project-application.md`
4. `exercises.md`
5. `answers.md`
6. `interview.md`
7. `teach-back.md`
8. `sources.md`

## 完成标准

- 能画出调用者、代理、事务拦截器、目标对象和事务管理器的调用链。
- 能解释代理模式下自调用失效，而不是只背“事务失效”。
- 能预测运行时异常、检查异常、`rollbackFor` 的提交或回滚结果。
- 能区分逻辑事务、物理事务、`REQUIRED`、`REQUIRES_NEW` 和 `NESTED`。
- 能解释 `UnexpectedRollbackException` 的形成过程。
- 能设计不持有数据库事务执行外部网络调用的 NotifyFlow 链路。
- 实验通过，练习达到 80 分，完成 15 分钟试讲。

## 发布前缺口

- Maven 实验必须获得真实 RED-GREEN 输出。
- H2 结论需要再用 MySQL/Testcontainers 复验方言和连接行为。
- 学习者需要独立完成练习、故障分析和试讲。
- 根据学习者误区修订后，才能从完整初稿进入发布状态。

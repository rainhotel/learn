# 来源、版本与验证记录

## A 级来源

### ThreadPoolExecutor

- [ThreadPoolExecutor, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html)
- 访问日期：2026-07-13。
- 用于核对：线程池作用、核心/最大线程、队列策略、拒绝策略、钩子、关闭和监控 API。

### BlockingQueue

- [BlockingQueue, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/BlockingQueue.html)
- 访问日期：2026-07-13。
- 用于核对：阻塞队列操作形式、内存一致性语义和实现类型。

### ExecutorService

- [ExecutorService, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html)
- 访问日期：2026-07-13。
- 用于核对：`shutdown`、`shutdownNow`、`awaitTermination`、Future 和两阶段关闭示例。

### CompletableFuture

- [CompletableFuture, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html)
- 访问日期：2026-07-13。
- 用于核对：默认异步执行器、组合、超时和异常处理 API。

### Executors

- [Executors, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html)
- 访问日期：2026-07-13。
- 用于核对：工厂方法的实际配置和虚拟线程执行器。

## B 级来源

- Brian Goetz 等，《Java 并发编程实战》：任务执行、取消、关闭和并发设计模型。
- Martin Kleppmann，《数据密集型应用系统设计》：排队、负载和可靠系统思维。

书籍内容用于建立模型；涉及 Java 21 行为时以当前官方 API 为准。

## C 级来源

- 牛客 2026 年公开 Java 后端面经搜索结果。
- 高频信号：线程池参数、执行过程、动态线程池、项目场景和 Kafka/异步任务。
- 面经仅用于确定教学重点，不作为 API 行为依据。

## 实验验证

- 源码：`lab/ThreadPoolLab.java`。
- 环境：Oracle JDK 21.0.6，Windows。
- 验证日期：2026-07-13。
- 结果：三个实验通过，输出 `ALL_EXPERIMENTS_PASSED`。

### 已验证

1. `core=2, max=4, queue=2` 时，六个阻塞任务形成四个活跃线程和两个排队任务，第七个任务被 AbortPolicy 拒绝。
2. `CallerRunsPolicy` 在池和队列饱和时由 `main` 执行任务，提交操作被该任务阻塞约 259ms。
3. `shutdownNow()` 返回两个未开始任务，两个运行任务观察到中断并退出。

### 不应过度推导

- 线程和任务的具体启动顺序不是确定的。
- 受控实验中的时间数据不能代表生产性能。
- `getActiveCount` 等值属于近似统计。

## 待补来源

- JEP 444：虚拟线程设计动机和边界。
- 所选 HTTP 客户端关于连接池和超时的官方文档。
- Micrometer 线程池指标接入文档。
- NotifyFlow 压测报告与真实 ADR。


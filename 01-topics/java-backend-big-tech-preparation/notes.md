# 知识地图与面经信号

## 近期真实面经观察

2026-07-13 在牛客公开搜索页采样的 Java 后端与实习面经中，重复出现：

- 先介绍实习和项目，随后连续追问技术选择与真实贡献。
- HashMap、ConcurrentHashMap、ThreadLocal、线程池参数和执行流程。
- JVM 内存、GC、OOM 排查、类加载。
- Spring IOC、Bean 生命周期、AOP、事务传播与失效。
- MySQL 索引、事务、慢 SQL、Explain、锁和业务场景。
- Redis 数据结构、ZSet、持久化、缓存问题和 Redisson。
- Kafka 顺序性、消息积压、重复消费和可靠性。
- RAG 的知识切分、向量知识库、会话记忆和数据存储。
- 限流实现、滑动窗口、JWT、网络协议和系统设计。

典型样本包括：京东健康后端面经询问向量知识库、Kafka 顺序性、线程池参数；美的后端实习询问 HashMap、ThreadLocal、MySQL 事务、Redisson；Java Agent 项目面试追问 ReAct/Plan、RAG 切分与方向定位。

## 面试官真正验证什么

### 真实性

- 是你做的还是团队做的？
- 代码边界是什么？
- 数据从哪里来？
- 为什么选择这个组件？

### 深度

- 原理是什么？
- 参数如何设置？
- 失败时发生什么？
- 替代方案是什么？

### 工程判断

- 这个系统真的需要 Redis/MQ/向量库吗？
- 如何防止过度设计？
- 如何验证优化有效？
- 如何定位线上故障？

## Java 专精边界

### 必须闭卷实现或使用

- 常用集合和 Comparator。
- 多线程同步、线程池和 CompletableFuture。
- Spring Boot CRUD、事务、参数校验和测试。
- SQL、索引设计和事务边界。
- Redis 常用结构和缓存策略。
- MQ 生产、消费、重试和幂等。

### 必须能够解释

- HashMap put/resize。
- JMM 与 happens-before。
- synchronized/AQS/CAS。
- JVM 内存与 GC。
- Spring 代理和事务失效。
- MySQL MVCC、索引和锁。
- Redis 单线程模型、持久化和锁边界。
- MQ 可靠性与顺序性。

## RAG 完备知识面

1. 数据：来源、权限、版本、清洗、去重。
2. 解析：PDF/HTML/Markdown/表格和元数据。
3. 切分：长度、重叠、语义结构和父子块。
4. 表征：Embedding 模型、维度和语言适配。
5. 索引：HNSW/IVF、构建成本和过滤。
6. 检索：Dense、Sparse、Hybrid、Query Rewrite。
7. 重排：Cross Encoder 或 LLM Rerank。
8. 生成：上下文预算、引用、拒答和防注入。
9. 评测：检索质量、回答质量、延迟和成本。
10. 运行：增量更新、删除、租户隔离和可观测性。


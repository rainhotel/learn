# 课程资料地图

## 使用原则

- 官方文档用于确认事实和版本行为。
- 书籍用于建立系统模型。
- 源码与实验用于验证理解。
- JD 和面经用于决定学习优先级，不作为技术事实来源。
- 不追求把所有资料看完，只读取与当前章节问题直接相关的部分。

## Java

### 官方

- [Dev.java Learn](https://dev.java/learn/)：Java 语言与核心 API 学习入口。
- [JDK 21 Documentation](https://docs.oracle.com/en/java/javase/21/)：Java 21 API、工具和语言资料。
- [OpenJDK JEP Index](https://openjdk.org/jeps/0)：语言/JVM 新特性的设计依据。
- [Java Language Specification](https://docs.oracle.com/javase/specs/jls/se21/html/)：语言行为最终依据。
- [Java Virtual Machine Specification](https://docs.oracle.com/javase/specs/jvms/se21/html/)：JVM 行为依据。

### 书籍

- 《Effective Java》：API 设计、对象模型与工程习惯。
- 《Java 并发编程实战》：并发设计基础；结合现代 Java 验证部分旧 API。
- 《深入理解 Java 虚拟机》：JVM、类加载、GC 与工具。

## Spring

### 官方

- [Spring Framework Reference](https://docs.spring.io/spring-framework/reference/)：IOC、AOP、事务、MVC。
- [Spring Boot Reference](https://docs.spring.io/spring-boot/index.html)：自动配置、测试、Actuator 与部署。
- [Spring Guides](https://spring.io/guides)：最小实践入口。
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/)：Java AI/RAG 集成。

### 阅读重点

- IoC Container、AOP、Transaction Management、Testing。
- Spring Boot Features、Actuator、Testcontainers。

## MySQL

### 官方

- [MySQL 8.4 Reference Manual](https://dev.mysql.com/doc/refman/8.4/en/)：InnoDB、索引、事务、锁、优化器。
- [MySQL EXPLAIN](https://dev.mysql.com/doc/refman/8.4/en/explain.html)：执行计划。

### 书籍

- 《高性能 MySQL》：查询、索引、架构和运行实践。
- 《MySQL 技术内幕：InnoDB 存储引擎》：InnoDB 机制。

## Redis

### 官方

- [Redis Documentation](https://redis.io/docs/latest/)：数据结构、持久化、复制和集群。
- [Redis Commands](https://redis.io/docs/latest/commands/)：命令语义和复杂度。
- [Redisson Reference](https://redisson.pro/docs/)：Java 客户端、锁和同步结构。

### 书籍

- 《Redis 设计与实现》：内部数据结构与核心机制。

## 消息队列

### Kafka 路线

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)：Producer、Broker、Consumer、配置和语义。
- 《Kafka 权威指南》：工程使用和运行模型。

### RocketMQ 路线

- [Apache RocketMQ Documentation](https://rocketmq.apache.org/docs/)：概念、消息类型、生产消费与部署。

课程首版深入 Kafka；RocketMQ 用于中国 Java/先进制造岗位的事务、FIFO 和延迟消息对比。

## 测试、性能与部署

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)。
- [Testcontainers for Java](https://java.testcontainers.org/)。
- [OpenJDK JMH](https://github.com/openjdk/jmh)。
- [Micrometer Documentation](https://docs.micrometer.io/micrometer/reference/)。
- [Docker Documentation](https://docs.docker.com/)。

## 向量数据库与 RAG

- [pgvector](https://github.com/pgvector/pgvector)：PostgreSQL 向量检索扩展。
- [Milvus Documentation](https://milvus.io/docs)：独立向量数据库。
- [LangChain4j Documentation](https://docs.langchain4j.dev/)：Java LLM/Agent/RAG 框架。
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/)：Spring 体系 AI 集成。
- [Elasticsearch Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)：BM25、向量和混合检索。

## 大模型原理与服务

- [Attention Is All You Need](https://arxiv.org/abs/1706.03762)：Transformer 原始论文。
- [The Illustrated Transformer](https://jalammar.github.io/illustrated-transformer/)：用于建立直观图示，关键事实回到论文核对。
- [Hugging Face LLM Course](https://huggingface.co/learn/llm-course/chapter1/1)：Tokenizer、Transformer、训练和推理实践。
- [Hugging Face Transformers Documentation](https://huggingface.co/docs/transformers/index)：模型加载、推理和任务接口。
- [vLLM Documentation](https://docs.vllm.ai/)：推理服务、吞吐和 KV Cache 相关实践。
- [OpenAI Cookbook](https://cookbook.openai.com/)：结构化输出、工具调用、RAG 和评测案例；API 事实以官方产品文档为准。

## Docker

- [Docker Docs](https://docs.docker.com/)：镜像、容器、网络、卷、Compose、Build 和安全。
- [OCI Runtime Specification](https://github.com/opencontainers/runtime-spec)：容器运行时标准。
- [Linux namespaces](https://man7.org/linux/man-pages/man7/namespaces.7.html)：进程隔离原理。
- [Linux cgroups](https://www.kernel.org/doc/html/latest/admin-guide/cgroup-v2.html)：资源控制原理。

## Kubernetes

- [Kubernetes Documentation](https://kubernetes.io/docs/home/)：对象、控制器、网络、配置、调度和故障排查。
- [Kubernetes Concepts](https://kubernetes.io/docs/concepts/)：Pod、Deployment、Service、ConfigMap、Secret、Job。
- [Kubernetes Tasks](https://kubernetes.io/docs/tasks/)：部署、探针、资源管理、扩缩容和调试。
- 《Kubernetes 权威指南》：系统化理解，版本行为以官方文档为准。

## 分布式系统

- 《数据密集型应用系统设计》：复制、分区、事务、流处理和一致性。
- 《分布式系统概念与设计》：系统模型、故障和一致性。
- [Raft Paper](https://raft.github.io/raft.pdf)：共识理解，不要求首期实现。
- [Google SRE Books](https://sre.google/books/)：可靠性、容量、事故和服务目标。
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)：跨服务 Trace、Metrics、Logs。

## 计算机基础

- 《深入理解计算机系统》。
- 《计算机网络：自顶向下方法》。
- 《现代操作系统》。
- RFC 9110/9112：HTTP 语义与 HTTP/1.1。

## 算法

- LeetCode 官方题目。
- 《算法（第 4 版）》用于数据结构与算法模型。
- 《剑指 Offer》用于面试表达与经典题型。

## 求职校准来源

- 企业招聘官网 JD，保存在 `../../06-research/agent-backend-job-market/source-log.md`。
- 牛客 Java 后端/实习公开面经搜索。
- 项目面试后记录的真实追问。

## 资料卡模板

每个来源应记录：

- 标题与链接。
- 作者/组织。
- 版本或发布日期。
- 访问日期。
- 支持哪些课程结论。
- 是否需要二次核验。
- 可否引用、转载或改编。

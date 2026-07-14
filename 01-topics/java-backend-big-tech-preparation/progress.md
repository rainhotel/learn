# Java 后端大厂准备进度

## Snapshot

- Current phase: Phase 0 - 事实核验与基线
- Last updated: 2026-07-14
- Main language: Java

## Milestones

- [x] 建立课程生产标准与章节模板
- [x] 建立第一版权威资料地图
- [ ] 完成课程术语表与风格指南
- [ ] 发布第一章“线程池与异步通知任务”
- [ ] 发布第二章“HashMap 与集合选型”
- [ ] 发布第三章“JMM、volatile 与 synchronized”
- [ ] 发布第四章“MySQL 索引、事务与任务表设计”
- [ ] 完成大烨实习事实台账
- [ ] 完成 Java 80 题基线测试
- [ ] 完成 NotifyFlow MVP
- [ ] 完成并发与线程池实验
- [ ] 完成 MySQL 索引与事务实验
- [ ] 完成 Redis 缓存与限流实验
- [ ] 完成 MQ 可靠消息链路
- [ ] 完成 JVM/生产排障报告
- [ ] 完成 RAG 知识服务和评测集
- [ ] 完成 80-100 道算法题
- [ ] 完成 Java 后端简历
- [ ] 完成 3 次模拟面试

## Weekly Evidence

每周至少提交：

- 1 个可运行功能。
- 1 个原理实验。
- 1 份测试或压测结果。
- 5 个闭卷口述答案。
- 1 次项目设计复盘。

## Course Production Evidence

- 第一章讲义初稿：已完成。
- 第一章 Java 21 实验：已通过，三个实验输出 `ALL_EXPERIMENTS_PASSED`。
- 第一章练习、答案、面试与试讲材料：已完成。
- 第一章发布前缺口：用户完成学习、作业和实际试讲；根据反馈修订。
- 第二章讲义与实验：已完成；实验输出 `ALL_EXPERIMENTS_PASSED`。
- 第二章发布前缺口：用户完成学习、作业和实际试讲；根据反馈修订。
- 第三章讲义与 JMM 实验：已完成；五组实验输出 `ALL_EXPERIMENTS_PASSED`。
- 第三章发布前缺口：用户完成学习、作业和实际试讲；根据反馈修订。
- 第四章讲义与 SQL 实验：已完成；索引、SKIP LOCKED、隔离级别和死锁实验已验证。
- 第四章发布前缺口：用户完成学习、作业和实际试讲；根据反馈修订。
- 第五章 Spring 事务课程包：讲义、NotifyFlow 应用、练习答案、面试、来源和试讲稿已完成初稿。
- 第五章资料核验：Spring Framework 7.0.8 声明式事务、代理、回滚、传播与 `@Transactional` Javadoc 已核对。
- 第五章实验状态：已写入首个代理测试，但 Maven 构建写权限未获批，尚无有效 RED-GREEN 输出。
- 第五章发布前缺口：完成八组事务实验、MySQL/Testcontainers 复验、用户学习和实际试讲。
- 第六章 Redis 课程包：讲义、NotifyFlow 应用、练习答案、面试、来源和试讲稿已完成初稿。
- 第六章资料核验：已覆盖数据结构、SET/EXPIRE、事务、Lua、限流、淘汰、持久化、复制、Sentinel、Cluster、延迟诊断、Spring Data Redis 事务/缓存/序列化。
- 第六章实验状态：Docker CLI 存在，`redis-cli` 未独立安装；八组实验均为 Pending，尚无运行证据。
- 第六章发布前缺口：固定实验版本，完成限流竞态、击穿、eviction、fencing、大 key、Sentinel 和 Spring Data Redis 实验，并完成学习者试讲。
- 第七章 Kafka 课程包：讲义、NotifyFlow Outbox 应用、九组故障实验、练习答案、面试、来源和试讲稿已完成初稿。
- 第七章资料核验：Apache Kafka 4.3、Spring Kafka 4.1.0，以及 RocketMQ 5.0 事务/FIFO/延迟消息官方页面已核对。
- 第七章设计选择：Kafka 为主线；RocketMQ 作为中国 Java/先进制造岗位对照；数据库任务表保留为低规模方案。
- 第七章实验状态：所有 Kafka、Spring Kafka 和 MySQL Outbox 实验均为 Pending，尚无真实运行证据。
- 第七章基础实验包：已固定官方 `apache/kafka:4.3.1`，完成 Compose、Producer/Consumer 配置、环境验收、顺序和 offset/lag 脚本。
- 第七章静态验证：先获得缺少实现的 RED，再获得 `STATIC_CHECKS_PASSED`；Compose 解析和六个 PowerShell 脚本语法检查通过。
- 第七章运行阻塞：Docker Engine 当前未运行，Kafka 容器与两个基础实验仍无运行证据。
- 第七章发布前缺口：固定镜像与框架兼容版本，完成 offset 崩溃、顺序、lag、rebalance、poison message、DLT、最小 ISR 和 Outbox 重复发布实验，并完成学习者试讲。
- 第八章恢复控制面课程包：讲义、NotifyFlow DDL/API、八组实验设计、练习答案、面试、来源和试讲稿已完成初稿。
- 第八章资料核验：Spring Kafka 4.1.0 `DefaultErrorHandler`、`DeadLetterPublishingRecoverer`、non-blocking retry 与事务边界，以及 AWS 超时/重试/退避/抖动一手资料已核对。
- 第八章核心设计：错误分类、单点重试预算、上限指数退避与抖动、retry token、DLT case、Unknown 对账、replay batch、权限审批和审计。
- 第八章实验状态：重试放大和 Full Jitter 已通过 Java 21 TDD 验证，输出分别为 243/3 次调用与 10000/1044 峰值；其余六组 Pending。
- 第八章发布前缺口：实现错误分类、Spring Kafka DLT、Unknown 对账、回调乱序、安全重放和分阶段恢复，完成恢复操作台和学习者试讲。

## Exit Criteria

- `qa.md` 随机抽题正确率 80% 以上。
- 核心项目可以在新环境一键启动并完成演示。
- 能解释所有简历技术词的原理、应用、问题和替代方案。
- 实习事实与独立项目边界清晰，没有虚构经历。

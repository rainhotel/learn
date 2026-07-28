# 跨层系统设计哲学 Resource Map

## Usage Rules

- P0 是主线资源，优先完成；P1 用于专题深化；P2 用于形成工程审美。
- 不要求从头到尾通读所有书。按 `outline.md` 当前阶段定向阅读。
- 每份资料都要转化为机制卡、实验或项目判断，不能只记录“看过”。
- 论文先读问题、假设、保证和限制，再读实现细节。

## P0: 主教材与课程

### 操作系统与计算机系统

- [Operating Systems: Three Easy Pieces](https://pages.cs.wisc.edu/~remzi/OSTEP/)
  - 用途：虚拟化、并发、持久化主教材。
  - 重点：Concurrency、Persistence、Virtual Memory。
  - 先修：C、数据结构基础。

- [MIT 6.1810 Operating System Engineering](https://pdos.csail.mit.edu/6.1810/)
  - 用途：通过 xv6 把抽象落实到内核实现。
  - 重点：系统调用、页表、Trap、锁、文件系统。
  - 先修：C、RISC-V 基础；不必一开始完成所有 Lab。

- [Computer Systems: A Programmer's Perspective](https://csapp.cs.cmu.edu/)
  - 用途：连接 CPU、缓存、虚拟内存、I/O、网络与并发。
  - 重点：第 5、6、9、10、11、12 章。

### 数据库与数据系统

- [CMU 15-445/645 Database Systems](https://15445.courses.cs.cmu.edu/)
  - 用途：Buffer Pool、索引、查询执行、并发控制、WAL 与恢复。
  - 建议：先听课程与做小实验，再决定是否完整实现 BusTub。

- [Designing Data-Intensive Applications](https://dataintensive.net/)
  - 用途：贯穿事务、复制、分片、一致性与流处理。
  - 重点：第 3、5、6、7、8、9 章。
  - 注意：它提供思考框架，不替代数据库和协议实现细节。

### 分布式系统与可靠性

- [MIT 6.5840 Distributed Systems](https://pdos.csail.mit.edu/6.824/)
  - 用途：部分失败、Raft、分布式 KV 和分片。
  - 先修：Go、并发、RPC、基本数据库概念。
  - 建议：Raft Lab 是理解共识最有价值的实践之一。

- [Distributed Systems 4th Edition](https://www.distributed-systems.net/index.php/books/ds4/)
  - 用途：免费体系教材，用于补齐术语和全局地图。

- [Google Site Reliability Engineering](https://sre.google/sre-book/table-of-contents/)
  - 用途：SLO、监控、过载、级联故障、变更和事故管理。
  - 重点：Monitoring、Handling Overload、Cascading Failures。

- [Google SRE Workbook](https://sre.google/workbook/table-of-contents/)
  - 用途：把 SRE 原则落实到告警、容量、事故和演练。

## P0: 生产工程文章

### AWS Builders' Library

- [Challenges with distributed systems](https://aws.amazon.com/builders-library/challenges-with-distributed-systems/)
- [Timeouts, retries, and backoff with jitter](https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/)
- [Using load shedding to avoid overload](https://aws.amazon.com/builders-library/using-load-shedding-to-avoid-overload/)
- [Avoiding insurmountable queue backlogs](https://aws.amazon.com/builders-library/avoiding-insurmountable-queue-backlogs/)
- [Caching challenges and strategies](https://aws.amazon.com/builders-library/caching-challenges-and-strategies/)
- [Leader election in distributed systems](https://aws.amazon.com/builders-library/leader-election-in-distributed-systems/)
- [Workload isolation using shuffle-sharding](https://aws.amazon.com/builders-library/workload-isolation-using-shuffle-sharding/)
- [Implementing health checks](https://aws.amazon.com/builders-library/implementing-health-checks/)

阅读目的：连接理论和真实事故，重点记录假设、退化路径、反馈环和恢复策略。

## P1: 缓存专题

- [RFC 9111 — HTTP Caching](https://www.rfc-editor.org/rfc/rfc9111)
- [RFC 5861 — stale-while-revalidate / stale-if-error](https://www.rfc-editor.org/rfc/rfc5861)
- [Scaling Memcache at Facebook](https://www.usenix.org/conference/nsdi13/technical-sessions/presentation/nishtala)
- [Redis Caching](https://redis.io/solutions/caching/)
- [Redis Eviction Policies](https://redis.io/docs/latest/develop/reference/eviction/)

带着问题阅读：谁是事实源、允许多旧、失效如何传递、全 miss 时系统是否存活。

## P1: 并发、事务与锁

- [Linux Kernel Locking Documentation](https://docs.kernel.org/locking/)
- [PostgreSQL Transaction Isolation](https://www.postgresql.org/docs/current/transaction-iso.html)
- [PostgreSQL Explicit Locking](https://www.postgresql.org/docs/current/explicit-locking.html)
- [SQLite Atomic Commit](https://www.sqlite.org/atomiccommit.html)
- [PostgreSQL WAL Introduction](https://www.postgresql.org/docs/current/wal-intro.html)
- [Redis Distributed Locks](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/)
- [How to do distributed locking — Martin Kleppmann](https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html)
- [The Chubby Lock Service](https://research.google/pubs/the-chubby-lock-service-for-loosely-coupled-distributed-systems/)
- [ZooKeeper Recipes](https://zookeeper.apache.org/doc/current/recipes.html)

Redis 官方算法与 Kleppmann 的批评应配对阅读，重点区分效率锁、正确性锁、租约和 Fencing。

## P1: 基础论文主线

### 系统边界、时间与不可能性

- [End-to-End Arguments in System Design](https://web.mit.edu/Saltzer/www/publications/endtoend/endtoend.pdf)
- [Time, Clocks, and the Ordering of Events](https://lamport.azurewebsites.net/pubs/time-clocks.pdf)
- [FLP Impossibility](https://groups.csail.mit.edu/tds/papers/Lynch/jacm85.pdf)
- [Leases](https://web.stanford.edu/class/cs240/readings/89-leases.pdf)

### 共识与一致性

- [Paxos Made Simple](https://lamport.azurewebsites.net/pubs/paxos-simple.pdf)
- [In Search of an Understandable Consensus Algorithm — Raft](https://raft.github.io/raft.pdf)
- [CAP Formalization](https://groups.csail.mit.edu/tds/papers/Gilbert/Brewer2.pdf)

### 分片、事务与恢复

- [Consistent Hashing](https://www.cs.princeton.edu/courses/archive/fall09/cos518/papers/chash.pdf)
- [Sagas](https://www.cs.cornell.edu/andru/cs711/2002fa/reading/sagas.pdf)
- [ARIES](https://dl.acm.org/doi/10.1145/128765.128770)
- [Life Beyond Distributed Transactions](https://queue.acm.org/detail.cfm?id=3025012)

## P1: 经典生产系统论文

- [Amazon Dynamo](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf)：Quorum、最终一致、版本冲突。
- [Google File System](https://research.google/pubs/the-google-file-system/)：复制、故障假设和大规模存储。
- [Bigtable](https://research.google/pubs/bigtable-a-distributed-storage-system-for-structured-data-by-chang-fay-et-al/)：Tablet、分片和结构化存储。
- [Spanner](https://research.google/pubs/pub39966/)：全球事务、TrueTime 与外部一致性。
- [ZooKeeper](https://www.usenix.org/legacy/event/atc10/tech/full_papers/Hunt.pdf)：协调原语和顺序保证。
- [The Tail at Scale](https://research.google/pubs/the-tail-at-scale/)：Fan-out、尾延迟和冗余请求。
- [Dapper](https://research.google/pubs/dapper-a-large-scale-distributed-systems-tracing-infrastructure/)：分布式追踪。
- [Maglev](https://research.google/pubs/maglev-a-fast-and-reliable-software-network-load-balancer/)：稳定映射与大规模负载均衡。

## P1: 可观测性、性能与混沌

- [OpenTelemetry Observability Primer](https://opentelemetry.io/docs/concepts/observability-primer/)
- [Prometheus Histograms and Summaries](https://prometheus.io/docs/practices/histograms/)
- [Envoy Circuit Breaking](https://www.envoyproxy.io/docs/envoy/latest/intro/arch_overview/upstream/circuit_breaking)
- [Principles of Chaos Engineering](https://principlesofchaos.org/)
- [Jepsen Analyses](https://jepsen.io/analyses)
- [Universal Scalability Law](http://www.perfdynamics.com/Manifesto/USLscalability.html)
- [Performance Modeling and Design of Computer Systems](https://www.cs.cmu.edu/~harchol/PerformanceModeling/)

## P2: 进阶书籍

- *Systems Performance* — Brendan Gregg：性能诊断、USE 方法与资源分析。
- *Release It!* — Michael Nygard：稳定性模式、隔舱、熔断和生产故障。
- *Database Internals* — Alex Petrov：存储引擎、B Tree、LSM、复制与分布式数据库。
- *The Art of Multiprocessor Programming*：线性化、并发对象和无锁算法。
- *The Datacenter as a Computer*：把数据中心理解为一台大规模计算机。
- [Patterns of Distributed Systems](https://martinfowler.com/articles/patterns-of-distributed-systems/)：已有基础后整理模式词汇。

## Stage Reading Route

| 阶段 | 必读 | 深化 |
| --- | --- | --- |
| Phase 0 测量 | The Tail at Scale、SRE Monitoring | Systems Performance、USL、排队论教材 |
| Phase 1 并发 | OSTEP Concurrency、CSAPP 并发 | Linux Locking、并发编程教材 |
| Phase 2 缓存 | CSAPP Memory Hierarchy、RFC 9111 | Facebook Memcache、Redis 官方文档 |
| Phase 3 I/O/背压 | OSTEP I/O、SRE Handling Overload | UNIX Network Programming、AWS 队列文章 |
| Phase 4 事务/恢复 | CMU 15-445、DDIA 事务 | ARIES、SQLite Atomic Commit |
| Phase 5 网络/重试 | DDIA 第 8 章、AWS Retry | End-to-End Arguments、Lamport Clock |
| Phase 6 复制/共识 | DDIA 5/6/9、MIT 6.5840、Raft | Dynamo、Spanner、CAP、Jepsen |
| Phase 7 高可用 | SRE Book/Workbook、Release It! | Builders' Library、Chaos Principles |

## Source Evaluation Checklist

- 作者或机构是否对该系统负有真实工程责任？
- 资料是否明确故障模型和适用范围？
- 声称的保证是组件级还是端到端？
- 是否给出反例、限制和退化路径？
- 是否能通过实验、官方文档或论文交叉验证？
- 是否把产品宣传词误当成严格语义？


# 第 10 章实验：JVM 内存、GC 与排障

## 当前状态

- 状态：实验 Pending
- 已完成：实验变量、故障矩阵、证据目录和断言设计
- 先修：第 09 章真实 ThreadPoolExecutor 拒绝路径与自定义 JFR 事件
- 未完成：GC 日志、heap/native/thread/lock、OOM、NMT、heap dump、虚拟线程和容器预算的真实运行

## 实验矩阵

| 实验 | 目标 | 证据 |
|---|---|---|
| 1. 分配与 GC | 比较短命对象、高分配率和存活对象 | GC log、allocation、P99 |
| 2. Humongous | 观察大对象对 G1 的影响 | heap/GC/JFR |
| 3. heap leak | 构造保留链并定位 owner | histogram、heap dump、脱敏保留链 |
| 4. Metaspace | 重复 classloader/动态类加载 | class count、NMT、Metaspace |
| 5. Direct Memory | NIO/Netty buffer 上限 | buffer pool、NMT、RSS |
| 6. Native thread | 线程/栈/容器 PID 限制 | thread count、RSS、错误 |
| 7. Lock | 死锁与锁竞争 | Thread Dump、JFR monitor events |
| 8. Virtual thread | pinning 与阻塞调用 | JFR、线程状态、吞吐 |
| 9. OOM Runbook | 分类、止血、证据保留 | 时间线、dump、恢复验证 |

## 通用证据目录

```text
evidence/<experiment>/
  environment.md
  jvm-flags.txt
  workload.md
  gc.log
  jfr-summary.txt
  thread-dump.txt
  nmt-summary.txt
  metrics.json
  correctness.md
  timeline.md
  conclusion.md
```

## 安全约束

- 只在隔离环境制造 OOM、死锁和线程耗尽。
- heap dump、NMT、JFR 文件可能含敏感数据，必须脱敏和限制权限。
- 不在真实用户数据上执行无限分配、全量 dump 或自动重启。
- 每个实验先写停止条件和清理步骤。

## 发布门槛

- 不能只展示 GC 曲线；必须关联 workload、请求 P99 和业务正确性。
- 不能把单 JVM 结果外推到集群容量。
- 不能把 JFR 自定义事件当作 GC/锁/分配证据。
- 每组实验有真实版本、原始输出和限制说明后，才可标记 Verified。

# 第 10 章资料与验证状态

## 一手资料

1. Java SE 21 JVM Specification, Run-Time Data Areas：
   <https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-2.html>
2. Java SE 21 Garbage Collection Tuning Guide：
   <https://docs.oracle.com/en/java/javase/21/gctuning/>
3. Java SE 21 JFR API：
   <https://docs.oracle.com/en/java/javase/21/docs/api/jdk.jfr/module-summary.html>
4. Java SE 21 JCMD 工具：
   <https://docs.oracle.com/en/java/javase/21/docs/specs/man/jcmd.html>
5. JEP 439: Generational ZGC：
   <https://openjdk.org/jeps/439>
6. JEP 444: Virtual Threads：
   <https://openjdk.org/jeps/444>
7. Java SE 21 Troubleshooting Guide：
   <https://docs.oracle.com/en/java/javase/21/troubleshoot/index.html>

## 来源使用规则

- GC 参数和收集器行为以 Oracle JDK 21 指南为准，不把博客经验写成保证。
- JFR 事件字段以 JDK 21 API/事件元数据为准。
- 虚拟线程 pinning、Direct Memory 和 NMT 必须注明 JDK、平台和运行参数。
- MAT 等第三方工具只作为分析工具，不把工具输出当作因果证明。

## 当前验证状态

| 项目 | 状态 | 证据 |
|---|---|---|
| JMM/JVM/HotSpot 边界 | 讲义核验 | JVM Spec 与 Oracle Guide |
| G1/Parallel/ZGC 选型 | 讲义核验 | GC Tuning Guide、JEP 439 |
| JFR API 与自定义事件 | 先修证据可用 | 第 09 章真实 ThreadPoolExecutor/JFR Phase A |
| GC 日志/分配/泄漏实验 | Pending | 尚无本章运行输出 |
| heap dump/NMT/Direct Memory | Pending | 尚无本章运行输出 |
| 锁竞争/虚拟线程 pinning | Pending | 尚无本章运行输出 |

本章不能标记为 Lab Verified、Release Candidate 或 Released。

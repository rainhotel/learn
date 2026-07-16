# 第 10 章面试追问

## JVM 与 JMM 有什么关系？

JMM 是并发语义，不是堆栈布局。JVM 运行时数据区和 HotSpot 内存只是执行实现与资源问题。

## G1 为什么不是“低延迟保证”？

G1 通过 Region、并发标记和可预测暂停目标进行权衡，但暂停目标受堆大小、存活对象、分配速率、Humongous Object、CPU 和外部阻塞影响，不是 SLA 保证。

## 如何判断内存泄漏？

要观察随时间增长的存活集合，比较 class histogram/heap dump 的保留链，排除缓存、队列、重试和正常堆扩张。一次 OOM 或一次 heap 高峰不足以证明泄漏。

## heap 不高但 RSS 高怎么办？

检查线程栈、Direct Buffer、Metaspace、Code Cache、JNI/native 分配和容器限制；使用 NMT、buffer pool、线程数和操作系统 RSS 交叉验证。

## 线程 Dump 全是 WAITING 是故障吗？

WAITING 可能是正常条件队列、线程池空闲或 latch 等待；死锁需要锁依赖环、持有者和时间线证据。

## 虚拟线程 pinning 怎么排查？

先用 JFR/线程状态确认 pinning 位置，再检查长时间 `synchronized`、native 调用和阻塞 I/O；用小范围实验和降级策略验证，不能只因为用了虚拟线程就宣称吞吐提升。

## Agent 能自动做 JVM 排障吗？

可以整理证据、聚类症状、生成假设和只读查询；不能自主 dump、重启、改参数或清理生产数据。所有高风险动作必须经过确定性控制面和审批。

## 项目追问

- 你的 NotifyFlow 内存预算如何计算？
- 重试和 DLT 如何避免 payload 乘法保留？
- 你如何证明 P99 上升是队列而不是 GC？
- 为什么第 09 章的自定义 JFR 事件不能证明 GC 根因？
- 你做过哪些真实实验，哪些仍是 Pending？

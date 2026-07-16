# 来源、版本与验证记录

## A 级：语言规范

- [JLS 21, Chapter 17: Threads and Locks](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html)
  - 17.1：monitor synchronization 与可重入。
  - 17.2：wait set、通知、中断与虚假唤醒。
  - 17.3：sleep/yield 无同步语义。
  - 17.4：memory model、actions、data race、synchronization/happens-before。
  - 17.5：final 字段语义。
- 访问日期：2026-07-13。

## A 级：Java 21 API

- [Thread, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html)
- [Object.wait/notify, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html)
- [AtomicInteger](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/AtomicInteger.html)
- [LongAdder](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/LongAdder.html)

## B 级

- Brian Goetz 等，《Java 并发编程实战》：发布、共享、锁与取消。
- Jeremy Manson、William Pugh、Sarita Adve，Java Memory Model 相关设计资料。

涉及合法执行的结论以 JLS 21 为准。

## C 级

- Java 后端公开面经中 volatile、synchronized、JMM、锁升级、ThreadLocal 和线程池为高频连续追问。
- 面经仅用于教学重点，不定义语言语义。

## 实验

- 文件：`lab/JmmLab.java`。
- 环境：Oracle JDK 21.0.6，Windows。
- 验证日期：2026-07-13。

已验证：

1. volatile 发布使 reader 观察到此前 payload=42。
2. Barrier 固定读写交错，两个线程各执行 5,000 次 volatile read-modify-write，结果确定为 5,000 而不是 10,000。
3. synchronized 递增结果为 200,000。
4. monitor unlock/后续 lock 发布普通字段 99。
5. monitor 可重入，异常退出后 follower 可以获取锁。

### 实验边界

- 实验 1 证明给定同步协议，不证明所有无 volatile 程序一定失败。
- 实验 2 的 Barrier 有自身同步语义，用于确定性制造丢失更新交错。
- 实验不用于推断具体 CPU 缓存刷新指令。
- synchronized 的 HotSpot 锁优化、对象头和汇编属于实现层，需要独立章节和版本核验。


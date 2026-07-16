# Java 21 JVM 内存、GC 与生产排障

## 课程信息

- 所属模块：生产与平台工程
- 难度：深入
- 建议时长：24-32 小时
- 先修：JMM、线程池、可观测性、压测与 JFR Phase A
- 项目里程碑：NotifyFlow 从“能采集 JVM 事件”升级到“能建立 JVM 故障证据链”
- 实验状态：Pending；本讲义不声称 GC、OOM、锁或 NMT 实验已经通过

## 学习目标

完成本章后，学习者能够：

1. 区分语言并发语义、JVM 规范运行时数据区和 HotSpot 实现。
2. 解释对象分配、可达性、回收、晋升与类卸载。
3. 根据吞吐、延迟、占用和环境约束比较 GC。
4. 从用户症状出发选择低风险诊断工具。
5. 识别 heap、Metaspace、direct/native memory、线程与锁故障。
6. 为 NotifyFlow 和 RAG/Agent 工作负载建立有界内存设计。
7. 产出可复核、可脱敏、不过度外推的排障报告。

## 一、先纠正三个常见混淆

### 1.1 JMM 不是 JVM 内存分区图

Java Memory Model 定义线程之间何时可见、哪些重排序允许、何时存在数据竞争，以及 `volatile`、锁、线程启动与结束形成哪些 happens-before 关系。

堆、栈、方法区属于运行时数据区域问题。用“线程把工作内存刷新到主内存”画成真实 CPU/JVM 内存复制流程，会把抽象语义误当实现。

### 1.2 JVM 规范不等于 HotSpot 实现

JVM 规范定义 PC 寄存器、JVM 栈、堆、方法区、运行时常量池和本地方法栈等抽象区域。HotSpot 以 Metaspace、Code Cache、GC Heap、线程栈等具体结构实现其中一部分，并可随版本变化。

面试时应先说概念层级：

```text
JLS/JMM：并发语义
JVMS：运行时数据区抽象
HotSpot：Oracle/OpenJDK 常见实现
OS/Container：进程地址空间与资源限制
```

### 1.3 “对象在堆、引用在栈”只是入门近似

- 局部变量和部分引用通常位于栈帧，但字段中的引用随对象位于堆中。
- 静态字段、类镜像、JIT 优化后的值和 JNI 引用不能用一句话概括。
- 逃逸分析可能消除分配或进行标量替换，这是优化，不是 Java 语义保证。
- 排障应看真实分配、保留和 native memory 证据，不靠示意图猜测。

## 二、运行时内存地图

### 2.1 线程私有区域

每个平台线程通常关联：

- PC 寄存器：当前线程执行位置的抽象。
- JVM 栈：由栈帧组成，保存局部变量、操作数栈、动态链接和返回信息。
- 本地方法调用相关状态。
- HotSpot/OS 分配的 native thread stack。

递归过深或单帧过大可能产生 `StackOverflowError`。线程数量过多则可能耗尽地址空间、提交内存或 OS 线程配额，出现 `OutOfMemoryError: unable to create native thread`。

### 2.2 线程共享区域

- Java Heap：普通对象和数组的主要 GC 管理区域。
- 方法区：JVM 规范概念，保存类级结构、运行时常量池等。
- HotSpot Metaspace：主要使用 native memory 保存类元数据。
- Code Cache：JIT 编译后的机器码等。
- Direct Buffer、JNI、GC 内部结构和其他 native allocations：不受 `-Xmx` 直接约束。

### 2.3 进程内存不等于 heap

近似关系：

```text
Process RSS / committed memory
≈ Java Heap
+ Metaspace / Compressed Class Space
+ Code Cache
+ platform thread stacks
+ Direct Buffers / mapped memory
+ GC and JVM native structures
+ native libraries / JNI
+ allocator fragmentation and shared pages
```

因此容器限制为 4 GiB 时，不能把 `-Xmx` 也设为 4 GiB 后期待稳定运行。必须为 heap 外内存、诊断、峰值和系统组件留余量。

## 三、对象为什么活着

### 3.1 GC Roots 与可达性

GC 通常从一组 Roots 出发遍历引用图。常见 Roots 包括活动线程栈中的引用、类相关引用、JNI 引用和 JVM 内部引用。

对象“业务上已经没用”但仍可从 Root 到达，就不会被回收。这正是多数 Java heap leak 的本质：不是 GC 失效，而是引用生命周期错误。

### 3.2 四类引用

- 强引用：正常可达时不会回收。
- 软引用：受内存压力与实现策略影响，不适合作为严格容量缓存策略。
- 弱引用：在只剩弱可达时可被清理，常用于不拥有对象生命周期的映射。
- 虚引用：配合引用队列跟踪回收后处理，不能取得 referent。

无论使用哪类引用，都必须设计容量、过期、并发和清理路径。`WeakHashMap` 不是通用防泄漏开关。

### 3.3 常见保留链

```text
GC Root
-> singleton / static collection
-> tenant cache
-> task context
-> payload byte[]
```

```text
worker thread
-> ThreadLocalMap
-> trace / tenant context
-> large tool output
```

```text
plugin manager
-> old ClassLoader
-> loaded classes / static fields
-> application graph
```

Heap dump 分析的核心不是“哪种对象最多”，而是 dominator、retained size 和 path to GC roots。

## 四、分配、TLAB 与晋升

多数短命对象被分配在年轻代逻辑区域。HotSpot 可为线程提供 TLAB，减少常见分配路径的竞争；对象过大、TLAB 不适合或配置变化时会走其他路径。

需要区分：

- allocation rate：每秒新分配多少字节。
- live set：一次完整标记后仍存活的对象集合。
- retained size：某对象被移除后可释放的总对象图大小。
- promotion：对象从年轻代逻辑区域进入老年代。

高 allocation rate 可能导致频繁 Young GC，但不等于内存泄漏。若每次回收后基线持续上升，才更像 live set 增长或泄漏。

不要依赖 `finalize()` 释放外部资源。Finalization 已被弃用以便移除；文件、连接等应使用 `AutoCloseable` 与 try-with-resources，`Cleaner` 只能作为谨慎设计的后备机制。

## 五、GC 必须围绕目标选择

### 5.1 三个互相牵制的目标

- Throughput：应用线程占总时间比例。
- Latency：停顿和请求长尾。
- Footprint：heap 与 native memory 占用。

不存在对所有工作负载都最优的 GC。选择前必须固定业务 SLO、heap、CPU、容器限制、对象分配和 live set。

### 5.2 基本算法词汇

- Mark：识别存活对象。
- Sweep：回收未标记空间。
- Compact/Relocate：移动存活对象，减少碎片。
- Copy：把存活对象复制到目标区域。
- Write/Load Barrier：在引用读写时维护并发 GC 所需信息。
- Remembered Set/Card Table：跟踪跨区域/跨代引用，避免每次扫描整个 heap。

并发 GC 仍可能有 Stop-The-World 阶段；“并发”不等于“零停顿”。

## 六、G1：Java 21 常见基线

G1 把 heap 划分为等大小 Region，按运行状态承担 Eden、Survivor、Old 或 Humongous 等逻辑角色。它以可预测停顿为目标，并通过选择 Region 回收收益进行调度。

### 6.1 关键阶段

- Young GC：回收年轻代 Region，通常 Stop-The-World。
- Concurrent Marking：并发识别老年代 live set，包含短暂 STW 阶段。
- Mixed GC：在 Young Collection 中同时选择部分老年代 Region。
- Full GC：并发回收跟不上、分配失败或其他退化路径下的高风险信号。

`-XX:MaxGCPauseMillis` 是软目标，不是 SLA 保证。为了追求目标，G1 可能调整年轻代大小和回收集合；CPU、live set、引用处理、Humongous 分配和容器抖动仍会影响停顿。

### 6.2 Humongous Object

大于或等于约半个 G1 Region 的对象会走 Humongous 分配路径，占用连续 Region。大 JSON、批量收件人数组、PDF/CSV `byte[]`、Agent 超长上下文和工具大输出都可能触发。

不要只增大 Region 或 heap 掩盖问题。优先检查：

- 是否应流式处理。
- 是否可把大 payload 放对象存储，只在任务中保存引用。
- 是否可分页、分块或限制 Agent tool output。
- 是否存在大对象重复复制和序列化。

## 七、其他收集器与选择边界

### Parallel GC

适合吞吐优先、可接受较长 STW 停顿的批处理或离线任务。不能因“并行”就认为请求延迟更低。

### Serial GC

实现简单、占用较小，适合很小 heap、单核或工具型进程；不适合默认套用到高并发服务。

### ZGC

以低停顿和大 heap 可扩展性为目标，大量工作与应用并发执行。Java 21 提供 Generational ZGC，可通过 `-XX:+UseZGC -XX:+ZGenerational` 选择；实际可用性和行为必须以所用 JDK 发行版与 `java -XX:+PrintFlagsFinal -version` 为准。

低停顿不等于无成本。应在相同负载、heap、CPU 和正确性条件下比较吞吐、P99、内存占用和恢复行为。

### 选择流程

1. 默认从发行版推荐的服务端收集器和合理 heap 开始。
2. 先消除无界保留、分配风暴和下游阻塞。
3. 固定 workload，保存基线。
4. 只改变一个关键变量。
5. 用业务 P99、错误率、吞吐、GC 和 footprint 共同判断。

## 八、如何读 GC 证据

建议使用 Unified Logging：

```text
-Xlog:gc*,safepoint:file=logs/gc-%t.log:time,uptime,level,tags:filecount=10,filesize=20M
```

目录、权限、磁盘轮转和容器挂载必须提前验证。关注：

- GC 原因和类型。
- pause duration 与并发 cycle 时长。
- GC 前后 heap/region 占用。
- allocation/promotion 速度。
- old occupancy 和回收后基线。
- Humongous Region。
- Full GC、to-space exhausted、evacuation failure 等退化信号。
- safepoint 总停顿与到达 safepoint 的时间。

### 四种常见模式

1. 高频 Young GC，回收后低：更像分配率高或年轻代偏小。
2. 回收后基线不断上升：调查 live set、缓存、ThreadLocal 或泄漏。
3. Old 高位、Mixed 回收有限：检查老年代存活、Humongous、并发周期是否来不及。
4. GC 正常但 P99 高：继续看锁、线程、连接池、网络、数据库和供应商。

不要把相关性自动当因果。必须按时间轴对齐请求长尾、GC、CPU、线程和下游信号。

## 九、OOM 不是一种故障

### `Java heap space`

可能是 heap 太小、单次大分配、无界缓存、请求保留或真正泄漏。证据：GC 日志、回收后基线、class histogram、heap dump。

### `GC overhead limit exceeded`

表示大量时间用于 GC 而回收收益很低，是症状而非根因。关闭该保护不等于修复。

### `Metaspace`

检查动态生成类、代理、脚本/表达式引擎、热部署和 ClassLoader 无法卸载。只增加 `MaxMetaspaceSize` 可能延后故障。

### `Direct buffer memory`

heap 可能正常，但 NIO/Netty/客户端 direct buffer 达到限制或清理跟不上。检查 buffer pool 指标、NMT、客户端生命周期和进程 RSS。

### `unable to create native thread`

检查平台线程总数、线程栈大小、OS/容器进程限制、native memory、失控线程池和每请求创建线程。

### `Requested array size exceeds VM limit`

通常是计算溢出、非法长度或一次性超大数组，不应简单增大 heap。

`StackOverflowError` 与上述 OOM 不同，常见于无限递归或深调用链。

## 十、heap leak 排查路径

1. 从用户症状确认影响：OOM、暂停、容器重启还是只是 heap 高。
2. 保存时间线、JVM 参数、容器限制和 GC 日志。
3. 看回收后基线是否跨周期增长。
4. 对比多个时间点的 class histogram。
5. 在磁盘、停顿、敏感数据风险允许时生成 heap dump。
6. 查看 dominator、retained size 和 GC root 路径。
7. 回到代码确认生命周期 owner、容量与清理路径。
8. 在可重复负载下验证修复，而不是只看进程重启后 heap 变低。

Heap dump 可能包含手机号、邮件、token、prompt、消息正文和密钥。不得默认上传公共分析服务，也不得让 Agent 读取未脱敏原始 dump。

## 十一、heap 不高但 RSS 高

优先调查：

- Metaspace 与 classloader 数量。
- Code Cache。
- Direct/mapped buffers。
- 平台线程数量与 stack reservation/commit。
- GC/JVM native structures。
- JNI/native library 分配。
- allocator fragmentation。

Native Memory Tracking 需要启动时开启：

```text
-XX:NativeMemoryTracking=summary
```

然后使用：

```powershell
jcmd <pid> VM.native_memory baseline
jcmd <pid> VM.native_memory summary.diff
```

NMT 不能追踪所有第三方 native 分配，并有开销；`summary` 与 `detail` 也不同。必须先在目标版本验证。

## 十二、线程、锁与“CPU 不高但很慢”

### 12.1 线程状态不要机械翻译

- `RUNNABLE` 可能正在 CPU 执行，也可能停留在 native 调用；需要 CPU/JFR/栈证据。
- `BLOCKED` 指等待进入 `synchronized` monitor。
- `WAITING/TIMED_WAITING` 可能来自 `park`、`wait`、`join`、sleep 或并发工具。

一次 Thread Dump 只是快照。应在受控间隔获取多份，寻找长期不变栈、共同锁 owner、线程数量增长和池命名模式。

### 12.2 死锁与锁竞争

死锁是循环等待；锁竞争则可能仍有进展但吞吐和 P99 恶化。工具链：

```powershell
jcmd <pid> Thread.print -l
```

结合 JFR monitor enter、thread park、执行采样和业务 Trace。不要看到 `synchronized` 就立即替换为无锁结构；先确认临界区、持锁时间和争用频率。

### 12.3 Java 21 虚拟线程

虚拟线程适合大量阻塞式并发任务，但不会提高 CPU 密集计算吞吐，也不会扩大数据库连接或供应商配额。

Java 21 中，虚拟线程在某些 `synchronized` 或 native/foreign 调用中可能被 pin 到 carrier。应通过 JFR `jdk.VirtualThreadPinned`、线程 dump 和请求时间线确认。

设计原则：

- 不用固定线程池限制虚拟线程数量；用 semaphore/限流器保护稀缺下游。
- 谨慎使用 ThreadLocal，海量虚拟线程会放大每线程状态成本。
- 保留 deadline、取消、超时和结构化资源关闭。
- 平台线程池与虚拟线程都不能替代背压。

## 十三、JFR、JCMD 与诊断层级

### 低风险起点

```powershell
jcmd <pid> VM.version
jcmd <pid> VM.flags
jcmd <pid> GC.heap_info
jcmd <pid> Thread.print -l
jcmd <pid> JFR.check
```

命令影响因 JVM、heap、线程和事件配置而异，执行前应查看：

```powershell
jcmd <pid> help <command>
```

### JFR

```powershell
jcmd <pid> JFR.start name=notifyflow settings=default maxage=30m maxsize=512m
jcmd <pid> JFR.dump name=notifyflow filename=notifyflow-incident.jfr
jfr summary notifyflow-incident.jfr
```

`default` 与 `profile` 的事件和开销不同。生产录制要设置保留、磁盘、访问权限和脱敏规则。

第 09 章 Phase A 已验证：JDK 21 可从真实有界线程池任务写入并解析 4 条自定义 `com.notifyflow.ProviderCall` 事件。该证据没有启用 GC、Allocation、ThreadPark、JavaMonitor 或 ExecutionSample，不能扩展为本章实验结论。

### Class Histogram 与 Heap Dump

`GC.class_histogram` 和 heap dump 可能触发明显停顿、CPU 或磁盘压力。先检查命令帮助、heap 大小、可用磁盘、复制路径、权限和数据合规，再由事故负责人批准。

## 十四、生产排障不是工具清单

推荐流程：

```text
用户症状与影响
-> 时间线和最近变更
-> 保护用户/缩小爆炸半径
-> 提出可证伪假设
-> 选择最低风险证据
-> 关联业务、JVM 与依赖信号
-> 可逆缓解
-> 验证正确性和恢复
-> 根因、行动项与实验复现
```

### 事故前 15 分钟

1. 确认 P99、错误率、实例重启、积压和受影响租户。
2. 冻结高风险发布与自动恢复动作。
3. 保存实例、版本、JVM 参数、容器限制和时间窗口。
4. 若用户安全要求重启，先尽可能保存低风险证据；不能为取证牺牲服务恢复。
5. 比较健康与异常实例，而不是只分析单点。

### 禁止的条件反射

- 看到 heap 高就执行 `System.gc()`。
- 看到 OOM 就只增大 `-Xmx`。
- 看到 P99 高就切换 GC。
- 未检查磁盘就生成 heap dump。
- 未确认 owner 就清空缓存、队列或重放任务。
- 用一次 JFR/Thread Dump 宣称根因已证明。

## 十五、NotifyFlow 的真实风险

### 大 payload 保留

批量 CSV、模板渲染结果和附件被放进 `TaskContext`，又被队列、重试和 Future 同时引用，形成多份大 `byte[]`。改造方向是对象存储引用、流式解析、有界批次和明确生命周期。

### 无界 Map/Future

`ConcurrentHashMap<taskId, CompletableFuture<?>>` 在完成、超时和异常路径未统一移除。需要 owner、上限、deadline、finally 清理和指标。

### ThreadLocal 泄漏

租户、trace、prompt 或工具输出进入平台线程池 ThreadLocal，任务结束未 `remove()`。应使用结构化 context 传递并在 `finally` 清理。

### RAG/Agent 上下文膨胀

检索 topK、chunk 大小、历史轮数、tool output 和 agent step 无上限。内存治理必须与 token、时间、工具次数和外部副作用预算统一。

### 锁与 pinning

Provider 限流器在 `synchronized` 中执行网络调用，导致平台线程竞争；迁移虚拟线程后又产生 carrier pinning。应缩短临界区，把 I/O 移出锁，并用 semaphore/限流器表示供应商并发预算。

### ClassLoader/Metaspace

插件、脚本或动态代理热加载后旧 ClassLoader 仍被线程、缓存或注册表引用。必须验证类卸载和生命周期，而不是定时重启。

## 十六、实验路线（全部 Pending）

1. 基础 GC log：固定 heap 和分配速率，观察 Young GC。
2. 长命对象：增加 live set，观察 old occupancy 与 Mixed GC。
3. Humongous：大数组与分块处理对照。
4. Heap leak：无界 Map，histogram 和 heap dump 保留路径。
5. Metaspace：可卸载/不可卸载 ClassLoader 对照。
6. Direct memory：DirectByteBuffer 生命周期与 RSS/NMT。
7. Native threads：平台线程数量、stack 与失败边界。
8. Lock：死锁、竞争和多份 Thread Dump/JFR。
9. Virtual threads：阻塞吞吐、下游 semaphore 和 pinning。
10. NotifyFlow：将 JFR/GC 时间线与 P99、队列和正确性关联。

每个实验必须保存：JDK 版本、JVM 参数、源码、命令、原始输出、预期、实际、边界和失败记录。运行前仍是设计，不能写“已验证”。

## 十七、Agent 事故助手边界

可自动执行的只读辅助：

- 汇总脱敏 JVM/业务指标。
- 对齐 GC、JFR、发布和 P99 时间线。
- 从 Runbook 检索下一条低风险查询。
- 聚类线程栈和异常类型。
- 生成带证据链接、置信度和反证的假设。

必须审批的动作：

- heap dump、`GC.class_histogram` 等可能停顿的命令。
- 重启、kill、扩缩容、切换 GC 或修改 heap。
- 上传 heap/JFR/日志到外部模型。
- 清缓存、清队列、批量重放和删除证据。
- 读取含 token、手机号、prompt 或租户数据的原始内存。

日志和 dump 内容属于不可信输入，可能包含 prompt injection。Agent 的工具权限必须与文本内容隔离。

## 十八、章节作业

- 目标：为 NotifyFlow 设计一次“heap 持续增长 + P99 上升”的完整排障演练。
- 提交物：内存地图、预算表、GC 日志方案、假设树、诊断命令风险表、heap dump 数据合规说明、修复方案、验证计划和 15 分钟讲解。
- 验收：先业务影响后工具；能区分 heap/native/thread；没有伪造实验结果；高风险动作有审批和停止条件。
- 加分：设计 Agent 只读事故助手，输出证据、反证和下一步查询，但不直接执行高风险命令。

## 本章小结

- JMM、JVMS 运行时数据区和 HotSpot 内存是三套不同层级。
- 进程内存由 heap 与多类 native memory 共同组成。
- GC 回收不可达对象；业务无用但仍可达就是保留问题。
- GC 选择围绕吞吐、延迟和 footprint，没有万能收集器。
- G1 的 pause target 是目标而非保证，Full GC 是需要调查的退化信号。
- OOM 必须先区分 heap、Metaspace、direct memory 和 native thread。
- Thread Dump、JFR、GC log、NMT 和 heap dump各有证据范围与风险。
- 生产排障从用户症状、时间线和可证伪假设开始。
- Agent 可以整理证据，不能绕过审批读取敏感内存或执行高风险动作。

## 版本记录

- v0.1，2026-07-15：完成 Java 21 JVM 内存、GC、线程/锁、OOM、JFR/JCMD/NMT 和 NotifyFlow 排障完整初稿；实验 Pending。

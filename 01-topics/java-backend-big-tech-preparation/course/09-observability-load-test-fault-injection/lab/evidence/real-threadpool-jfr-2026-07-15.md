# 真实 ThreadPoolExecutor 与 JFR 实验记录

## 环境

- 日期：2026-07-15
- Java/JFR：Oracle JDK 21.0.6
- 外部依赖：无
- 线程池：2 个固定 worker
- 队列：`ArrayBlockingQueue`，容量 2
- 拒绝策略：`AbortPolicy`
- JFR 事件：`com.notifyflow.ProviderCall`

## 验证目标

1. 使用真实 `ThreadPoolExecutor`，不是离散事件模型。
2. 用 `CountDownLatch` 固定两个运行中任务和两个排队任务，稳定制造拒绝。
3. 每个被接受的 Provider 任务提交一条低基数字段的自定义 JFR 事件。
4. 使用 `RecordingFile`、`jfr summary` 和 `jfr print` 三种方式读取录制结果。

## TDD RED

先创建 `RealThreadPoolJfrTest.java`，测试引用尚不存在的 `ThreadPoolJfrExperiment`。

编译结果：退出码 1，得到 2 个“找不到符号/程序包不存在”错误，符合预期 RED。

## 第一次运行环境失败

实现完成后的第一次运行在创建 `Recording` 时失败：

```text
IllegalStateException: Can't create Flight Recorder
AccessDeniedException: C:\Users\RAINHO~1\AppData\Local\Temp
```

失败发生在实验执行前，原因是 JFR 默认临时仓库不可访问，不是业务断言失败。

修正方式：运行 Java 时显式设置：

```powershell
-Djava.io.tmpdir=D:\moniC\project\learn\tmp\jfr-runtime-20260715
```

没有修改全局 JVM 或系统配置。

## GREEN

重新编译并运行测试：

```text
ALL_OBSERVABILITY_EXPERIMENT_TESTS_PASSED
ALL_REAL_THREAD_POOL_JFR_TESTS_PASSED
```

实验真实输出：

```text
java_version=21.0.6
workers=2
queue_capacity=2
submitted=6
accepted=4
rejected=2
completed=4
max_queue_depth=2
jfr_event_type=com.notifyflow.ProviderCall
jfr_event_count=4
jfr_success_events=4
jfr_min_duration_nanos=1100
jfr_max_duration_nanos=827400
jfr_average_duration_nanos=411300
recording_path=D:\moniC\project\learn\tmp\notifyflow-threadpool-jfr-evidence-20260715.jfr
recording_bytes=113422
REAL_THREAD_POOL_JFR_EXPERIMENT_PASSED
```

耗时数据受机器调度影响，复跑时允许变化；提交、接受、拒绝、完成和事件数量是确定性断言。

## JFR 独立检查

`jfr summary` 结果包含：

```text
Event Type                       Count  Size (bytes)
com.notifyflow.ProviderCall          4           156
```

`jfr print --events com.notifyflow.ProviderCall` 展示了 4 条事件，每条均包含：

- `taskType="provider-call"`
- `accepted=true`
- `queueDepth` 在 0-2 之间
- 非负 `durationNanos`
- `outcome="SUCCESS"`

本录制只显式启用了自定义事件，`jfr summary` 中 GC、Allocation、ThreadPark、JavaMonitor 和 ExecutionSample 等 JDK 诊断事件均为 0，因此它不能作为 JVM GC、锁、分配或采样分析证据。

录制文件：

```text
path=tmp/notifyflow-threadpool-jfr-evidence-20260715.jfr
bytes=113422
sha256=0096AD61E09E29E94258FB6A72556529750A81324640F9FD08FCB884205F37BC
```

该二进制位于仓库临时目录，可通过实验命令重新生成；课程长期证据以本记录、源码和可重复命令为准。

## 运行命令

```powershell
$build='tmp\observability-real-jfr-build'
$jfrTemp=(Resolve-Path 'tmp').Path+'\jfr-runtime'
New-Item -ItemType Directory -Force -Path $build,$jfrTemp | Out-Null
$sourceRoot='01-topics\java-backend-big-tech-preparation\course\09-observability-load-test-fault-injection\lab\src'
$sources=Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d $build $sources
java "-Djava.io.tmpdir=$jfrTemp" -cp $build com.notifyflow.observability.RealThreadPoolJfrTest
java "-Djava.io.tmpdir=$jfrTemp" -cp $build com.notifyflow.observability.ThreadPoolJfrExperiment `
  'tmp\notifyflow-threadpool-jfr-evidence.jfr'
jfr summary 'tmp\notifyflow-threadpool-jfr-evidence.jfr'
jfr print --events com.notifyflow.ProviderCall 'tmp\notifyflow-threadpool-jfr-evidence.jfr'
```

## 结论

- 在本实验的闩锁场景，瞬时可容纳任务数为 `workers + queueCapacity=4`；第 5、6 次提交被真实 `AbortPolicy` 拒绝。吞吐容量仍由服务时间、调度和下游资源决定，不能由该式外推。
- 4 个被接受任务全部完成，并各生成 1 条自定义 JFR 事件。
- JFR 可以携带低基数业务字段，把 JVM 时间线与 Provider 调用证据关联起来。
- `taskId/traceId` 没有进入高频事件字段；需要定位单请求时应通过受控日志或 Trace 关联。

## 边界

- 这是单 JVM 的确定性拒绝路径实验，不是吞吐压测，不能外推出生产容量。
- 本实验只验证自定义 JFR 事件采集链路，没有验证 GC pause、对象分配、锁竞争或采样栈。
- 没有测量 JFR 在高事件频率、长时间录制和磁盘压力下的开销与数据丢失。
- JFR 不能替代业务 SLI、聚合 Metrics、结构化日志或跨服务 Trace。
- 第 09 章实验 9 只完成 Phase A，仍不能标记整章为 Lab Verified。

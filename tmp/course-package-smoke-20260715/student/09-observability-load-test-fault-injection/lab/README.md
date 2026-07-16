# 第 9 章实验：指标、负载模型与故障演练

## 当前状态

- 状态：实验 1-4 基础验证完成；实验 9 Phase A 自定义 JFR 事件已验证；实验 5-8、9 Phase B 和 10 Pending
- 已完成：长尾统计、负载模型、指标基数、线程池容量模型、真实有界 `ThreadPoolExecutor` 拒绝路径和自定义 JFR 事件的 TDD/运行证据
- 已准备但未验证：Spring Boot 4.1.0/Micrometer 1.17.0 最小工程
- 已准备但未运行：k6 开放/封闭/分阶段恢复脚本
- 未完成：真实 Micrometer Maven 运行、k6 runtime/threshold、JFR GC/分配/锁分析、Dashboard、基础设施故障注入和运行证据

真实证据：`evidence/java-experiments-2026-07-15.md`、`evidence/real-threadpool-jfr-2026-07-15.md`。

k6 静态证据：`evidence/k6-static-2026-07-15.md`；本机未安装 k6，不能填写真实压测结果。

## 源码

```text
src/main/java/com/notifyflow/observability/
  LatencyStatistics.java
  LongTailStatisticsExperiment.java
  LoadModelSimulator.java
  OpenClosedLoadExperiment.java
  MetricCardinalitySimulator.java
  MetricCardinalityExperiment.java
  ThreadPoolSaturationSimulator.java
  ThreadPoolSaturationExperiment.java
  ProviderCallJfrEvent.java
  ThreadPoolJfrExperiment.java

src/test/java/com/notifyflow/observability/
  ObservabilityExperimentsTest.java
  RealThreadPoolJfrTest.java
```

运行时只需要 Java 21，不需要 Maven 和第三方依赖。编译产物输出到仓库 `tmp/`，不污染课程源码目录。

## 实验 1：平均值掩盖长尾

状态：JDK 21 确定性统计模型 Verified。

构造 9900 个 10 ms 请求、99 个 500 ms 请求和 1 个 10 s 请求。

输出：average、P50、P95、P99、P99.9、max。解释不同统计量看到的用户体验。

实际结果：平均值 15.85 ms，P99 10 ms，P99.9 500 ms，最大值 10000 ms。最大值仍是平均值的 630.91 倍。

## 实验 2：开放与封闭负载

状态：JDK 21 确定性离散事件模型 Verified；真实 k6 运行 Pending。

### 场景

目标系统在 30 秒后从 100 ms 变慢到 2 秒。

### 对比

- 固定 100 VU 的封闭模型。
- 500 iteration/s 的开放模型。

### 验收

- 封闭模型实际 arrival rate 随延迟下降。
- 开放模型继续尝试维持 500/s，并产生更真实的排队、dropped iteration 或错误。
- 报告 coordinated omission 对结论的影响。

实际结果：系统变慢后，封闭模型到达率从 1000/s 降至 50/s；开放模型仍调度 500/s，在最大并发 300 的约束下，慢阶段接受 4500 次、丢弃 10500 次。封闭模型 P95 仍为 100 ms，开放模型 P95 为 2000 ms。

k6 实现：`k6/open-load.js` 与 `k6/closed-load.js` 已准备；真实 k6 运行、threshold 退出码和服务端证据仍 Pending。

## 实验 3：指标 tag 基数

状态：JDK 21 唯一时序计数模型 Verified；真实 Micrometer 与 `MeterFilter` Pending。

对比：

- `provider/result` 两个低基数 tag。
- `taskId` 高基数 tag。

第一阶段记录唯一时序数，并用显式的每时序 512 字节假设估算元数据量；第二阶段再记录真实 Micrometer meter 数、heap 和导出数据量，并验证 `MeterFilter` 拒绝危险 tag。

实际结果：`provider/result` 产生 6 条时序；加入唯一 `taskId` 后产生 10000 条，放大 1666.67 倍。512 字节/时序得到 4.8828 MiB 仅为模型估算，不是实测 heap。

## 运行命令

从仓库根目录执行：

```powershell
$build='tmp\observability-lab-build-green-20260715'
New-Item -ItemType Directory -Force -Path $build | Out-Null
$sourceRoot='01-topics\java-backend-big-tech-preparation\course\09-observability-load-test-fault-injection\lab\src'
$sources=Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d $build $sources
java -cp $build com.notifyflow.observability.ObservabilityExperimentsTest
java -cp $build com.notifyflow.observability.LongTailStatisticsExperiment
java -cp $build com.notifyflow.observability.OpenClosedLoadExperiment
java -cp $build com.notifyflow.observability.MetricCardinalityExperiment
java -cp $build com.notifyflow.observability.ThreadPoolSaturationExperiment
```

JFR 运行还需要把 JVM 临时目录指向仓库可写目录：

```powershell
$jfrTemp=(Resolve-Path 'tmp').Path+'\jfr-runtime'
New-Item -ItemType Directory -Force -Path $jfrTemp | Out-Null
java "-Djava.io.tmpdir=$jfrTemp" -cp $build com.notifyflow.observability.RealThreadPoolJfrTest
java "-Djava.io.tmpdir=$jfrTemp" -cp $build com.notifyflow.observability.ThreadPoolJfrExperiment `
  'tmp\notifyflow-threadpool-jfr-evidence.jfr'
```

## 实验 4：线程池饱和

状态：容量拐点模型与真实有界 `ThreadPoolExecutor` 拒绝路径均 Verified；真实到达率压测和 Micrometer 联动 Pending。

- 有界线程池执行慢 Provider 调用。
- 逐级提高开放到达率。
- 记录 active、queue、rejected、P99 和 completed throughput。
- 找到队列开始持续增长的拐点。

固定 20 个 worker、100 容量队列和 100 ms 服务时间时，模型容量约为 200/s。实际结果：

- 100/s：无排队、无拒绝、P99 100 ms。
- 200/s：无拒绝，完成吞吐 199.68/s。
- 250/s：队列打满 100，拒绝 2900 次，P99 600 ms，完成吞吐仍为 199.68/s。
- 400/s：拒绝 11900 次，吞吐不再增长。

这证明“队列增长和 P99 上升先于吞吐增长停止”可以作为容量拐点证据。另一个真实 `ThreadPoolExecutor` 实验通过闩锁稳定占满 2 个 worker 和 2 个队列槽，第 5、6 次提交被 `AbortPolicy` 拒绝；它验证拒绝路径，不外推生产吞吐。

## 实验 5：数据库慢与连接池

- 注入 500 ms/2 s SQL 延迟或持锁。
- 记录 Hikari active、pending、timeout 和 API P99。
- 验证 timeout、限流和拒绝是否保护数据库。
- 检查事务和 task/outbox 正确性。

## 实验 6：Kafka/Consumer 停止与恢复

- 固定 Producer 速率。
- 停 Consumer 制造 backlog。
- 恢复并记录 lag 曲线。
- 根据 `backlog/(consumerCapacity-producerRate)` 计算理论恢复时间并与实际对比。

## 实验 7：供应商全故障

- Stub 持续返回 503 或 429。
- 观察 retry rate、circuit、queue、lag 和 DLT。
- 验证重试预算、暂停和分阶段恢复。
- 供应商恢复时不得释放全部 backlog。

## 实验 8：UNKNOWN

- Provider 保存请求后丢弃响应。
- 验证本地状态进入 UNKNOWN 而不是普通 FAILED。
- 通过查询/回调对账收敛。
- 断言没有重复供应商副作用。

## 实验 9：JVM/JFR

状态：Phase A 自定义 Provider 事件与 `RecordingFile` 读取 Verified；Phase B GC、分配、锁竞争和 P99 关联 Pending。

- 构造对象分配、锁竞争和阻塞线程。
- 记录 GC pause、allocation、thread state 和自定义 JFR 事件。
- 使用 JFR 证据解释 P99 上升。

Phase A 真实结果：

- 6 次提交，4 次接受，2 次拒绝，4 次完成。
- 生成 113422 字节 JFR 文件。
- `RecordingFile`、`jfr summary` 与 `jfr print` 均确认 4 条 `com.notifyflow.ProviderCall`。
- 事件只包含低基数 `taskType/outcome`、接受状态、队列深度和耗时，没有写入 taskId/traceId。

证据：`evidence/real-threadpool-jfr-2026-07-15.md`。

## 实验 10：Soak

- 以安全负载持续 2-4 小时。
- 观察 heap 基线、线程、连接、meter 数、缓存和文件句柄。
- 检查是否随时间单调增长。

## 证据目录

```text
evidence/<experiment>/
  environment.md
  workload.js
  thresholds.md
  metrics.md
  logs.md
  traces.md
  jfr.md
  correctness.sql
  timeline.md
  conclusion.md
```

## 评分

| 维度 | 分值 |
|---|---:|
| 环境与工作负载可复现 | 15 |
| Threshold 和 SLO 对齐 | 15 |
| 指标/日志/Trace/JFR 证据 | 20 |
| 故障和恢复时间线 | 20 |
| 数据正确性 | 15 |
| 结论边界与修订 | 15 |

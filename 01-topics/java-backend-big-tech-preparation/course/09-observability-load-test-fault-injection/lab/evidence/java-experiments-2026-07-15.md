# Java 21 可观测性基础实验记录

## 环境

- 日期：2026-07-15
- Java：21.0.6 LTS
- 编译器：javac 21.0.6
- 外部依赖：无
- 构建工具：无，直接使用 `javac` 和 `java`
- GREEN 编译产物：仓库 `tmp/observability-lab-build-green-20260715/`

## TDD RED

先只创建 `ObservabilityExperimentsTest.java`，测试引用尚不存在的：

- `LatencyStatistics`
- `LongTailStatisticsExperiment`
- `LoadModelSimulator`
- `MetricCardinalitySimulator`

执行仅编译测试文件的命令后，退出码为 1，得到 24 个“找不到符号/程序包不存在”错误。失败原因是生产实现尚不存在，符合预期 RED。

## TDD GREEN

实现统计、负载和基数模拟器及三个实验入口后，编译全部源码并运行测试：

```text
ALL_OBSERVABILITY_EXPERIMENT_TESTS_PASSED
```

测试覆盖：

- nearest-rank 的平均值、P50、P95、P99、P99.9 和最大值。
- 空样本和非法 percentile 被拒绝。
- 封闭模型随服务变慢自动降低到达率。
- 开放模型保持调度速率，并通过 dropped arrival 暴露容量不足。
- coordinated omission 使封闭模型 P95 低估慢阶段。
- 低基数 tag 组合稳定为 6 条时序。
- 唯一 `taskId` 令时序数线性增长到 10000。
- `taskId/traceId/userId` 被策略识别为危险 tag，`provider/result` 被允许。

## 第二轮 TDD：线程池饱和

先在测试中加入 `ThreadPoolSaturationSimulator` 的容量、拒绝、队列和 P99 断言，但不创建实现。编译全部零依赖源码得到退出码 1 和 6 个“找不到符号/程序包不存在”错误，符合预期 RED。

实现模拟器与实验入口后重新编译，测试再次输出：

```text
ALL_OBSERVABILITY_EXPERIMENT_TESTS_PASSED
```

## 实验 1：平均值掩盖长尾

输入：9900 个 10 ms、99 个 500 ms、1 个 10000 ms 请求。

真实输出：

```text
sample_count=10000
average_millis=15.8500
p50_millis=10
p95_millis=10
p99_millis=10
p999_millis=500
max_millis=10000
max_to_average_ratio=630.9148
LONG_TAIL_STATISTICS_EXPERIMENT_PASSED
```

结论：平均值只有 15.85 ms，P99 也仍为 10 ms，但单个请求达到 10 秒。只报平均值或单一 percentile 都可能遗漏少量极端用户体验，必须同时结合分布、最大值、超时率和错误分类。

## 实验 2：开放/封闭负载与 coordinated omission

固定参数：

```text
duration_millis=60000
slowdown_at_millis=30000
fast_latency_millis=100
slow_latency_millis=2000
closed_virtual_users=100
open_arrival_rate_per_second=500
open_max_in_flight=300
```

真实输出：

```text
closed_fast_started=30000
closed_slow_started=1500
closed_fast_rate_per_second=1000.00
closed_slow_rate_per_second=50.00
closed_observed_p95_millis=100
open_fast_scheduled=15000
open_slow_scheduled=15000
open_slow_accepted=4500
open_slow_dropped=10500
open_slow_drop_ratio=0.7000
open_observed_p95_millis=2000
closed_omitted_slow_phase_demand=13500
OPEN_CLOSED_LOAD_EXPERIMENT_PASSED
```

结论：

- 封闭模型在延迟从 100 ms 上升到 2 秒后，到达率从 1000/s 自动降至 50/s。
- 开放模型继续调度 500/s，因此明确暴露慢阶段 70% 的 dropped arrival。
- 慢阶段占一半墙钟时间，却只占封闭模型样本的 4.76%，导致其 P95 仍为 100 ms。
- `closed_omitted_slow_phase_demand=13500` 表示封闭客户端因等待响应而没有发出的目标需求，不是服务端显式拒绝数。

## 实验 3：指标 tag 基数增长

输入：10000 条事件、3 个 provider、2 个 result，以及可选的唯一 `taskId`。

真实输出：

```text
events=10000
low_cardinality_series=6
task_id_series=10000
series_amplification=1666.6667
assumed_metadata_bytes_per_series=512
estimated_metadata_bytes=5120000
estimated_metadata_mib=4.8828
METRIC_CARDINALITY_EXPERIMENT_PASSED
```

结论：低基数 `provider/result` 只有 6 种组合；加入唯一 `taskId` 后，每条事件几乎创建一条新时序，放大 1666.67 倍。高基数标识应放在日志或 Trace，而不是 metric tag。

## 实验 4：线程池饱和与容量拐点

参数：运行 60 秒、20 个 worker、有界队列 100、每个请求占用 worker 100 ms。

真实输出：

```text
rate,offered,accepted,rejected,max_queue,p99_ms,throughput_per_second
100,6000,6000,0,0,100,99.85
125,7500,7500,0,0,100,124.80
200,12000,12000,0,0,100,199.68
250,15000,12100,2900,100,600,199.68
400,24000,12100,11900,100,600,199.68
500,30000,12100,17900,100,600,199.68
THREAD_POOL_SATURATION_EXPERIMENT_PASSED
```

结论：

- 理论服务容量为 `20/0.1=200 req/s`。
- 到达率不超过 200/s 时没有排队和拒绝，P99 保持 100 ms。
- 250/s 时吞吐不再上升，队列达到 100，2900 个请求被拒绝，P99 上升到 600 ms。
- 继续增加流量只增加拒绝数，不增加有效吞吐。
- 容量结论必须同时观察吞吐、队列、拒绝和长尾，不能只看 CPU 或平均延迟。

## 边界

- percentile 使用 nearest-rank 和完整样本排序，不是生产 Histogram、滑动窗口或近似分位实现。
- 负载实验是确定性离散事件模型，不包含真实网络、线程调度、排队器、连接池和 k6 executor。
- dropped arrival 模拟最大并发不足，不等价于 k6 的全部 `dropped_iterations` 语义。
- 基数实验计算唯一 tag set；512 字节/时序是显式假设，不是 JVM heap、Prometheus 或 Micrometer 实测值。
- 线程池实验是确定性 worker/有界队列模型，不包含 `ThreadPoolExecutor` 调度、上下文切换、GC、网络和下游资源竞争。
- 真实 Micrometer `MeterFilter`、k6 threshold、Dashboard、JFR GC/分配/锁/采样诊断和故障注入仍为 Pending；自定义 JFR 事件链路见 `real-threadpool-jfr-2026-07-15.md`。
- 四组基础实验通过不代表第 09 章整体达到 Lab Verified 或 Released。

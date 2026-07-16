# Java 21 重试恢复实验记录

## 环境

- 日期：2026-07-14
- Java：21.0.6 LTS
- 编译器：javac 21.0.6
- 外部依赖：无
- 构建工具：无，直接使用 `javac` 和 `java`
- 编译产物目录：仓库 `tmp/recovery-lab-build-green/`

## TDD RED

先只创建 `RecoveryExperimentsTest.java`，测试引用尚不存在的：

- `RetryAmplificationSimulator`
- `BackoffSimulator`

执行编译：

```powershell
javac -encoding UTF-8 -d tmp\recovery-lab-build-red `
  01-topics\java-backend-big-tech-preparation\course\08-retry-dlt-reconciliation-recovery\lab\src\test\java\com\notifyflow\recovery\RecoveryExperimentsTest.java
```

结果：退出码 1，得到 14 个“找不到符号”错误。失败原因是实现类不存在，符合预期 RED。

## TDD GREEN

实现两个模拟器和两个实验入口后，编译全部源码并运行测试：

```text
ALL_RECOVERY_EXPERIMENT_TESTS_PASSED
```

测试覆盖：

- 五层、每层三次尝试的底层调用数为 243。
- 单一重试责任点的调用数为 3。
- 非法层数和尝试次数被拒绝。
- 上限指数退避在第四次尝试为 8000 ms。
- delay 不超过 30000 ms 上限。
- Full Jitter 峰值低于固定退避峰值的 20%。
- 相同随机种子得到相同分布。

## 实验 1：多层重试放大

真实输出：

```text
layers=5
attempts_per_layer=3
layered_leaf_calls=243
single_owner_leaf_calls=3
load_amplification_vs_single_owner=81x
RETRY_AMPLIFICATION_EXPERIMENT_PASSED
```

结论：

- 五层分别负责重试时，底层调用达到 `3^5=243`。
- 只在一个层次负责三次尝试时，底层只有 3 次调用。
- 相比单一重试责任点，多层重试的底层负载放大 81 倍。

这验证了课程中“重试必须指定唯一责任层”的结论。

## 实验 2：固定退避与 Full Jitter

参数：

```text
tasks=10000
cap_millis=1000
bucket_millis=100
seed=20260714
```

真实输出：

```text
fixed_peak=10000
full_jitter_peak=1044
jitter_to_fixed_peak_ratio=0.1044

0-99ms=987
100-199ms=1027
200-299ms=1012
300-399ms=966
400-499ms=987
500-599ms=975
600-699ms=1044
700-799ms=1010
800-899ms=988
900-999ms=992
1000-1099ms=12

BACKOFF_JITTER_EXPERIMENT_PASSED
```

结论：

- 固定 1000 ms 重试让 10000 个任务进入同一时间桶。
- Full Jitter 把任务分散到约 10 个主要时间桶。
- 最大时间桶从 10000 降为 1044，峰值为固定退避的 10.44%。
- Jitter 降低同步峰值，但不减少总请求数；总重试预算仍然必要。

## 边界

- 这是确定性离散事件模拟，不包含真实网络、线程调度和供应商限流。
- Jitter 结果依赖任务数、cap、bucket 宽度和随机种子。
- 实验验证负载分布机制，不直接证明 NotifyFlow 生产容量。
- DLT、Unknown 对账和安全重放实验仍为 Pending。


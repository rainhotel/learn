# 第 8 章实验：重试风暴、DLT、对账与安全重放

## 当前状态

- 状态：实验 1-2 已通过 Java 21 验证，实验 3-8 Pending
- 已完成：重试放大和 Full Jitter 零依赖 Java 实现、TDD RED/GREEN、真实输出与证据记录
- 未完成：错误分类、Spring Kafka DLT、Unknown 对账、回调乱序、安全重放、分阶段恢复和事故复盘

真实证据：`evidence/java-experiments-2026-07-14.md`。

## 源码

```text
src/main/java/com/notifyflow/recovery/
  RetryAmplificationSimulator.java
  RetryAmplificationExperiment.java
  BackoffSimulator.java
  BackoffJitterExperiment.java

src/test/java/com/notifyflow/recovery/
  RecoveryExperimentsTest.java
```

运行时只需要 Java 21，不需要 Maven 和第三方依赖。编译产物建议输出到仓库 `tmp/`，避免污染课程源码目录。

## 实验 1：多层重试放大

状态：JDK 21 Verified。

### 目标

验证五层调用链每层最多 3 次尝试时，最底层调用数如何接近 `3^5=243`。

### 方案

- 构造五层函数调用，每层失败时重试。
- 底层始终失败并计数。
- 对比“每层重试”和“只在最外层重试”。

### 指标

- 底层调用次数。
- 总执行时间。
- 每层尝试次数。

### 验收

- 多层方案展示指数放大。
- 单点方案调用次数受明确预算控制。

## 实验 2：固定退避与 Full Jitter

状态：JDK 21 Verified。

### 目标

模拟 1000 个任务同时失败，比较固定间隔、上限指数退避和 Full Jitter 的重试时间分布。

### 指标

- 每 100 ms 时间桶内最大重试数。
- P50/P95/P99 重试等待。
- 峰值与平均值之比。

### 预期

固定间隔形成明显尖峰；Full Jitter 把重试分散，但会引入等待分布。

实际结果：固定退避峰值 10000，Full Jitter 峰值 1044，峰值比例 10.44%。

## 运行命令

从仓库根目录执行：

```powershell
$build='tmp\recovery-lab-build-green'
New-Item -ItemType Directory -Force -Path $build | Out-Null
$sourceRoot='01-topics\java-backend-big-tech-preparation\course\08-retry-dlt-reconciliation-recovery\lab\src'
$sources=Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d $build $sources
java -cp $build com.notifyflow.recovery.RecoveryExperimentsTest
java -cp $build com.notifyflow.recovery.RetryAmplificationExperiment
java -cp $build com.notifyflow.recovery.BackoffJitterExperiment
```

## 实验 3：错误分类

输入：429、500、非法号码、反序列化错误、客户端超时、数据库不可用。

断言：

- 429/部分 5xx 进入有限重试。
- 非法号码直接永久失败。
- Schema/反序列化进入 DLT。
- 客户端超时进入 Unknown 和对账。
- 数据库全故障触发系统性暂停，不逐条高速重试。

## 实验 4：Spring Kafka DLT

### 目标

- 使用 `DefaultErrorHandler`、`FixedBackOff` 和 `DeadLetterPublishingRecoverer`。
- 验证 fatal exception 跳过重试。
- 验证可重试异常达到上限后进入 DLT。

### 故障点

- DLT Partition 数少于原 Topic。
- DLT 发布失败。
- Header 堆栈过大。
- DLT Consumer 再次失败。

### 证据

- 原 Topic/Partition/offset。
- delivery attempt。
- 异常分类。
- DLT Header 和 payload 大小。

## 实验 5：供应商 Unknown 对账

### 时间线

```text
Provider 收到请求并保存
-> 响应被故障代理丢弃
-> NotifyFlow timeout，attempt=UNKNOWN
-> 普通 retry 不执行
-> 对账查询返回 SUCCEEDED
-> 本地状态收敛为 SUCCEEDED
```

对照错误实现：超时后立即新建 attempt，产生重复供应商请求。

## 实验 6：回调乱序

顺序注入：

```text
DELIVERED callback
ACCEPTED callback（晚到）
DELIVERED callback（重复）
FAILED callback（错误或过期）
```

验收：最终状态保持 DELIVERED；重复回调被唯一键识别；非法倒退创建异常 case。

## 实验 7：安全重放

### 数据

- 100 条已成功事件。
- 50 条永久失败事件。
- 30 条修复后可重放事件。
- 20 条跨租户无权限事件。

### 验收

- preview 返回准确分布。
- 已成功事件以原 eventId 重放时安全 no-op。
- 永久失败不进入执行。
- 跨租户记录被权限过滤。
- 30 条任务按 max QPS 执行。
- 审计记录包含请求者、审批者、原因和结果。

## 实验 8：下游全故障与分阶段恢复

### 步骤

1. 固定新流量速率。
2. 让供应商持续返回 503。
3. 观察普通重试引起的额外负载。
4. 打开渠道熔断并暂停消费。
5. 供应商恢复后用 1% 探测。
6. 按 5%、20%、50%、100% 分阶段恢复。
7. 记录 backlog、成功率、P99 和恢复时间。

### 验收

- 故障期间 retry rate 受预算限制。
- 恢复流量不超过供应商安全容量。
- backlog 持续下降。
- Unknown case 被独立对账。

## 证据要求

每组实验保存：

- 环境和版本。
- 故障注入方式。
- 输入数据。
- 原始日志和数据库快照。
- 指标时间线。
- 失败路径与恢复路径。
- 残余风险。
- 对 NotifyFlow 设计的修订。

## 评分

| 维度 | 分值 |
|---|---:|
| 故障可重复 | 15 |
| 时间线完整 | 15 |
| 指标和原始证据 | 20 |
| 错误分类正确 | 15 |
| 恢复不制造二次事故 | 15 |
| 权限与审计 | 10 |
| 机制解释和反例 | 10 |

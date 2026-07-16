# Spring Boot/Micrometer 最小实验

## 当前状态

- 实现状态：测试、应用、指标封装、危险 tag 策略和 Actuator exposure 配置已准备
- 运行状态：Pending
- 固定版本：Spring Boot 4.1.0、Micrometer 1.17.0、Java 21
- 阻塞原因：本机无精确版本缓存；Maven 写入课程内本地仓库被沙箱拒绝，联网下载审批又返回 403

当前失败发生在依赖解析之前，不是有效的测试 RED，也不能声称编译或测试通过。

证据：`../evidence/micrometer-build-2026-07-15.md`。

## 要验证的结论

1. `provider/result` allowlist 只产生 3×2=6 条 Timer 时序。
2. Timer 记录 count、total time，并开启 percentile histogram。
3. 带 `taskId/traceId/userId/requestId/eventId` 的 meter 在注册前被 `MeterFilter` 拒绝。
4. `/actuator/health`、`/actuator/metrics` 和自定义 metric 端点可访问。
5. `/actuator/env` 未被 exposure allowlist 暴露，返回 404。

## 目录

```text
src/main/java/com/notifyflow/observability/micrometer/
  NotifyFlowApplication.java
  NotifyFlowMetrics.java
  MetricTagPolicy.java

src/main/resources/
  application.yml

src/test/java/com/notifyflow/observability/micrometer/
  NotifyFlowMetricsTest.java
  ActuatorExposureTest.java
```

## 运行命令

首次联网运行需要用户批准依赖下载：

```powershell
$mvn='D:\Program Files\Java\apache-maven-3.9.9\bin\mvn.cmd'
$repo='D:\moniC\project\learn\tmp\m2-observability'
& $mvn -U "-Dmaven.repo.local=$repo" test
```

依赖下载成功后必须再离线复跑：

```powershell
& $mvn -o "-Dmaven.repo.local=$repo" test
```

发布证据必须包含：

- `mvn test` 退出码 0。
- Spring Boot 和 Micrometer 的实际依赖树版本。
- 两个测试类的通过数量。
- HTTP exposure 断言的真实结果。
- 离线复跑成功，证明依赖已固定。

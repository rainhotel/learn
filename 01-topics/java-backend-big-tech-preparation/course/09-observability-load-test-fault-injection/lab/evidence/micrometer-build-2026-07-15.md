# Micrometer 实验构建记录

## 目标版本

- Java：21.0.6 LTS
- Maven：3.9.9
- Spring Boot：4.1.0
- Micrometer：1.17.0

## 缓存审计

- Maven 全局配置把默认本地仓库设置为 `D:\Program Files\Java\apache-maven-3.9.9\local`，当前沙箱只能读取，不能写入。
- 默认仓库和用户 `.m2` 都没有 Spring Boot 4.1.0 与 Micrometer 1.17.0。
- 默认仓库只有较旧的 Spring Boot 3.5.11 与 Micrometer 1.15.9；不能为了离线通过而降级，因为这会使实验与课程资料基线不一致。

## 已准备实现

- `NotifyFlowMetrics`：只允许 3 个 provider 和 2 个 result，使用 Timer histogram 记录供应商请求。
- `MetricTagPolicy`：通过 `MeterFilter` 拒绝身份类高基数 tag。
- `application.yml`：只通过 Web exposure 暴露 health 和 metrics。
- `NotifyFlowMetricsTest`：断言 6 条低基数 Timer 和危险 tag 拒绝。
- `ActuatorExposureTest`：启动随机端口，通过 Java HTTP Client 验证允许与禁止的 Actuator 端点。

## 第一次 Maven 尝试

命令在课程 `micrometer-demo` 目录执行，并把本地仓库指向该目录下 `.m2`。

真实结果：

```text
exit_code=1
Could not create local repository at ...\micrometer-demo\.m2
```

这表明 Maven 子进程写入被沙箱拒绝，尚未进入依赖解析、Java 编译或测试阶段。

## 联网审批尝试

随后请求在仓库 `tmp/m2-observability` 下载依赖并运行测试。审批服务返回 403，命令未执行。

因此：

- 当前没有有效的测试 RED。
- 当前没有 GREEN 输出。
- 当前不能声称 Spring Boot、Micrometer、`MeterFilter` 或 Actuator HTTP 测试通过。

## 下一次验证顺序

1. 用户显式批准 Maven 联网下载。
2. 运行在线 `mvn test`，首先观察实现是否产生真实编译或测试失败。
3. 修复代码直至 GREEN。
4. 输出 `dependency:tree`，核对 Spring Boot 4.1.0 与 Micrometer 1.17.0。
5. 使用同一本地仓库执行 `mvn -o test` 离线复跑。

## 边界

目前只完成了可审阅的工程实现和配置，未完成依赖解析与运行验证。该实验仍为 Pending。

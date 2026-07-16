# 来源与验证记录

## Google SRE Book

### Implementing SLOs

- URL：<https://sre.google/workbook/implementing-slos/>
- 访问日期：2026-07-14
- 支撑结论：
  - SLI 应尽量使用用户可感知的 good events / valid events 比例。
  - SLO 是可靠性决策工具，不应只作为展示 KPI。
  - SLO 通常不应默认设为 100%，否则失去风险交换空间。
  - Error budget 只有实际影响发布和优先级决策时才有意义。
  - 初始 SLO 可以不完美，但需要持续修订机制。

### Monitoring Distributed Systems

- URL：<https://sre.google/sre-book/monitoring-distributed-systems/>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照与文本查找。
- 支撑结论：
  - 四个黄金信号是 latency、traffic、errors 和 saturation。
  - 成功请求与失败请求的延迟应区分；快速 500 不能让总体延迟看起来更好。
  - Error 不只包含显式失败，也包含错误内容和违反延迟策略的请求。
  - Saturation 关注最受约束资源，系统通常在 100% 使用率前就开始退化。
  - P99 延迟上升可能是饱和度的早期信号。
  - 告警更应面向用户症状或即将发生的真实问题，原因型信号主要辅助诊断。

## Spring Boot 4.1.0 与 Micrometer 1.17.0

### Metrics

- URL：<https://docs.spring.io/spring-boot/reference/actuator/metrics.html>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照与文本查找。
- 支撑结论：
  - Spring Boot 自动配置 composite `MeterRegistry`，按 classpath 添加具体 registry。
  - 自定义指标应使用 Spring 管理的 `MeterRegistry`，而不是静态全局 registry。
  - `MeterBinder` 适合注册一组可复用指标。
  - `MeterFilter` 可用于接受、拒绝或调整 meter。
  - Spring MVC 默认产生 `http.server.requests` 指标。
  - `ObservationRegistry` 上的 `DefaultMeterObservationHandler` 为完成的 Observation 生成指标。
  - 当前 Spring Boot 4.1.0 页面引用 Micrometer 1.17.0 API。

### Micrometer Concepts

- URL：<https://docs.micrometer.io/micrometer/reference/concepts.html>
- 访问日期：2026-07-14
- 版本：页面显示 Micrometer 1.17.0。
- 支撑结论：Micrometer 提供 Counter、Gauge、Timer、Distribution Summary、Long Task Timer、Histogram/Percentile、Observation、Meter Filter 和高基数检测等概念入口。
- 边界：客户端 percentile、pause detection 和不同后端的聚合能力存在额外成本或差异，必须结合实际 registry 验证。

### Actuator Endpoints 与 Observability

- URL：<https://docs.spring.io/spring-boot/reference/actuator/endpoints.html>
- URL：<https://docs.spring.io/spring-boot/reference/actuator/observability.html>
- URL：<https://docs.spring.io/spring-boot/reference/actuator/tracing.html>
- 访问日期：2026-07-14
- 支撑结论：endpoint bean 的创建与 HTTP/JMX 暴露是不同控制面；管理端点不能默认全部对 Web 开放；生产环境需要认证、授权、管理端口和网络隔离。
- 边界：Actuator 提供通用基础设施指标，NotifyFlow 的任务等待、DLT、UNKNOWN 和 deadline SLI 仍需自行定义。

## OpenTelemetry

### Signals

- URL：<https://opentelemetry.io/docs/concepts/signals/>
- URL：<https://opentelemetry.io/docs/concepts/context-propagation/>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照与文本查找。
- 支撑结论：
  - Trace 表示请求在应用中的路径。
  - Metric 是运行时捕获的测量值。
  - Log 是事件记录。
  - Baggage 是在信号之间传播的上下文信息。
  - 跨进程关联依赖 carrier 和 propagator 正确传播 context。

课程边界：采集多种 signal 不会自动产生关联；OpenTelemetry 也不等于长期存储和可视化后端。Baggage 会跨进程传播，不能放密码、token、手机号等敏感内容，也不适合无限携带高基数业务数据。

## Grafana k6

### Open and closed models

- URL：<https://grafana.com/docs/k6/latest/using-k6/scenarios/concepts/open-vs-closed/>
- 访问日期：2026-07-14
- 核验方式：Playwright 实页快照与文本查找。
- 支撑结论：
  - 封闭模型只有当前 iteration 完成后才开始下一次。
  - 系统变慢会降低封闭模型的到达率，形成 coordinated omission。
  - 开放模型将 iteration 到达与完成耗时解耦。
  - k6 的 `constant-arrival-rate` 和 `ramping-arrival-rate` 用于开放模型。

### Thresholds

- URL：<https://grafana.com/docs/k6/latest/using-k6/thresholds/>
- 访问日期：2026-07-14
- 支撑结论：Threshold 是测试指标的 pass/fail 条件；不满足条件时测试以非零退出码结束，可用于编码 SLO 和 CI 门禁。
- 边界：Threshold 只证明本次环境和负载样本是否达标，不能单独证明生产容量或长期稳定性。

## JDK 21 Flight Recorder API

- URL：<https://docs.oracle.com/en/java/javase/21/jfapi/>
- URL：<https://docs.oracle.com/en/java/javase/21/jfapi/why-use-jfr-api.html>
- 访问日期：2026-07-14
- 支撑结论：JFR API 支持创建自定义事件、配置启用状态/阈值/stack trace、通过 Event Streaming API 监控事件，以及解析 recording 文件。
- 边界：JFR 只提供单 JVM 内部证据，不能替代跨服务 Trace 和业务 SLI；生产环境仍需验证高频事件、stack trace 和采样配置的开销。

## 当前验证状态

| 项目 | 状态 | 证据 |
|---|---|---|
| 黄金信号和告警原则 | 已核验 | Google SRE 官方书籍 |
| Spring Boot/Micrometer 版本与集成 | 已核验 | Spring Boot 4.1.0 官方文档 |
| OpenTelemetry 信号模型 | 已核验 | OpenTelemetry 官方文档 |
| 开放/封闭负载和 threshold | 已核验 | Grafana k6 官方文档 |
| JFR 能力边界 | 已核验 | Oracle JDK 21 官方指南 |
| 长尾、负载模型、tag 基数和线程池饱和基础实验 | Java 21 已验证 | `lab/evidence/java-experiments-2026-07-15.md` |
| 真实 ThreadPoolExecutor 拒绝路径和自定义 JFR 事件 | JDK 21 已验证 | `lab/evidence/real-threadpool-jfr-2026-07-15.md` |
| k6 开放/封闭/分阶段恢复脚本 | 已准备，静态验证 | `lab/evidence/k6-static-2026-07-15.md` |
| NotifyFlow Micrometer 指标实现 | 已准备，运行 Pending | `lab/evidence/micrometer-build-2026-07-15.md` |
| k6 压测 | Pending | 尚无真实报告 |
| JFR GC/分配/锁竞争与故障注入 | Pending | 自定义事件 Phase A 已验证，其余尚无真实证据 |

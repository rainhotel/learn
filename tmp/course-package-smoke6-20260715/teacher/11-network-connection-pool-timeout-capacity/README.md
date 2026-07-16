# 第 11 章：网络、连接池、超时与容量

## 章节定位

- 类型：Networking + Java Backend + Capacity + Incident + Lab Design + Interview
- 难度：深入
- 建议学习时间：24-32 小时
- 先修章节：线程池、MySQL、Redis、Kafka、可观测性、JVM
- 对应项目：NotifyFlow Provider/数据库/Redis/Kafka/模型 API 的连接与超时预算

## 当前状态

- 阶段：八件套完整内容初稿，实验 Pending
- 调研日期：2026-07-15
- 已完成：DNS/TCP/TLS/HTTP、连接池、超时、取消、重试、容量和事故设计
- 未完成：网络代理、连接池、端口耗尽、慢依赖和模型流式调用运行证据

本章不能标记为 Lab Verified、Release Candidate 或 Released。

## 核心问题

1. 一次 HTTP 请求经过 DNS、连接池、TCP、TLS、服务端队列和响应的哪些阶段？
2. connect/read/write/acquire/request/deadline timeout 有什么区别？
3. 为什么连接池和线程池都不是越大越好？
4. keep-alive、HTTP/2 multiplexing、DNS TTL 和负载均衡如何影响连接复用？
5. 超时后下游可能已执行时，如何进入 UNKNOWN 和对账？
6. 重试、hedging、熔断、限流和 backpressure 如何避免放大？
7. Little's Law、并发、吞吐、P99 和池大小如何关联？
8. Java SSE/模型流式响应如何处理取消、断线和半截输出？

## 退出标准

- 能画出请求分段延迟并为每段设置预算。
- 能解释数据库/HTTP/Redis/Kafka 连接池的不同语义。
- 能用 Little's Law 估算在途请求和容量，而不是拍脑袋配置池大小。
- 能区分 timeout、拒绝、连接失败、半开连接和 UNKNOWN。
- 能设计取消传播、有限重试、熔断、限流和恢复。
- 能排查 DNS、端口、连接池、线程池、下游慢和客户端断线。
- 能为 Provider 与模型流式 API 设计指标、Trace 和数据正确性。

## 发布前缺口

- 运行 DNS/TCP/TLS/HTTP 分段时间实验。
- 运行 Hikari/HTTP 连接池 acquire timeout 和饱和实验。
- 运行慢响应、半开、RST、丢包、端口耗尽和取消传播实验。
- 运行 Provider timeout -> UNKNOWN -> reconciliation。
- 运行 SSE/模型流式断线和最终状态实验。
- 完成学习者作业与 Teach-back。

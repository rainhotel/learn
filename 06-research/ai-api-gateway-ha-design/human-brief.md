# High Availability AI API Gateway Design Human Brief

## What This Research Is About

- 研究对象：高可用 AI API 网关，也就是在业务应用和 OpenAI、Anthropic、Gemini、DeepSeek、Bedrock、自建模型等上游之间的一层统一访问入口。
- 研究目的：把市场上的开源项目拆开看，提炼出一个可以自己实现、可以压测、可以写简历的后端项目。

## Current Best Understanding

- 市面上的项目大致分三类：
  - LLM 专用网关：LiteLLM、Portkey Gateway、Bifrost、Helicone AI Gateway、GoModel、AxonHub。
  - API 网关加 AI 能力：Apache APISIX、Higress、Envoy AI Gateway、Kong AI Gateway。
  - LLMOps 一体化平台：TensorZero、Langfuse OSS LLMOps Stack 等。
- 简历项目最适合走“LLM 专用网关 + API 网关高可用能力”的组合路线。
- 只做模型转发不够强，最好覆盖：
  - OpenAI 兼容接口和 SSE 流式返回。
  - 多 Provider / 多 Key / 多模型路由。
  - 超时、重试、故障转移、熔断、健康评分。
  - 基于 Redis 的分布式 RPM / TPM 限流。
  - 虚拟 API Key、配额、成本统计。
  - Prometheus 指标、结构化日志、链路追踪。
  - Docker Compose 或 Kubernetes 多副本部署和压测报告。

## Decision Value

- 能帮助判断：要做什么功能，项目才不像普通 CRUD 或简单代理。
- 能帮助判断：应该参考哪些开源项目，而不是盲目从零想象。
- 暂时不能判断：最终技术栈必须选 Go、Java、Rust 还是 TypeScript，这要结合投递岗位决定。

## Recommended Project Shape

- 项目名：`ModelMesh Gateway`。
- 推荐定位：High-Availability OpenAI-Compatible AI Gateway。
- 推荐技术栈：Java 21 + Spring Boot WebFlux + Redis + PostgreSQL + Prometheus/Grafana。
- 选择原因：最贴合 Java 后端简历，同时能展示非阻塞 IO、SSE 长连接、分布式限流、熔断降级、可观测和压测。
- 最小可用版本：
  - 实现 `/v1/chat/completions`，支持普通 JSON 和 SSE streaming。
  - 支持至少 2 个上游 Provider 或 Mock Provider。
  - 支持权重路由、失败重试、fallback、限流和指标。

## Resume Fast

- 下次打开先读：`resume-project-plan.md`
- 下次打开先做：创建项目仓库骨架，先完成 WebFlux gateway + Mock Provider。
- 当前最关键的 1 个问题：第一版先把 SSE streaming 和 fallback 做扎实。

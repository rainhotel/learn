# High Availability AI API Gateway Design Conclusion

## Final Position For Now

- 最适合做进简历的方向是：高可用 OpenAI-compatible AI API Gateway。
- 不建议只做“统一转发接口”，因为技术含量会被看成普通代理。
- 推荐把项目设计成“LLM 专用路由能力 + API 网关高可用能力”的结合体。
- 最终收束项目：`ModelMesh Gateway`，Java 21 + Spring Boot WebFlux + Redis + PostgreSQL + Prometheus/Grafana。

## Evidence Chain

1. LiteLLM、Portkey、Bifrost、AxonHub 等 LLM gateway 都把多 provider、load balancing、fallback、cost、observability 作为核心卖点。
2. APISIX、Higress、Envoy AI Gateway 说明传统 API 网关正在把 AI 流量治理纳入插件和云原生网关体系。
3. TensorZero、Helicone 等项目说明单纯转发已经不够，观测、实验、成本、缓存会成为 AI gateway 的差异化能力。
4. 简历项目如果能复现故障转移、分布式限流、SSE streaming、指标观测，就能明显区别于 CRUD 和简单 API proxy。

## What Seems True

- 高可用 AI API 网关的关键不是“支持多少模型”，而是“上游不稳定时能不能稳定服务业务”。
- MVP 只需要 2 到 3 个 provider / mock provider，但必须有可演示的故障注入和压测结果。
- 最重要的模块优先级：
  - OpenAI-compatible protocol
  - streaming proxy
  - model alias and routing policy
  - retry / fallback / circuit breaker
  - Redis distributed rate limiting
  - usage and cost accounting
  - Prometheus metrics

## What Is Still Uncertain

- 技术栈要不要选 Go：
  - Go 更贴近 Bifrost、Higress、Envoy AI Gateway 这类基础设施项目。
  - Java 更贴近国内后端岗位，但需要用 WebFlux / Netty 才能把 streaming 和高并发讲清楚。
- 是否要做管理后台：
  - 有后台更完整，但容易分散精力。
  - 第一版可以只做 Admin API 和配置文件，后续再加 UI。
- 是否要做 semantic cache：
  - 是亮点，但涉及 embedding、相似度阈值和缓存命中解释。
  - 建议作为二期功能。

## Recommendation

- 是否继续研究：继续。
- 最值得继续的方向：直接进入项目实现，先做 WebFlux gateway skeleton 和 Mock Provider。
- 推荐项目定位：
  - 中文：高可用 AI API 网关
  - 英文：High-Availability OpenAI-Compatible AI Gateway
- 推荐首版功能边界：
  - Chat Completions + SSE streaming。
  - 2 个 mock upstream + 1 个真实 OpenAI-compatible upstream。
  - 权重路由、重试、fallback、熔断。
  - Redis 分布式 RPM / TPM 限流。
  - Prometheus metrics + Grafana dashboard。
  - 故障注入压测报告。
- 详细项目计划：`resume-project-plan.md`

## Draft Resume Bullet

- 设计并实现高可用 AI API Gateway，兼容 OpenAI Chat Completions 与 SSE 流式响应，支持多 Provider 权重路由、超时重试、熔断降级、自动 fallback、Redis 分布式限流和虚拟 API Key 配额管理。
- 接入 Prometheus/Grafana 构建可观测体系，统计 QPS、p95/p99 延迟、上游错误率、fallback 次数和 token 成本；通过 Mock LLM Provider 构造 5xx、429、慢响应故障场景并完成压测验证。
